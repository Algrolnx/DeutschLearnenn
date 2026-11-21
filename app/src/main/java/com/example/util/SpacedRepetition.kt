package com.example.util

import java.util.concurrent.TimeUnit

enum class ReviewRating {
    AGAIN, // Forgot -> Reset to Box 1, due soon
    HARD,  // Struggled -> Keep Box, due in 1 day
    GOOD,  // Correct -> Advance +1 Box
    EASY   // Mastered -> Advance +2 Boxes
}

data class SrsResult(
    val newBox: Int,
    val nextReviewTimestamp: Long,
    val isCorrect: Boolean
)

data class SrsStepResult(
    val newStep: Int,
    val newBox: Int,
    val nextReviewTimestamp: Long,
    val intervalHours: Long,
    val isMastered: Boolean
)

object SpacedRepetition {

    /**
     * Target number of spaced repetitions to move a word into long-term memory.
     */
    const val TARGET_REPETITIONS = 16

    /**
     * First scheduled review happens 6 hours after completing a word/subtopic,
     * so the learner consolidates it once more the same day.
     */
    const val INITIAL_DELAY_HOURS = 6L

    /**
     * Progressive post-review intervals (in hours) for steps 1..16:
     * Review #1 (after 6h) -> next in 1d (24h)
     * Review #2 -> next in 2d (48h)
     * Review #3 -> 4d, #4 -> 6d, #5 -> 9d, #6 -> 12d, #7 -> 16d, #8 -> 21d,
     * #9 -> 28d, #10 -> 35d, #11 -> 45d, #12 -> 60d, #13 -> 75d, #14 -> 90d,
     * #15 -> 120d, #16 (mastered) -> 150d maintenance check.
     */
    val POST_REVIEW_INTERVAL_HOURS: List<Long> = listOf(
        24L,    // after 1st review -> 1 day
        48L,    // after 2nd review -> 2 days
        96L,    // after 3rd review -> 4 days
        144L,   // after 4th review -> 6 days
        216L,   // after 5th review -> 9 days
        288L,   // after 6th review -> 12 days
        384L,   // after 7th review -> 16 days
        504L,   // after 8th review -> 21 days
        672L,   // after 9th review -> 28 days
        840L,   // after 10th review -> 35 days
        1080L,  // after 11th review -> 45 days
        1440L,  // after 12th review -> 60 days
        1800L,  // after 13th review -> 75 days
        2160L,  // after 14th review -> 90 days
        2880L,  // after 15th review -> 120 days
        3600L   // after 16th review (mastered) -> 150 days
    )

    fun initialEnrollmentTimestamp(now: Long = System.currentTimeMillis()): Long {
        return now + TimeUnit.HOURS.toMillis(INITIAL_DELAY_HOURS)
    }

    /**
     * Gentle 16-repetition spaced-repetition step calculation.
     * - Correct answer advances step by +1 (up to 16) with progressively wider intervals.
     * - Mistake applies a gentle setback (-2 steps, never wiping hard-earned progress to 0)
     *   and schedules a calm retry after 6h (or 12h for higher steps).
     */
    fun calculate16StepReview(
        currentStep: Int,
        isCorrect: Boolean,
        now: Long = System.currentTimeMillis()
    ): SrsStepResult {
        val safeStep = currentStep.coerceIn(0, TARGET_REPETITIONS)
        return if (isCorrect) {
            val newStep = (safeStep + 1).coerceAtMost(TARGET_REPETITIONS)
            val delayHours = POST_REVIEW_INTERVAL_HOURS.getOrElse(safeStep) {
                POST_REVIEW_INTERVAL_HOURS.last()
            }
            SrsStepResult(
                newStep = newStep,
                newBox = mapStepToLeitnerBox(newStep),
                nextReviewTimestamp = now + TimeUnit.HOURS.toMillis(delayHours),
                intervalHours = delayHours,
                isMastered = newStep >= TARGET_REPETITIONS
            )
        } else {
            val newStep = (safeStep - 2).coerceAtLeast(0)
            val retryHours = if (safeStep >= 4) 12L else INITIAL_DELAY_HOURS
            SrsStepResult(
                newStep = newStep,
                newBox = mapStepToLeitnerBox(newStep),
                nextReviewTimestamp = now + TimeUnit.HOURS.toMillis(retryHours),
                intervalHours = retryHours,
                isMastered = false
            )
        }
    }

    /**
     * Maps 0..16 repetition steps to the classic 1..5 Leitner box scale for Progress charts.
     */
    fun mapStepToLeitnerBox(step: Int): Int = when {
        step >= 14 -> 5
        step >= 10 -> 4
        step >= 6 -> 3
        step >= 3 -> 2
        else -> 1
    }

    fun formatIntervalUk(hours: Long): String {
        if (hours < 24L) {
            return "через $hours год"
        }
        val days = (hours / 24L).toInt().coerceAtLeast(1)
        return if (days == 1) {
            "завтра (1 день)"
        } else {
            "через $days ${pluralDaysUk(days)}"
        }
    }

    fun formatTimeUntilUk(targetTimestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diffMs = targetTimestamp - now
        if (diffMs <= 0L) return "Зараз"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        if (minutes < 60L) {
            val m = minutes.coerceAtLeast(1L)
            return "Через $m хв"
        }
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        if (hours < 24L) {
            return "Через $hours год"
        }
        val days = TimeUnit.MILLISECONDS.toDays(diffMs).toInt().coerceAtLeast(1)
        return if (days == 1) {
            "Завтра"
        } else {
            "Через $days ${pluralDaysUk(days)}"
        }
    }

    private fun pluralDaysUk(days: Int): String {
        val mod100 = days % 100
        val mod10 = days % 10
        return when {
            mod100 in 11..14 -> "днів"
            mod10 == 1 -> "день"
            mod10 in 2..4 -> "дні"
            else -> "днів"
        }
    }

    /**
     * Legacy Leitner helper preserved for compatibility.
     */
    fun calculateNextReview(currentBox: Int, rating: ReviewRating, now: Long = System.currentTimeMillis()): SrsResult {
        return when (rating) {
            ReviewRating.AGAIN -> {
                SrsResult(
                    newBox = 1,
                    nextReviewTimestamp = now + TimeUnit.HOURS.toMillis(INITIAL_DELAY_HOURS),
                    isCorrect = false
                )
            }
            ReviewRating.HARD -> {
                SrsResult(
                    newBox = currentBox.coerceIn(1, 5),
                    nextReviewTimestamp = now + TimeUnit.DAYS.toMillis(1),
                    isCorrect = true
                )
            }
            ReviewRating.GOOD -> {
                val nextBox = (currentBox + 1).coerceAtMost(5)
                val intervalDays = getIntervalDaysForBox(nextBox)
                SrsResult(
                    newBox = nextBox,
                    nextReviewTimestamp = now + TimeUnit.DAYS.toMillis(intervalDays),
                    isCorrect = true
                )
            }
            ReviewRating.EASY -> {
                val nextBox = (currentBox + 2).coerceAtMost(5)
                val intervalDays = getIntervalDaysForBox(nextBox) * 1.5
                SrsResult(
                    newBox = nextBox,
                    nextReviewTimestamp = now + TimeUnit.DAYS.toMillis(intervalDays.toLong().coerceAtLeast(2)),
                    isCorrect = true
                )
            }
        }
    }

    private fun getIntervalDaysForBox(box: Int): Long {
        return when (box) {
            1 -> 1L
            2 -> 3L
            3 -> 7L
            4 -> 14L
            5 -> 30L
            else -> 1L
        }
    }
}

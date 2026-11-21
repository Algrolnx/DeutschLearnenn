package com.example.util

import java.util.concurrent.TimeUnit

enum class ReviewRating {
    AGAIN, // Forgot -> Reset to Box 1, due immediately
    HARD,  // Struggled -> Keep Box, due in 1 day
    GOOD,  // Correct -> Advance +1 Box
    EASY   // Mastered -> Advance +2 Boxes
}

data class SrsResult(
    val newBox: Int,
    val nextReviewTimestamp: Long,
    val isCorrect: Boolean
)

object SpacedRepetition {

    /**
     * Calculates the new Leitner box and the epoch timestamp for the next review
     */
    fun calculateNextReview(currentBox: Int, rating: ReviewRating, now: Long = System.currentTimeMillis()): SrsResult {
        return when (rating) {
            ReviewRating.AGAIN -> {
                // Reset to box 1, due immediately
                SrsResult(
                    newBox = 1,
                    nextReviewTimestamp = now + TimeUnit.MINUTES.toMillis(10),
                    isCorrect = false
                )
            }
            ReviewRating.HARD -> {
                // Keep same box, review tomorrow
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

package com.example.data.repository

import android.content.Context
import com.example.data.db.BoxCount
import com.example.data.db.LevelStats
import com.example.data.db.SubtopicLevelCount
import com.example.data.db.WordDao
import com.example.data.model.SubtopicProgressEntity
import com.example.data.model.TopicCatalog
import com.example.data.model.WordEntity
import com.example.util.ReviewRating
import com.example.util.SpacedRepetition
import com.example.util.SrsStepResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ImportSummary(
    val importedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int
)

data class ReviewDashboardSnapshot(
    val dueCount: Int = 0,
    val enrolledCount: Int = 0,
    val mastered16Count: Int = 0,
    val nextReviewTimestamp: Long? = null
)

class VocabularyRepository(
    private val wordDao: WordDao,
    private val appContext: Context? = null
) {

    private val srsPrefs by lazy {
        appContext?.getSharedPreferences("deutschlern_srs_steps", Context.MODE_PRIVATE)
    }

    private val subtopicStagesPrefs by lazy {
        appContext?.getSharedPreferences("deutschlern_subtopic_stages", Context.MODE_PRIVATE)
    }

    fun observeWordCount(): Flow<Int> = wordDao.observeWordCount()

    fun observeFavoriteCount(): Flow<Int> = wordDao.observeFavoriteCount()

    fun observeEnrolledWordCount(): Flow<Int> = wordDao.observeEnrolledWordCount()

    fun observeBoxCounts(): Flow<List<BoxCount>> = wordDao.observeBoxCounts()

    fun observeLevelStats(): Flow<List<LevelStats>> = wordDao.observeLevelStats()

    fun observeSubtopicLevelCounts(): Flow<List<SubtopicLevelCount>> = wordDao.observeSubtopicLevelCounts()

    fun getWordsBySubtopic(subtopicId: String): Flow<List<WordEntity>> {
        return wordDao.getWordsBySubtopic(subtopicId)
    }

    suspend fun getWordsBySubtopicAndLevelOnce(
        subtopicId: String,
        level: String,
        limit: Int = 15
    ): List<WordEntity> = withContext(Dispatchers.IO) {
        val exact = wordDao.getWordsBySubtopicAndLevelOnce(subtopicId, level, limit)
        if (exact.isNotEmpty()) exact else wordDao.getWordsBySubtopicOnce(subtopicId, 12)
    }

    suspend fun getAvailableLevelsForSubtopicOnce(subtopicId: String): List<String> = withContext(Dispatchers.IO) {
        val found = wordDao.getAvailableLevelsForSubtopicOnce(subtopicId).toSet()
        val standard = listOf("A1", "A2", "B1", "B2")
        val filtered = standard.filter { it in found }
        filtered.ifEmpty { listOf("A1") }
    }

    suspend fun getWordsByLevelOnce(level: String): List<WordEntity> = withContext(Dispatchers.IO) {
        wordDao.getWordsByLevelOnce(level)
    }

    suspend fun getDistractorsForWord(target: WordEntity, count: Int = 3): List<WordEntity> = withContext(Dispatchers.IO) {
        val picked = mutableListOf<WordEntity>()
        fun accept(c: WordEntity): Boolean {
            if (c.german.equals(target.german, ignoreCase = true)) return false
            if (c.translationUk.equals(target.translationUk, ignoreCase = true)) return false
            return picked.none {
                it.german.equals(c.german, ignoreCase = true) ||
                    it.translationUk.equals(c.translationUk, ignoreCase = true)
            }
        }

        wordDao.getDistractorsSameSubtopic(target.subtopicId, target.german, count * 3).forEach {
            if (picked.size < count && accept(it)) picked.add(it)
        }
        if (picked.size < count) {
            wordDao.getDistractorsSameCategory(target.categoryId, target.german, count * 3).forEach {
                if (picked.size < count && accept(it)) picked.add(it)
            }
        }
        if (picked.size < count) {
            wordDao.getDistractorsGeneral(target.german, count * 4).forEach {
                if (picked.size < count && accept(it)) picked.add(it)
            }
        }
        picked
    }

    fun getFavoriteWords(): Flow<List<WordEntity>> {
        return wordDao.getFavoriteWords()
    }

    fun searchWordsLimited(query: String): Flow<List<WordEntity>> {
        return wordDao.searchWordsLimited(query)
    }

    suspend fun getWordById(id: Int): WordEntity? {
        return wordDao.getWordById(id)
    }

    fun getSubtopicProgress(subtopicId: String): Flow<SubtopicProgressEntity?> {
        return wordDao.getSubtopicProgress(subtopicId)
    }

    fun getAllSubtopicProgress(): Flow<List<SubtopicProgressEntity>> {
        return wordDao.getAllSubtopicProgress()
    }

    suspend fun initializePrepopulatedDataIfNeeded() = withContext(Dispatchers.IO) {
        val count = wordDao.getWordCount()
        val prefs = appContext?.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
        val seededCount = prefs?.getInt("vocab_seeded_v6_6000", -1) ?: -1
        if (count >= 6000 && count >= seededCount && seededCount > 0) {
            return@withContext
        }

        val initialWords = InitialVocabulary.getInitialWords(appContext)
        if (initialWords.isEmpty()) return@withContext

        val currentByGerman = if (count > 0) {
            wordDao.getAllWordsOnce().associateBy { it.german.lowercase() }
        } else {
            emptyMap()
        }

        val merged = initialWords.map { word ->
            val existing = currentByGerman[word.german.lowercase()]
            if (existing != null) {
                word.copy(
                    box = existing.box,
                    nextReviewTimestamp = existing.nextReviewTimestamp,
                    reviewCount = existing.reviewCount,
                    correctCount = existing.correctCount,
                    isFavorite = existing.isFavorite
                )
            } else {
                word
            }
        }

        // Preserve any custom user-imported words that aren't in the base dataset
        val baseKeys = initialWords.mapTo(HashSet()) { it.german.lowercase() }
        var nextCustomId = (merged.maxOfOrNull { it.id } ?: 6000) + 1
        val customExtras = currentByGerman.values
            .filter { it.german.lowercase() !in baseKeys }
            .map { it.copy(id = nextCustomId++) }

        val fullList = merged + customExtras
        wordDao.replaceAllWords(fullList)
        prefs?.edit()?.putInt("vocab_seeded_v6_6000", fullList.size)?.apply()
    }

    suspend fun exportWordsAsJson(): String = withContext(Dispatchers.IO) {
        val words = wordDao.getAllWordsOnce()
        val arr = JSONArray()
        for (w in words) {
            val obj = JSONObject().apply {
                put("german", w.german)
                put("article", w.article)
                put("plural", w.plural)
                put("transcription", w.transcription)
                put("translationUk", w.translationUk)
                put("translationEn", w.translationEn)
                put("exampleDe", w.exampleDe)
                put("exampleUk", w.exampleUk)
                put("categoryId", w.categoryId)
                put("subtopicId", w.subtopicId)
                put("level", w.level)
                put("box", w.box)
                put("isFavorite", w.isFavorite)
                put("reviewCount", w.reviewCount)
                put("correctCount", w.correctCount)
            }
            arr.put(obj)
        }
        val root = JSONObject().apply {
            put("app", "DeutschLern")
            put("version", 6)
            put("count", words.size)
            put("words", arr)
        }
        root.toString(2)
    }

    suspend fun exportWordsAsTsv(): String = withContext(Dispatchers.IO) {
        val words = wordDao.getAllWordsOnce()
        val sb = StringBuilder(words.size * 140)
        sb.appendLine("id\tgerman\tarticle\tplural\ttranscription\ttranslationUk\ttranslationEn\texampleDe\texampleUk\tcategoryId\tsubtopicId\tlevel\tbox\tisFavorite")
        for (w in words) {
            val clean = listOf(
                w.id.toString(),
                w.german,
                w.article,
                w.plural,
                w.transcription,
                w.translationUk,
                w.translationEn,
                w.exampleDe,
                w.exampleUk,
                w.categoryId,
                w.subtopicId,
                w.level,
                w.box.toString(),
                w.isFavorite.toString()
            ).map { it.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim() }
            sb.appendLine(clean.joinToString("\t"))
        }
        sb.toString()
    }

    suspend fun importWordsFromString(rawText: String): ImportSummary = withContext(Dispatchers.IO) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return@withContext ImportSummary(0, 0, 0)

        val validSubtopics = TopicCatalog.mainTopics.flatMap { it.subtopics }.associateBy { it.id }
        val defaultSubtopic = TopicCatalog.mainTopics.firstOrNull()?.subtopics?.firstOrNull()
        val parsed = mutableListOf<WordEntity>()
        var skipped = 0

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val array = if (trimmed.startsWith("{")) {
                JSONObject(trimmed).optJSONArray("words") ?: JSONArray()
            } else {
                JSONArray(trimmed)
            }
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val german = obj.optString("german", "").trim()
                val uk = obj.optString("translationUk", "").trim()
                val en = obj.optString("translationEn", "").ifBlank { uk }.trim()
                if (german.isEmpty() || uk.isEmpty()) {
                    skipped++
                    continue
                }
                val subIdRaw = obj.optString("subtopicId", "").trim()
                val resolvedSub = validSubtopics[subIdRaw] ?: defaultSubtopic
                val catId = obj.optString("categoryId", "").trim()
                    .ifBlank { resolvedSub?.mainTopicId ?: "food" }
                parsed.add(
                    WordEntity(
                        german = german,
                        article = obj.optString("article", "").trim(),
                        plural = obj.optString("plural", "").trim(),
                        transcription = obj.optString("transcription", "").trim(),
                        translationUk = uk,
                        translationEn = en,
                        exampleDe = obj.optString("exampleDe", "").trim(),
                        exampleUk = obj.optString("exampleUk", "").trim(),
                        categoryId = catId,
                        subtopicId = resolvedSub?.id ?: subIdRaw.ifBlank { "food_fruits" },
                        level = obj.optString("level", "A2").trim().uppercase().let {
                            if (it in listOf("A1", "A2", "B1", "B2")) it else "A2"
                        },
                        box = obj.optInt("box", 1).coerceIn(1, 5),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        reviewCount = obj.optInt("reviewCount", 0),
                        correctCount = obj.optInt("correctCount", 0)
                    )
                )
            }
        } else {
            val lines = trimmed.lines()
            for (line in lines) {
                if (line.isBlank()) continue
                val cols = line.split('\t')
                if (cols.getOrNull(1)?.equals("german", ignoreCase = true) == true ||
                    cols.getOrNull(0)?.equals("id", ignoreCase = true) == true
                ) {
                    continue
                }
                // Support both 12/14 column TSV (with id at 0) and simple 3+ column TSV (german, uk, en...)
                val word = if (cols.size >= 12 && cols[0].trim().toIntOrNull() != null) {
                    val subRaw = cols[10].trim()
                    val resolvedSub = validSubtopics[subRaw] ?: defaultSubtopic
                    WordEntity(
                        german = cols[1].trim(),
                        article = cols[2].trim(),
                        plural = cols[3].trim(),
                        transcription = cols[4].trim(),
                        translationUk = cols[5].trim(),
                        translationEn = cols[6].trim().ifBlank { cols[5].trim() },
                        exampleDe = cols[7].trim(),
                        exampleUk = cols[8].trim(),
                        categoryId = cols[9].trim().ifBlank { resolvedSub?.mainTopicId ?: "food" },
                        subtopicId = resolvedSub?.id ?: subRaw,
                        level = cols[11].trim().uppercase().let { if (it in listOf("A1", "A2", "B1", "B2")) it else "A2" },
                        box = cols.getOrNull(12)?.trim()?.toIntOrNull()?.coerceIn(1, 5) ?: 1,
                        isFavorite = cols.getOrNull(13)?.trim()?.toBooleanStrictOrNull() ?: false
                    )
                } else if (cols.size >= 2) {
                    val g = cols[0].trim()
                    val uk = cols[1].trim()
                    val en = cols.getOrNull(2)?.trim().orEmpty().ifBlank { uk }
                    if (g.isEmpty() || uk.isEmpty()) {
                        skipped++
                        continue
                    }
                    WordEntity(
                        german = g,
                        article = cols.getOrNull(3)?.trim().orEmpty(),
                        translationUk = uk,
                        translationEn = en,
                        categoryId = defaultSubtopic?.mainTopicId ?: "food",
                        subtopicId = defaultSubtopic?.id ?: "food_fruits",
                        level = "A2"
                    )
                } else {
                    skipped++
                    continue
                }
                if (word.german.isBlank() || word.translationUk.isBlank()) {
                    skipped++
                } else {
                    parsed.add(word)
                }
            }
        }

        if (parsed.isEmpty()) return@withContext ImportSummary(0, 0, skipped)

        val existingMap = wordDao.getAllWordsOnce().associateBy { it.german.lowercase() }
        var nextId = (existingMap.values.maxOfOrNull { it.id } ?: 0) + 1
        var imported = 0
        var updated = 0
        val toInsert = mutableListOf<WordEntity>()

        for (w in parsed) {
            val match = existingMap[w.german.lowercase()]
            if (match != null) {
                updated++
                toInsert.add(
                    w.copy(
                        id = match.id,
                        box = maxOf(match.box, w.box),
                        isFavorite = match.isFavorite || w.isFavorite,
                        reviewCount = maxOf(match.reviewCount, w.reviewCount),
                        correctCount = maxOf(match.correctCount, w.correctCount),
                        nextReviewTimestamp = match.nextReviewTimestamp
                    )
                )
            } else {
                imported++
                toInsert.add(w.copy(id = nextId++))
            }
        }

        toInsert.chunked(500).forEach { wordDao.insertAll(it) }
        val newTotal = wordDao.getWordCount()
        appContext?.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
            ?.edit()?.putInt("vocab_seeded_v6_6000", newTotal)?.apply()

        ImportSummary(imported, updated, skipped)
    }

    fun getWordSrsStep(word: WordEntity): Int {
        val stored = srsPrefs?.getInt("step_${word.id}", -1) ?: -1
        if (stored >= 0) return stored.coerceIn(0, SpacedRepetition.TARGET_REPETITIONS)
        return if (word.nextReviewTimestamp > 0L) {
            ((word.box - 1) * 3).coerceIn(0, 12)
        } else {
            0
        }
    }

    private fun saveWordSrsStep(wordId: Int, step: Int) {
        srsPrefs?.edit()?.putInt("step_$wordId", step.coerceIn(0, SpacedRepetition.TARGET_REPETITIONS))?.apply()
    }

    private fun countMastered16StepWords(): Int {
        val all = srsPrefs?.all ?: return 0
        var count = 0
        for ((k, v) in all) {
            if (k.startsWith("step_") && (v as? Int ?: 0) >= SpacedRepetition.TARGET_REPETITIONS) {
                count++
            }
        }
        return count
    }

    suspend fun getReviewDashboardSnapshot(now: Long = System.currentTimeMillis()): ReviewDashboardSnapshot =
        withContext(Dispatchers.IO) {
            ReviewDashboardSnapshot(
                dueCount = wordDao.countDueEnrolledWords(now),
                enrolledCount = wordDao.countEnrolledWords(),
                mastered16Count = countMastered16StepWords(),
                nextReviewTimestamp = wordDao.getNextUpcomingReviewTimestamp(now)
            )
        }

    suspend fun getDueWordsForMixedReview(
        now: Long = System.currentTimeMillis(),
        limit: Int = 15
    ): List<WordEntity> = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext emptyList()
        wordDao.getDueEnrolledWordsOnce(now, limit)
    }

    suspend fun getEarlyWordsForMixedReview(limit: Int = 10): List<WordEntity> = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext emptyList()
        wordDao.getEarlyReviewEnrolledWordsOnce(limit)
    }

    suspend fun enrollSubtopicWordsForSrs(
        subtopicId: String,
        level: String,
        now: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val words = getWordsBySubtopicAndLevelOnce(subtopicId, level, 15)
        val firstDue = SpacedRepetition.initialEnrollmentTimestamp(now)
        for (word in words) {
            if (word.nextReviewTimestamp == 0L) {
                wordDao.enrollWordForSrs(word.id, firstDue)
                if ((srsPrefs?.getInt("step_${word.id}", -1) ?: -1) < 0) {
                    saveWordSrsStep(word.id, 0)
                }
            }
        }
    }

    private fun stageSetKey(subtopicId: String, level: String): String =
        "stages_${subtopicId}_${level.uppercase()}"

    fun getCompletedStageSet(
        subtopicId: String,
        level: String,
        fallbackProgress: SubtopicProgressEntity? = null
    ): Set<Int> {
        val raw = subtopicStagesPrefs?.getString(stageSetKey(subtopicId, level), null)
        if (raw != null) {
            return raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..TOTAL_STAGES }
                .toSet()
        }
        val legacyCount = fallbackProgress?.completedStages ?: 0
        return if (legacyCount >= TOTAL_STAGES) {
            ALL_STAGES
        } else if (legacyCount > 0) {
            (1..legacyCount).toSet()
        } else {
            emptySet()
        }
    }

    private fun saveCompletedStageSet(subtopicId: String, level: String, stages: Set<Int>) {
        subtopicStagesPrefs?.edit()
            ?.putString(stageSetKey(subtopicId, level), stages.sorted().joinToString(","))
            ?.apply()
    }

    /**
     * Marks a stage as completed (only called once the learner reaches 100% mastery of that stage
     * via cyclic mistake repetition). Spaced repetition enrollment happens STRICTLY after all 8
     * stages (1..8) have been completed at 100% for this subtopic.
     *
     * @return true if this completion unlocked Spaced Repetition (all 8 stages reached 100%).
     */
    suspend fun completeStage(
        subtopicId: String,
        stage: Int,
        score: Int,
        enabledStages: Set<Int> = ALL_STAGES,
        level: String = "A1",
        achieved100Percent: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val currentProgress = wordDao.getSubtopicProgress(subtopicId).firstOrNull()
            ?: SubtopicProgressEntity(subtopicId = subtopicId)
        val completedSet = getCompletedStageSet(subtopicId, level, currentProgress).toMutableSet()
        if (achieved100Percent && stage in 1..TOTAL_STAGES) {
            completedSet.add(stage)
            saveCompletedStageSet(subtopicId, level, completedSet)
        }

        val allStagesMastered = isAllStagesCompleted(completedSet)
        val nextEnabled = enabledStages.filter { it > stage }.minOrNull()
        val newUnlocked = if (nextEnabled != null) {
            maxOf(currentProgress.unlockedStage, nextEnabled)
        } else {
            TOTAL_STAGES
        }
        val newCompleted = if (allStagesMastered) {
            TOTAL_STAGES
        } else {
            maxOf(
                currentProgress.completedStages.coerceAtMost(TOTAL_STAGES - 1),
                completedSet.size.coerceAtMost(TOTAL_STAGES - 1)
            )
        }
        val newStars = currentProgress.stars + if (score >= 4) 3 else if (score >= 2) 2 else 1

        val updated = when (stage) {
            1 -> currentProgress.copy(unlockedStage = newUnlocked, completedStages = newCompleted, stars = newStars, stage1Score = score)
            2 -> currentProgress.copy(unlockedStage = newUnlocked, completedStages = newCompleted, stars = newStars, stage2Score = score)
            3 -> currentProgress.copy(unlockedStage = newUnlocked, completedStages = newCompleted, stars = newStars, stage3Score = score)
            4 -> currentProgress.copy(unlockedStage = newUnlocked, completedStages = newCompleted, stars = newStars, stage4Score = score)
            5 -> currentProgress.copy(unlockedStage = newUnlocked, completedStages = newCompleted, stars = newStars, stage5Score = score)
            6 -> currentProgress.copy(unlockedStage = newUnlocked, completedStages = newCompleted, stars = newStars, stage6Score = score)
            7, 8 -> currentProgress.copy(unlockedStage = newUnlocked, completedStages = newCompleted, stars = newStars)
            else -> currentProgress
        }
        wordDao.saveSubtopicProgress(updated)

        // Enroll subtopic words into the 6h -> 16-repetition cycle ONLY when all 8 stages are 100% done!
        if (allStagesMastered) {
            enrollSubtopicWordsForSrs(subtopicId, level)
        }
        allStagesMastered
    }

    /**
     * Records a practice answer during initial subtopic stages (Stages 1..8).
     * Never enrolls words prematurely into Spaced Repetition; enrollment happens strictly
     * after all 8 stages of the subtopic are completed at 100%.
     */
    suspend fun recordReview(wordId: Int, rating: ReviewRating) = withContext(Dispatchers.IO) {
        val isCorrectDelta = if (rating != ReviewRating.AGAIN) 1 else 0
        wordDao.incrementWordCountsOnly(wordId, isCorrectDelta)
    }

    /**
     * Records an answer inside the Spaced-Repetition Mixed Review tab,
     * advancing or gently stepping back the 16-repetition schedule.
     */
    suspend fun recordSpacedReview(wordId: Int, isCorrect: Boolean): SrsStepResult? = withContext(Dispatchers.IO) {
        val word = wordDao.getWordById(wordId) ?: return@withContext null
        val currentStep = getWordSrsStep(word)
        val result = SpacedRepetition.calculate16StepReview(currentStep, isCorrect)
        saveWordSrsStep(wordId, result.newStep)
        wordDao.updateReviewResult(
            id = wordId,
            newBox = result.newBox,
            nextReview = result.nextReviewTimestamp,
            isCorrectDelta = if (isCorrect) 1 else 0
        )
        result
    }

    suspend fun toggleFavorite(wordId: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        wordDao.updateFavorite(wordId, isFavorite)
    }

    suspend fun resetAllProgress() = withContext(Dispatchers.IO) {
        val initialWords = InitialVocabulary.getInitialWords(appContext)
        wordDao.deleteAllWords()
        initialWords.chunked(500).forEach { chunk ->
            wordDao.insertAll(chunk)
        }
        srsPrefs?.edit()?.clear()?.apply()
        subtopicStagesPrefs?.edit()?.clear()?.apply()
    }

    companion object {
        const val TOTAL_STAGES: Int = 8
        val ALL_STAGES: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7, 8)

        internal fun isAllStagesCompleted(completedStages: Set<Int>): Boolean {
            return ALL_STAGES.all { it in completedStages }
        }
    }
}

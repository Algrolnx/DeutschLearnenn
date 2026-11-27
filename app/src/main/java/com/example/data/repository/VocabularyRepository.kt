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
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ImportSummary(
    val importedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val restoredSubtopicsCount: Int = 0,
    val restoredSrsWordsCount: Int = 0
)

data class ReviewDashboardSnapshot(
    val dueCount: Int = 0,
    val dueLaterTodayCount: Int = 0,
    val nextBatchCount: Int = 0,
    val enrolledCount: Int = 0,
    val reviewedInCycleCount: Int = 0,
    val mastered16Count: Int = 0,
    val totalCycleStepsDone: Int = 0,
    val totalCycleStepsTarget: Int = 0,
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
        val seededCount = prefs?.getInt(SEEDED_VOCAB_PREF_KEY, -1) ?: -1
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

        // Preserve any custom user-imported words (id > 6000) that aren't in the base dataset
        val baseKeys = initialWords.mapTo(HashSet()) { it.german.lowercase() }
        var nextCustomId = (merged.maxOfOrNull { it.id } ?: 6000) + 1
        val customExtras = currentByGerman.values
            .filter { it.id > 6000 && it.german.lowercase() !in baseKeys }
            .map { it.copy(id = nextCustomId++) }

        val fullList = merged + customExtras
        wordDao.replaceAllWords(fullList)
        prefs?.edit()?.putInt(SEEDED_VOCAB_PREF_KEY, fullList.size)?.apply()
    }

    suspend fun exportWordsAsJson(): String = withContext(Dispatchers.IO) {
        val words = wordDao.getAllWordsOnce()
        val arr = JSONArray()
        for (w in words) {
            val srsStep = getWordSrsStep(w)
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
                put("nextReviewTimestamp", w.nextReviewTimestamp)
                put("srsStep", srsStep)
            }
            arr.put(obj)
        }

        // Export Room subtopic progress rows
        val progressRows = wordDao.getAllSubtopicProgressOnce()
        val progressArr = JSONArray()
        for (p in progressRows) {
            progressArr.put(
                JSONObject().apply {
                    put("subtopicId", p.subtopicId)
                    put("unlockedStage", p.unlockedStage)
                    put("completedStages", p.completedStages)
                    put("stars", p.stars)
                    put("stage1Score", p.stage1Score)
                    put("stage2Score", p.stage2Score)
                    put("stage3Score", p.stage3Score)
                    put("stage4Score", p.stage4Score)
                    put("stage5Score", p.stage5Score)
                    put("stage6Score", p.stage6Score)
                }
            )
        }

        // Export per-level stage checkmarks (stages_<subtopicId>_<level>)
        val stagesObj = JSONObject()
        subtopicStagesPrefs?.all?.forEach { (k, v) ->
            if (k.startsWith("stages_") && v is String) {
                stagesObj.put(k, v)
            }
        }

        // Export important sentences progress
        val sentencePrefs = appContext?.getSharedPreferences("deutschlern_important_sentences", Context.MODE_PRIVATE)
        val sentencesObj = JSONObject().apply {
            put("learnedIds", sentencePrefs?.getString("learned_sentence_ids", "") ?: "")
            put("favoriteIds", sentencePrefs?.getString("favorite_sentence_ids", "") ?: "")
        }

        // Export core learner preferences & streak
        val mainPrefs = appContext?.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
        val prefsObj = JSONObject().apply {
            mainPrefs?.getString("selected_cefr_level", null)?.let { put("selectedLevel", it) }
            mainPrefs?.getString("enabled_stages_csv", null)?.let { put("enabledStagesCsv", it) }
            if (mainPrefs?.contains("daily_review_limit") == true) {
                put("dailyReviewLimit", mainPrefs.getInt("daily_review_limit", 60))
            }
            mainPrefs?.getString("stage5_fillblank_input_mode", null)?.let { put("stage5InputMode", it) }
            mainPrefs?.getString("stage6_input_mode", null)?.let { put("stage6InputMode", it) }
            if (mainPrefs?.contains("streak_days") == true) {
                put("streakDays", mainPrefs.getInt("streak_days", 0))
            }
            mainPrefs?.getString("streak_last_active_date", null)?.let { put("streakLastActiveDate", it) }
            if (mainPrefs?.contains("daily_review_count") == true) {
                put("dailyReviewCount", mainPrefs.getInt("daily_review_count", 0))
            }
            mainPrefs?.getString("daily_review_date", null)?.let { put("dailyReviewDate", it) }
        }

        val root = JSONObject().apply {
            put("app", "DeutschLern")
            put("version", 8)
            put("exportedAt", System.currentTimeMillis())
            put("count", words.size)
            put("words", arr)
            put("subtopicProgress", progressArr)
            put("subtopicStages", stagesObj)
            put("importantSentences", sentencesObj)
            put("preferences", prefsObj)
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
        val trimmed = rawText.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return@withContext ImportSummary(0, 0, 0)

        val validSubtopics = TopicCatalog.mainTopics.flatMap { it.subtopics }.associateBy { it.id }
        val defaultSubtopic = TopicCatalog.mainTopics.firstOrNull()?.subtopics?.firstOrNull()
        val parsed = mutableListOf<Pair<WordEntity, Int?>>() // word + optional srsStep
        var skipped = 0
        var rootObj: JSONObject? = null

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val array = if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                rootObj = obj
                obj.optJSONArray("words") ?: JSONArray()
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
                val step = if (obj.has("srsStep")) obj.optInt("srsStep", -1).takeIf { it >= 0 } else null
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
                        correctCount = obj.optInt("correctCount", 0),
                        nextReviewTimestamp = obj.optLong("nextReviewTimestamp", 0L).coerceAtLeast(0L)
                    ) to step
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
                    parsed.add(word to null)
                }
            }
        }

        val existingWords = wordDao.getAllWordsOnce()
        val existingByComposite = existingWords.associateBy {
            "${it.subtopicId}|${it.level.uppercase()}|${it.german.lowercase()}"
        }
        val existingByGerman = existingWords.associateBy { it.german.lowercase() }
        var nextId = (existingWords.maxOfOrNull { it.id } ?: 0) + 1
        var imported = 0
        var updated = 0
        var restoredSrsWords = 0
        val toInsert = mutableListOf<WordEntity>()
        val srsEditor = srsPrefs?.edit()

        for ((w, importedStep) in parsed) {
            val compositeKey = "${w.subtopicId}|${w.level.uppercase()}|${w.german.lowercase()}"
            val match = existingByComposite[compositeKey] ?: existingByGerman[w.german.lowercase()]
            val resolvedNextTs = when {
                w.nextReviewTimestamp > 0L && (match?.nextReviewTimestamp ?: 0L) > 0L ->
                    maxOf(match!!.nextReviewTimestamp, w.nextReviewTimestamp)
                w.nextReviewTimestamp > 0L -> w.nextReviewTimestamp
                else -> match?.nextReviewTimestamp ?: 0L
            }
            val finalEntity = if (match != null) {
                updated++
                // Keep fresh built-in curated text if this is a standard word, while restoring all user progress!
                match.copy(
                    box = maxOf(match.box, w.box),
                    isFavorite = match.isFavorite || w.isFavorite,
                    reviewCount = maxOf(match.reviewCount, w.reviewCount),
                    correctCount = maxOf(match.correctCount, w.correctCount),
                    nextReviewTimestamp = resolvedNextTs
                )
            } else {
                imported++
                w.copy(id = nextId++, nextReviewTimestamp = resolvedNextTs)
            }
            toInsert.add(finalEntity)
            if (resolvedNextTs > 0L) {
                restoredSrsWords++
            }
            if (importedStep != null) {
                val existingStep = srsPrefs?.getInt("step_${finalEntity.id}", -1) ?: -1
                val mergedStep = maxOf(existingStep, importedStep).coerceIn(0, SpacedRepetition.TARGET_REPETITIONS)
                srsEditor?.putInt("step_${finalEntity.id}", mergedStep)
            }
        }
        srsEditor?.apply()

        if (toInsert.isNotEmpty()) {
            toInsert.chunked(500).forEach { wordDao.insertAll(it) }
            val newTotal = wordDao.getWordCount()
            appContext?.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
                ?.edit()?.putInt(SEEDED_VOCAB_PREF_KEY, newTotal)?.apply()
        }

        // Restore full backup metadata (subtopic progress, per-level stages, sentences, preferences)
        var restoredSubtopics = 0
        if (rootObj != null) {
            restoredSubtopics = restoreBackupMetadata(rootObj)
        }

        ImportSummary(
            importedCount = imported,
            updatedCount = updated,
            skippedCount = skipped,
            restoredSubtopicsCount = restoredSubtopics,
            restoredSrsWordsCount = restoredSrsWords
        )
    }

    private suspend fun restoreBackupMetadata(root: JSONObject): Int {
        var restoredSubtopics = 0

        // 1. Restore Room subtopic_progress rows (merging max progress so existing progress is never lost)
        val progressArr = root.optJSONArray("subtopicProgress")
        if (progressArr != null && progressArr.length() > 0) {
            val existingById = wordDao.getAllSubtopicProgressOnce().associateBy { it.subtopicId }
            val mergedRows = mutableListOf<SubtopicProgressEntity>()
            for (i in 0 until progressArr.length()) {
                val obj = progressArr.optJSONObject(i) ?: continue
                val subId = obj.optString("subtopicId", "").trim()
                if (subId.isEmpty()) continue
                val existing = existingById[subId]
                val restored = SubtopicProgressEntity(
                    subtopicId = subId,
                    unlockedStage = maxOf(existing?.unlockedStage ?: 1, obj.optInt("unlockedStage", 1)),
                    completedStages = maxOf(existing?.completedStages ?: 0, obj.optInt("completedStages", 0)),
                    stars = maxOf(existing?.stars ?: 0, obj.optInt("stars", 0)),
                    stage1Score = maxOf(existing?.stage1Score ?: 0, obj.optInt("stage1Score", 0)),
                    stage2Score = maxOf(existing?.stage2Score ?: 0, obj.optInt("stage2Score", 0)),
                    stage3Score = maxOf(existing?.stage3Score ?: 0, obj.optInt("stage3Score", 0)),
                    stage4Score = maxOf(existing?.stage4Score ?: 0, obj.optInt("stage4Score", 0)),
                    stage5Score = maxOf(existing?.stage5Score ?: 0, obj.optInt("stage5Score", 0)),
                    stage6Score = maxOf(existing?.stage6Score ?: 0, obj.optInt("stage6Score", 0))
                )
                mergedRows.add(restored)
                if (restored.completedStages > 0 || restored.stars > 0) {
                    restoredSubtopics++
                }
            }
            if (mergedRows.isNotEmpty()) {
                wordDao.saveAllSubtopicProgress(mergedRows)
            }
        }

        // 2. Restore per-level stage checkmarks (union of completed stages)
        val stagesObj = root.optJSONObject("subtopicStages")
        if (stagesObj != null) {
            val editor = subtopicStagesPrefs?.edit()
            val keys = stagesObj.keys()
            var stageEntriesRestored = 0
            while (keys.hasNext()) {
                val key = keys.next()
                if (!key.startsWith("stages_")) continue
                val importedRaw = stagesObj.optString(key, "")
                val importedSet = importedRaw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..TOTAL_STAGES }
                if (importedSet.isEmpty()) continue
                val existingRaw = subtopicStagesPrefs?.getString(key, "").orEmpty()
                val existingSet = existingRaw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..TOTAL_STAGES }
                val merged = (existingSet + importedSet).toSortedSet()
                editor?.putString(key, merged.joinToString(","))
                stageEntriesRestored++
            }
            editor?.apply()
            if (stageEntriesRestored > restoredSubtopics) {
                restoredSubtopics = stageEntriesRestored
            }
        }

        // 3. Restore Important Sentences learned & favorites (union)
        val sentencesObj = root.optJSONObject("importantSentences")
        if (sentencesObj != null) {
            val sPrefs = appContext?.getSharedPreferences("deutschlern_important_sentences", Context.MODE_PRIVATE)
            if (sPrefs != null) {
                fun mergeCsvIds(existingCsv: String?, importedCsv: String): String {
                    val a = existingCsv.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }
                    val b = importedCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
                    return (a + b).toSortedSet().joinToString(",")
                }
                val learned = mergeCsvIds(sPrefs.getString("learned_sentence_ids", ""), sentencesObj.optString("learnedIds", ""))
                val favorites = mergeCsvIds(sPrefs.getString("favorite_sentence_ids", ""), sentencesObj.optString("favoriteIds", ""))
                sPrefs.edit()
                    .putString("learned_sentence_ids", learned)
                    .putString("favorite_sentence_ids", favorites)
                    .apply()
            }
        }

        // 4. Restore learner preferences & streak
        val prefsObj = root.optJSONObject("preferences")
        val mainPrefs = appContext?.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
        if (prefsObj != null && mainPrefs != null) {
            val editor = mainPrefs.edit()
            prefsObj.optString("selectedLevel", "").takeIf { it in listOf("A1", "A2", "B1", "B2") }?.let {
                editor.putString("selected_cefr_level", it)
            }
            prefsObj.optString("enabledStagesCsv", "").takeIf { it.isNotBlank() }?.let {
                editor.putString("enabled_stages_csv", it)
                editor.putBoolean("stages_migrated_to_8", true)
            }
            if (prefsObj.has("dailyReviewLimit")) {
                editor.putInt("daily_review_limit", prefsObj.optInt("dailyReviewLimit", 60).coerceIn(20, 100))
            }
            prefsObj.optString("stage5InputMode", "").takeIf { it.isNotBlank() }?.let {
                editor.putString("stage5_fillblank_input_mode", it)
            }
            prefsObj.optString("stage6InputMode", "").takeIf { it.isNotBlank() }?.let {
                editor.putString("stage6_input_mode", it)
            }
            val importedStreak = when {
                prefsObj.has("streakDays") -> prefsObj.optInt("streakDays", 0)
                prefsObj.has("streakCount") -> prefsObj.optInt("streakCount", 0)
                else -> -1
            }
            if (importedStreak >= 0) {
                val restoredStreak = maxOf(mainPrefs.getInt("streak_days", 0), importedStreak)
                editor.putInt("streak_days", restoredStreak)
            }
            val importedLastDate = prefsObj.optString("streakLastActiveDate", "")
                .ifBlank { prefsObj.optString("lastStudyDate", "") }
            if (importedLastDate.isNotBlank()) {
                editor.putString("streak_last_active_date", importedLastDate)
            }
            val importedDailyCount = when {
                prefsObj.has("dailyReviewCount") -> prefsObj.optInt("dailyReviewCount", 0)
                prefsObj.has("reviewsDoneTodayCount") -> prefsObj.optInt("reviewsDoneTodayCount", 0)
                else -> -1
            }
            if (importedDailyCount >= 0) {
                editor.putInt("daily_review_count", importedDailyCount)
            }
            val importedDailyDate = prefsObj.optString("dailyReviewDate", "")
                .ifBlank { prefsObj.optString("reviewsDoneTodayDate", "") }
            if (importedDailyDate.isNotBlank()) {
                editor.putString("daily_review_date", importedDailyDate)
            }
            editor.apply()
        }

        return restoredSubtopics
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
            val enrolledWords = wordDao.getEnrolledWordsOnce()
            val endOfToday = endOfLocalDayTimestamp(now)
            var dueNowCount = 0
            var dueLaterTodayCount = 0
            var reviewedInCycleCount = 0
            var mastered16Count = 0
            var totalStepsDone = 0
            var nextReviewTs: Long? = null

            for (word in enrolledWords) {
                val step = getWordSrsStep(word)
                totalStepsDone += step
                if (step >= 1) reviewedInCycleCount++
                if (step >= SpacedRepetition.TARGET_REPETITIONS) mastered16Count++

                val ts = word.nextReviewTimestamp
                if (ts <= now) {
                    dueNowCount++
                } else {
                    if (ts <= endOfToday) {
                        dueLaterTodayCount++
                    }
                    if (nextReviewTs == null || ts < nextReviewTs) {
                        nextReviewTs = ts
                    }
                }
            }

            val nextBatchCount = if (nextReviewTs != null) {
                val batchWindowEnd = nextReviewTs + TimeUnit.MINUTES.toMillis(15L)
                enrolledWords.count { it.nextReviewTimestamp in nextReviewTs..batchWindowEnd }
            } else {
                0
            }

            val enrolledCount = enrolledWords.size
            ReviewDashboardSnapshot(
                dueCount = dueNowCount,
                dueLaterTodayCount = dueLaterTodayCount,
                nextBatchCount = nextBatchCount,
                enrolledCount = enrolledCount,
                reviewedInCycleCount = reviewedInCycleCount,
                mastered16Count = mastered16Count,
                totalCycleStepsDone = totalStepsDone,
                totalCycleStepsTarget = enrolledCount * SpacedRepetition.TARGET_REPETITIONS,
                nextReviewTimestamp = nextReviewTs
            )
        }

    private fun endOfLocalDayTimestamp(now: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
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

    private fun hasAnyPerLevelStageKey(subtopicId: String): Boolean {
        val prefix = "stages_${subtopicId}_"
        return subtopicStagesPrefs?.all?.keys?.any { it.startsWith(prefix) } == true
    }

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
        // Only apply legacy cross-level Room fallback to A1 when no per-level keys exist yet
        // so completing stages in A1 never leaks into A2 / B1 / B2.
        if (!level.equals("A1", ignoreCase = true) || hasAnyPerLevelStageKey(subtopicId)) {
            return emptySet()
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

        val targetStages = enabledStages.ifEmpty { ALL_STAGES }
        val allStagesMastered = isAllStagesCompleted(completedSet, targetStages)
        val nextEnabled = targetStages.filter { it > stage }.minOrNull()
        val newUnlocked = if (nextEnabled != null) {
            maxOf(currentProgress.unlockedStage, nextEnabled)
        } else {
            TOTAL_STAGES
        }
        val completedInTargetCount = completedSet.count { it in targetStages }
        val newCompleted = if (allStagesMastered) {
            TOTAL_STAGES
        } else {
            completedInTargetCount.coerceAtMost(TOTAL_STAGES - 1)
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

        // Enroll subtopic words into the 6h -> 16-repetition cycle once 100% of enabled stages are mastered!
        if (allStagesMastered) {
            enrollSubtopicWordsForSrs(subtopicId, level)
        }
        allStagesMastered
    }

    /**
     * When the learner reduces their enabled stages (e.g., from 8 down to 6), any subtopic
     * where all currently enabled stages are already completed reaches 100% and enrolls in SRS.
     */
    suspend fun enrollCompletedSubtopicsForEnabledStages(enabledStages: Set<Int>) = withContext(Dispatchers.IO) {
        val targetStages = enabledStages.ifEmpty { ALL_STAGES }
        val allEntries = subtopicStagesPrefs?.all ?: return@withContext
        for ((key, value) in allEntries) {
            if (!key.startsWith("stages_") || value !is String) continue
            val withoutPrefix = key.removePrefix("stages_")
            val lastUnderscore = withoutPrefix.lastIndexOf('_')
            if (lastUnderscore <= 0) continue
            val subtopicId = withoutPrefix.substring(0, lastUnderscore)
            val level = withoutPrefix.substring(lastUnderscore + 1).uppercase()
            val completedSet = value.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..TOTAL_STAGES }
                .toSet()
            if (isAllStagesCompleted(completedSet, targetStages)) {
                enrollSubtopicWordsForSrs(subtopicId, level)
            }
        }
    }

    /**
     * Records a practice answer during initial subtopic stages (Stages 1..8).
     * Never enrolls words prematurely into Spaced Repetition; enrollment happens strictly
     * after all enabled stages of the subtopic are completed at 100%.
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
        if (initialWords.isNotEmpty()) {
            wordDao.replaceAllWords(initialWords)
        } else {
            wordDao.deleteAllWords()
        }
        wordDao.deleteAllSubtopicProgress()
        srsPrefs?.edit()?.clear()?.apply()
        subtopicStagesPrefs?.edit()?.clear()?.apply()
    }

    companion object {
        private const val SEEDED_VOCAB_PREF_KEY = "vocab_seeded_v8_6000"
        const val TOTAL_STAGES: Int = 8
        val ALL_STAGES: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7, 8)

        internal fun isAllStagesCompleted(
            completedStages: Set<Int>,
            requiredStages: Set<Int> = ALL_STAGES
        ): Boolean {
            val target = requiredStages.ifEmpty { ALL_STAGES }
            return target.all { it in completedStages }
        }
    }
}

package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.ImportantSentence
import com.example.data.model.SentenceSituationGroup
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Loads the bundled offline catalog of 600 essential German sentences (150 each for A1, A2, B1, B2)
 * organized by real-life situations, and persists learned / favorite sentence IDs in SharedPreferences.
 */
class ImportantSentencesRepository(context: Context? = null) {

    private val appContext = context?.applicationContext
    private val prefs by lazy {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Volatile
    private var cachedSentences: List<ImportantSentence>? = null

    fun getAllSentences(): List<ImportantSentence> {
        cachedSentences?.let { return it }
        val loaded = loadSentencesFromAssets(appContext)
        cachedSentences = loaded
        return loaded
    }

    fun getSentencesByLevel(level: String): List<ImportantSentence> {
        val norm = level.uppercase()
        return getAllSentences().filter { it.level == norm }
    }

    fun getSituationGroupsByLevel(level: String): List<SentenceSituationGroup> {
        val byLevel = getSentencesByLevel(level)
        if (byLevel.isEmpty()) return emptyList()
        val ordered = LinkedHashMap<String, MutableList<ImportantSentence>>()
        for (sentence in byLevel) {
            ordered.getOrPut(sentence.situationId) { mutableListOf() }.add(sentence)
        }
        return ordered.values.map { list ->
            val first = list.first()
            SentenceSituationGroup(
                level = first.level,
                situationId = first.situationId,
                titleDe = first.situationDe,
                titleUk = first.situationUk,
                sentences = list
            )
        }
    }

    fun getLearnedIds(): Set<Int> {
        val raw = prefs?.getString(KEY_LEARNED_IDS, null).orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun saveLearnedIds(ids: Set<Int>) {
        prefs?.edit()?.putString(KEY_LEARNED_IDS, ids.sorted().joinToString(","))?.apply()
    }

    fun getFavoriteIds(): Set<Int> {
        val raw = prefs?.getString(KEY_FAVORITE_IDS, null).orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun saveFavoriteIds(ids: Set<Int>) {
        prefs?.edit()?.putString(KEY_FAVORITE_IDS, ids.sorted().joinToString(","))?.apply()
    }

    companion object {
        private const val TAG = "ImportantSentencesRepo"
        private const val ASSET_FILE = "important_sentences.tsv"
        private const val PREFS_NAME = "deutschlern_important_sentences"
        private const val KEY_LEARNED_IDS = "learned_sentence_ids"
        private const val KEY_FAVORITE_IDS = "favorite_sentence_ids"

        internal fun loadSentencesFromAssets(context: Context? = null): List<ImportantSentence> {
            val stream = openAssetStream(context) ?: return emptyList()
            return try {
                stream.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                        lines.mapNotNull { line -> parseLine(line) }.toList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load $ASSET_FILE: ${e.message}", e)
                emptyList()
            }
        }

        private fun openAssetStream(context: Context?): InputStream? {
            if (context != null) {
                try {
                    return context.assets.open(ASSET_FILE)
                } catch (_: Exception) {
                }
            }
            val candidatePaths = listOf(
                "app/src/main/assets/$ASSET_FILE",
                "src/main/assets/$ASSET_FILE",
                "assets/$ASSET_FILE"
            )
            for (path in candidatePaths) {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    return file.inputStream()
                }
            }
            return ImportantSentencesRepository::class.java.classLoader?.getResourceAsStream(ASSET_FILE)
        }

        private fun parseLine(rawLine: String): ImportantSentence? {
            if (rawLine.isBlank() || rawLine.startsWith("#")) return null
            val cols = rawLine.split('\t')
            if (cols.size < 7) return null
            val id = cols[0].trim().toIntOrNull() ?: return null
            val level = cols[1].trim().uppercase()
            val situationId = cols[2].trim()
            val situationDe = cols[3].trim()
            val situationUk = cols[4].trim()
            val german = cols[5].trim()
            val ukrainian = cols[6].trim()
            val noteUk = cols.getOrNull(7)?.trim().orEmpty()
            if (german.isEmpty() || ukrainian.isEmpty() || situationId.isEmpty()) return null
            return ImportantSentence(
                id = id,
                level = level,
                situationId = situationId,
                situationDe = situationDe,
                situationUk = situationUk,
                german = german,
                ukrainian = ukrainian,
                noteUk = noteUk
            )
        }
    }
}

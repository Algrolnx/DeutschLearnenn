package com.example.data.repository

import android.content.Context
import com.example.data.model.WordEntity
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

object InitialVocabulary {

    private const val ASSET_FILE = "vocabulary.tsv"

    /**
     * Streams the 6,000-word German dataset from assets/vocabulary.tsv (or local filesystem in JVM unit tests).
     * Tab-separated columns (12):
     * 0:id, 1:german, 2:article, 3:plural, 4:transcription,
     * 5:translationUk, 6:translationEn, 7:exampleDe, 8:exampleUk,
     * 9:categoryId, 10:subtopicId, 11:level
     */
    fun getInitialWords(context: Context? = null): List<WordEntity> {
        val stream = openVocabularyStream(context) ?: return emptyList()
        return stream.use { parseTsvStream(it) }
    }

    private fun openVocabularyStream(context: Context?): InputStream? {
        if (context != null) {
            try {
                return context.assets.open(ASSET_FILE)
            } catch (_: Exception) {
                // Fall back to filesystem check below for Robolectric/unit test environments
            }
        }
        val candidatePaths = listOf(
            "app/src/main/assets/$ASSET_FILE",
            "src/main/assets/$ASSET_FILE",
            ASSET_FILE
        )
        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                return file.inputStream()
            }
        }
        return InitialVocabulary::class.java.classLoader?.getResourceAsStream("assets/$ASSET_FILE")
    }

    fun parseTsvStream(inputStream: InputStream): List<WordEntity> {
        val result = ArrayList<WordEntity>(6100)
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
            var fallbackId = 1
            lines.forEach { rawLine ->
                if (rawLine.isBlank()) return@forEach
                val cols = rawLine.split('\t')
                if (cols.size < 12) return@forEach
                val id = cols[0].trim().toIntOrNull() ?: fallbackId
                val german = cols[1].trim()
                val uk = cols[5].trim()
                val en = cols[6].trim()
                if (german.isEmpty() || uk.isEmpty() || en.isEmpty()) return@forEach
                result.add(
                    WordEntity(
                        id = id,
                        german = german,
                        article = cols[2].trim(),
                        plural = cols[3].trim(),
                        transcription = cols[4].trim(),
                        translationUk = uk,
                        translationEn = en,
                        exampleDe = cols[7].trim(),
                        exampleUk = cols[8].trim(),
                        exampleEn = "",
                        categoryId = cols[9].trim(),
                        subtopicId = cols[10].trim(),
                        level = cols[11].trim().ifEmpty { "A2" }
                    )
                )
                fallbackId++
            }
        }
        return result
    }
}

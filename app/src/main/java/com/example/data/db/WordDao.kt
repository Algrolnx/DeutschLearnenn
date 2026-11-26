package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SubtopicProgressEntity
import com.example.data.model.WordEntity
import kotlinx.coroutines.flow.Flow

data class BoxCount(
    val box: Int,
    val cnt: Int
)

data class LevelStats(
    val level: String,
    val totalCount: Int,
    val masteredCount: Int
)

data class SubtopicLevelCount(
    val subtopicId: String,
    val level: String,
    val cnt: Int
)

@Dao
interface WordDao {

    @Query("SELECT * FROM words ORDER BY id ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words ORDER BY id ASC")
    suspend fun getAllWordsOnce(): List<WordEntity>

    @Query("SELECT * FROM words WHERE categoryId = :categoryId ORDER BY id ASC")
    fun getWordsByCategory(categoryId: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE subtopicId = :subtopicId ORDER BY id ASC")
    fun getWordsBySubtopic(subtopicId: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE subtopicId = :subtopicId AND UPPER(level) = UPPER(:level) ORDER BY id ASC LIMIT :limit")
    suspend fun getWordsBySubtopicAndLevelOnce(subtopicId: String, level: String, limit: Int = 15): List<WordEntity>

    @Query("SELECT * FROM words WHERE subtopicId = :subtopicId ORDER BY id ASC LIMIT :limit")
    suspend fun getWordsBySubtopicOnce(subtopicId: String, limit: Int = 15): List<WordEntity>

    @Query("SELECT DISTINCT UPPER(level) FROM words WHERE subtopicId = :subtopicId")
    suspend fun getAvailableLevelsForSubtopicOnce(subtopicId: String): List<String>

    @Query("SELECT * FROM words WHERE level = :level ORDER BY id ASC")
    fun getWordsByLevel(level: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE UPPER(level) = UPPER(:level) ORDER BY id ASC")
    suspend fun getWordsByLevelOnce(level: String): List<WordEntity>

    @Query("SELECT * FROM words WHERE nextReviewTimestamp <= :currentTimestamp OR nextReviewTimestamp = 0 ORDER BY nextReviewTimestamp ASC, box ASC LIMIT 60")
    fun getWordsDueForReview(currentTimestamp: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE nextReviewTimestamp > 0 AND nextReviewTimestamp <= :now ORDER BY nextReviewTimestamp ASC, box ASC LIMIT :limit")
    suspend fun getDueEnrolledWordsOnce(now: Long, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE nextReviewTimestamp > 0 ORDER BY nextReviewTimestamp ASC, box ASC LIMIT :limit")
    suspend fun getEarlyReviewEnrolledWordsOnce(limit: Int): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words WHERE nextReviewTimestamp > 0 AND nextReviewTimestamp <= :now")
    suspend fun countDueEnrolledWords(now: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE nextReviewTimestamp > 0")
    suspend fun countEnrolledWords(): Int

    @Query("SELECT * FROM words WHERE nextReviewTimestamp > 0")
    suspend fun getEnrolledWordsOnce(): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words WHERE nextReviewTimestamp > 0")
    fun observeEnrolledWordCount(): Flow<Int>

    @Query("SELECT MIN(nextReviewTimestamp) FROM words WHERE nextReviewTimestamp > :now")
    suspend fun getNextUpcomingReviewTimestamp(now: Long): Long?

    @Query("SELECT * FROM words WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavoriteWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Int): WordEntity?

    @Query("SELECT * FROM words WHERE LOWER(german) = LOWER(:german) LIMIT 1")
    suspend fun getWordByGermanOnce(german: String): WordEntity?

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Query("SELECT COUNT(*) FROM words")
    fun observeWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT box, COUNT(*) AS cnt FROM words GROUP BY box")
    fun observeBoxCounts(): Flow<List<BoxCount>>

    @Query("SELECT UPPER(level) AS level, COUNT(*) AS totalCount, SUM(CASE WHEN box >= 4 THEN 1 ELSE 0 END) AS masteredCount FROM words GROUP BY UPPER(level)")
    fun observeLevelStats(): Flow<List<LevelStats>>

    @Query("SELECT subtopicId, UPPER(level) AS level, COUNT(*) AS cnt FROM words GROUP BY subtopicId, UPPER(level)")
    fun observeSubtopicLevelCounts(): Flow<List<SubtopicLevelCount>>

    // Distractor queries (lightweight random sampling directly in SQLite)
    @Query("SELECT * FROM words WHERE subtopicId = :subtopicId AND LOWER(german) != LOWER(:excludeGerman) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getDistractorsSameSubtopic(subtopicId: String, excludeGerman: String, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE categoryId = :categoryId AND LOWER(german) != LOWER(:excludeGerman) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getDistractorsSameCategory(categoryId: String, excludeGerman: String, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE LOWER(german) != LOWER(:excludeGerman) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getDistractorsGeneral(excludeGerman: String, limit: Int): List<WordEntity>

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @androidx.room.Transaction
    suspend fun replaceAllWords(words: List<WordEntity>) {
        deleteAllWords()
        words.chunked(500).forEach { chunk ->
            insertAll(chunk)
        }
    }

    @Update
    suspend fun updateWord(word: WordEntity)

    @Query("UPDATE words SET box = :newBox, nextReviewTimestamp = :nextReview, reviewCount = reviewCount + 1, correctCount = correctCount + :isCorrectDelta WHERE id = :id")
    suspend fun updateReviewResult(id: Int, newBox: Int, nextReview: Long, isCorrectDelta: Int)

    @Query("UPDATE words SET nextReviewTimestamp = :nextReview WHERE id = :id AND nextReviewTimestamp = 0")
    suspend fun enrollWordForSrs(id: Int, nextReview: Long)

    @Query("UPDATE words SET reviewCount = reviewCount + 1, correctCount = correctCount + :isCorrectDelta WHERE id = :id")
    suspend fun incrementWordCountsOnly(id: Int, isCorrectDelta: Int)

    @Query("UPDATE words SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT * FROM words WHERE german LIKE '%' || :query || '%' OR translationUk LIKE '%' || :query || '%' OR translationEn LIKE '%' || :query || '%' ORDER BY id ASC LIMIT 40")
    fun searchWordsLimited(query: String): Flow<List<WordEntity>>

    // === Subtopic Progress ===
    @Query("SELECT * FROM subtopic_progress WHERE subtopicId = :subtopicId LIMIT 1")
    fun getSubtopicProgress(subtopicId: String): Flow<SubtopicProgressEntity?>

    @Query("SELECT * FROM subtopic_progress")
    fun getAllSubtopicProgress(): Flow<List<SubtopicProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubtopicProgress(progress: SubtopicProgressEntity)

    @Query("DELETE FROM subtopic_progress")
    suspend fun deleteAllSubtopicProgress()
}

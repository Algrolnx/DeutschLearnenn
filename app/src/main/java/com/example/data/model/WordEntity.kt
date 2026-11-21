package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["subtopicId"]),
        Index(value = ["level"]),
        Index(value = ["isFavorite"]),
        Index(value = ["german"])
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val german: String,
    val article: String = "",          // "der", "die", "das", or empty
    val plural: String = "",           // e.g. "die Äpfel"
    val transcription: String = "",    // IPA or phonetic transcription e.g. "[ˈapfl̩]"
    val translationUk: String,         // Ukrainian translation e.g. "яблуко"
    val translationEn: String,         // English translation e.g. "apple"
    val exampleDe: String = "",        // Example in German
    val exampleUk: String = "",        // Example in Ukrainian
    val exampleEn: String = "",        // Example in English
    val categoryId: String,            // Main topic id e.g. "food", "travel"
    val subtopicId: String = "",       // Subtopic id e.g. "food_fruits", "travel_transport"
    val level: String = "A1",          // "A1", "A2", "B1"
    val box: Int = 1,                  // Leitner box 1 to 5
    val nextReviewTimestamp: Long = 0L,// Milliseconds when next review is due
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val isFavorite: Boolean = false
) {
    /**
     * Display word with article if available, e.g. "der Apfel"
     */
    val displayWithArticle: String
        get() = if (article.isNotBlank()) "$article $german" else german

    /**
     * Gender type identifier for coloring
     */
    val genderType: GenderType
        get() = when (article.lowercase().trim()) {
            "der" -> GenderType.MASCULINE
            "die" -> GenderType.FEMININE
            "das" -> GenderType.NEUTER
            else -> GenderType.NONE
        }
}

enum class GenderType {
    MASCULINE, FEMININE, NEUTER, NONE
}

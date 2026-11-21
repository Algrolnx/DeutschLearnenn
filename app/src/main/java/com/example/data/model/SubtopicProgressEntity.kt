package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subtopic_progress")
data class SubtopicProgressEntity(
    @PrimaryKey
    val subtopicId: String,
    val unlockedStage: Int = 1,      // 1 to 6
    val completedStages: Int = 0,    // 0 to 6
    val stars: Int = 0,              // Total stars earned in this subtopic
    val stage1Score: Int = 0,        // Flashcards
    val stage2Score: Int = 0,        // Multiple Choice
    val stage3Score: Int = 0,        // Listening
    val stage4Score: Int = 0,        // Article Trainer
    val stage5Score: Int = 0,        // Fill in Blank
    val stage6Score: Int = 0         // Spelling
)

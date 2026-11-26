package com.example.data.model

data class ImportantSentence(
    val id: Int,
    val level: String,
    val situationId: String,
    val situationDe: String,
    val situationUk: String,
    val german: String,
    val ukrainian: String,
    val noteUk: String = ""
)

data class SentenceSituationGroup(
    val level: String,
    val situationId: String,
    val titleDe: String,
    val titleUk: String,
    val sentences: List<ImportantSentence>
)

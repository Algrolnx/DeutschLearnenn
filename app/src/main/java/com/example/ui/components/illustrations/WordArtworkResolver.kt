package com.example.ui.components.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.WordEntity

/**
 * Resolves every German vocabulary word across all 6,000 entries to a cohesive
 * offline glossy flat-vector illustration without false mid-word substring matches.
 */
object WordArtworkResolver {

    private val enWordRegex = Regex("[a-z]+")
    private val ukWordRegex = Regex("[а-яіїєґ]+")

    fun resolveWordArtworkKey(word: WordEntity): WordArtworkKey {
        val cleanDe = normalizeGerman(word.german)
        val cleanEn = word.translationEn.lowercase().trim()
        val cleanUk = word.translationUk.lowercase().trim()

        // 1. Exact German word match (handles concrete nouns, vegetables, station terms, colors, disambiguations)
        WordArtworkRules.exactDeMap[cleanDe]?.let { return it }

        // 2. Safe German compound suffix match (endsWith only, never arbitrary mid-word contains)
        for ((suffix, key) in WordArtworkRules.deCompoundSuffixRules) {
            if (cleanDe.length > suffix.length && cleanDe.endsWith(suffix)) {
                return key
            }
        }

        // 3. Multi-word English phrases
        for ((phrase, key) in WordArtworkRules.enPhraseRules) {
            if (cleanEn.contains(phrase)) {
                return key
            }
        }

        // 4. Whole-word English token matching
        for (token in enWordRegex.findAll(cleanEn).map { it.value }) {
            WordArtworkRules.enWholeWordRules[token]?.let { return it }
        }

        // 5. Whole-word Ukrainian token matching
        for (token in ukWordRegex.findAll(cleanUk).map { it.value }) {
            WordArtworkRules.ukWholeWordRules[token]?.let { return it }
        }

        // 6. Meaningful subtopic / category fallback
        return WordArtworkRules.subtopicFallbackMap[word.subtopicId]
            ?: WordArtworkRules.categoryFallbackMap[word.categoryId]
            ?: WordArtworkKey.BOOK
    }

    fun resolveSemanticTint(word: WordEntity): Color? {
        val cleanDe = normalizeGerman(word.german)
        val cleanEn = word.translationEn.lowercase().trim()
        return WordArtworkRules.resolveColorTint(cleanDe, cleanEn)
    }

    private fun normalizeGerman(raw: String): String =
        raw.lowercase().trim()
            .removePrefix("der ")
            .removePrefix("die ")
            .removePrefix("das ")
            .trim()
}

/**
 * Renders a 100% offline glossy shaded flat-vector illustration on a Compose Canvas.
 * Infinitely scalable across all screen sizes with zero network calls or API limits.
 */
@Composable
fun WordVectorArtwork(
    word: WordEntity,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val artworkKey = remember(word.id, word.german, word.translationEn, word.translationUk, word.subtopicId, word.categoryId) {
        WordArtworkResolver.resolveWordArtworkKey(word)
    }
    val semanticTint = remember(word.id, word.german, word.translationEn, artworkKey) {
        if (artworkKey == WordArtworkKey.COLOR_DROP) {
            WordArtworkResolver.resolveSemanticTint(word)
        } else {
            null
        }
    }

    Canvas(modifier = modifier) {
        if (size.minDimension <= 1f) return@Canvas
        drawVectorStage(accent = accentColor, isDark = isDark)
        val drawn = drawFoodProduceVector(artworkKey) ||
            drawFoodMealsAndDrinksVector(artworkKey) ||
            drawNatureAndAnimalVector(artworkKey) ||
            drawHomeAndTravelVector(artworkKey) ||
            drawTechLifestyleAndBodyVector(artworkKey)
        if (!drawn) {
            drawConceptSceneVector(artworkKey, semanticTint)
        }
    }
}

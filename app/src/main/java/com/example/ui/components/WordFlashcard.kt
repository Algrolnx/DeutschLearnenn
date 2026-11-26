package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntity
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.util.WordExampleGenerator

/**
 * Tactile Flashcard with Full-Bleed Real Word Photography as Background.
 * Combines full-card matched photography on the front with warm editorial typography,
 * protective gradient scrim for instant legibility, gender color accents, and zero emojis.
 */
@Composable
fun WordFlashcard(
    word: WordEntity,
    isFlipped: Boolean,
    currentIndex: Int,
    totalCount: Int,
    onFlip: () -> Unit,
    onSpeakWord: (slow: Boolean) -> Unit,
    onSpeakExample: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    onSpeakExampleText: ((String) -> Unit)? = null,
    onSpeakPlural: (() -> Unit)? = null,
    showNavigationArrows: Boolean = false,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    showSlowSpeedButton: Boolean = true,
    allowPhotoChange: Boolean = false,
    cardTestTag: String = "word_flashcard"
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "wordFlashcardFlip"
    )
    val isFrontVisible by remember { derivedStateOf { rotation <= 90f } }

    val extended = LocalAppExtendedColors.current
    val (genderAccent, genderBg) = when (word.article.lowercase().trim()) {
        "der" -> extended.genderDer to extended.genderDerBg
        "die" -> extended.genderDie to extended.genderDieBg
        "das" -> extended.genderDas to extended.genderDasBg
        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isFlipped) genderAccent.copy(alpha = 0.55f) else extended.cardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(onClick = onFlip)
            .testTag(cardTestTag)
    ) {
        if (isFrontVisible) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val contentWidth = (maxWidth - 32.dp).coerceAtLeast(180.dp)
                val photoBoxHeight = minOf(contentWidth * 0.75f, maxHeight * 0.46f).coerceAtLeast(140.dp)

                // Top Grammatical Gender Bookmark Accent Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(genderAccent)
                        .align(Alignment.TopCenter)
                )

                val foregroundInk = MaterialTheme.colorScheme.onSurface
                val secondaryInk = MaterialTheme.colorScheme.onSurfaceVariant
                val pillBg = extended.subtleFill

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Clean Top Header (never obscures photo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = pillBg
                            ) {
                                Text(
                                    text = "${(currentIndex + 1).toString().padStart(2, '0')} / ${totalCount.toString().padStart(2, '0')}",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = foregroundInk,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = pillBg
                            ) {
                                Text(
                                    text = word.level.uppercase(),
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (word.article.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = genderBg,
                                    border = BorderStroke(1.dp, genderAccent.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = word.article.lowercase(),
                                        fontFamily = MonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = genderAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(pillBg)
                                    .testTag("flashcard_favorite_btn")
                            ) {
                                Icon(
                                    imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (word.isFavorite) "Видалити з обраного" else "Додати в обране",
                                    tint = if (word.isFavorite) Color(0xFFF43F5E) else foregroundInk,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.35f))

                    // 2. Dedicated Framed 4:3 Close-Up Photo Stage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(photoBoxHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, extended.cardBorder, RoundedCornerShape(16.dp))
                    ) {
                        WordPhotoSurface(
                            word = word,
                            accentColor = genderAccent,
                            showFallbackIcon = true,
                            fitWholeSubject = false,
                            allowPhotoChange = allowPhotoChange,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.65f))

                    // 3. Bottom Word Hero + Audio Controls
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (word.article.isNotBlank()) {
                            Text(
                                text = word.article.lowercase(),
                                fontFamily = LoraFontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = genderAccent
                            )
                        }

                        Text(
                            text = word.german,
                            fontFamily = LoraFontFamily,
                            fontSize = if (word.german.length > 15) 26.sp else 33.sp,
                            fontWeight = FontWeight.Bold,
                            color = foregroundInk,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 38.sp
                        )

                        val pluralTrimmed = word.plural.trim()
                        val isNounPlural = word.article.lowercase().trim() in listOf("der", "die", "das") &&
                            pluralTrimmed.startsWith("die ", ignoreCase = true)

                        if (word.transcription.isNotBlank() || pluralTrimmed.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (word.transcription.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = pillBg
                                    ) {
                                        Text(
                                            text = word.transcription,
                                            fontFamily = MonoFontFamily,
                                            fontSize = 12.sp,
                                            color = secondaryInk,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (isNounPlural) {
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = extended.warningYellowBg,
                                        border = BorderStroke(1.dp, extended.warningYellow.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable(enabled = onSpeakPlural != null) { onSpeakPlural?.invoke() }
                                            .testTag("flashcard_plural_badge")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "Множина: ",
                                                fontFamily = ManropeFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = extended.warningYellow
                                            )
                                            Text(
                                                text = pluralTrimmed,
                                                fontFamily = LoraFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = foregroundInk
                                            )
                                            if (onSpeakPlural != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Вимовити множину",
                                                    tint = extended.warningYellow,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (pluralTrimmed.isNotBlank() && pluralTrimmed != "-") {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = pillBg
                                    ) {
                                        Text(
                                            text = "Форми: $pluralTrimmed",
                                            fontFamily = ManropeFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = secondaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (showNavigationArrows) Arrangement.SpaceBetween else Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showNavigationArrows) {
                                IconButton(
                                    onClick = onPrev,
                                    enabled = currentIndex > 0,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(pillBg)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Попереднє",
                                        tint = foregroundInk.copy(alpha = if (currentIndex > 0) 1f else 0.35f)
                                    )
                                }
                            }

                            SpeakerButton(
                                onSpeak = onSpeakWord,
                                size = 50.dp,
                                tint = Color.White,
                                backgroundColor = genderAccent,
                                showSlowButton = showSlowSpeedButton
                            )

                            if (showNavigationArrows) {
                                IconButton(
                                    onClick = onNext,
                                    enabled = currentIndex < totalCount - 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(pillBg)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Наступне",
                                        tint = foregroundInk.copy(alpha = if (currentIndex < totalCount - 1) 1f else 0.35f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FlipCameraAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = secondaryInk
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Торкніться картки для перекладу",
                                fontFamily = ManropeFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryInk
                            )
                        }
                    }
                }
            }
        } else {
            // === BACK SIDE: Clean Editorial Dictionary Card + Real Mini Photo ===
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(genderAccent)
                        .align(Alignment.TopCenter)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Rounded Mini Photo + Word + Audio + Favorite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            WordPhotoSurface(
                                word = word,
                                accentColor = genderAccent,
                                showFallbackIcon = true,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, extended.cardBorder, RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (word.article.isNotBlank()) {
                                    Text(
                                        text = word.article.lowercase(),
                                        fontFamily = MonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = genderAccent
                                    )
                                }
                                Text(
                                    text = word.german,
                                    fontFamily = LoraFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val backPlural = word.plural.trim()
                                if (word.article.lowercase().trim() in listOf("der", "die", "das") &&
                                    backPlural.startsWith("die ", ignoreCase = true)
                                ) {
                                    Text(
                                        text = "Множина: $backPlural",
                                        fontFamily = ManropeFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.warningYellow,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable(enabled = onSpeakPlural != null) {
                                            onSpeakPlural?.invoke()
                                        }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onSpeakWord(false) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(extended.subtleFill)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Вимовити слово",
                                    tint = genderAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(extended.subtleFill)
                            ) {
                                Icon(
                                    imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Обране",
                                    tint = if (word.isFavorite) extended.genderDie else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Center: Crisp Translation Block + 3 Contextual Example Sentences (scrollable)
                    val examples = remember(word.id, word.german, word.exampleDe, word.exampleUk) {
                        WordExampleGenerator.buildExamplesForWord(word)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = genderBg,
                            border = BorderStroke(1.dp, genderAccent.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "UA · УКРАЇНСЬКОЮ",
                                fontFamily = MonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = genderAccent,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = word.translationUk,
                            fontFamily = LoraFontFamily,
                            fontSize = if (word.translationUk.length > 18) 23.sp else 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(64.dp),
                            color = extended.cardBorder
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = extended.subtleFill
                            ) {
                                Text(
                                    text = "EN",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = word.translationEn,
                                fontFamily = ManropeFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (examples.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Приклади вживання (${examples.size})",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Натисніть, щоб послухати",
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("flashcard_examples_list")
                            ) {
                                examples.forEachIndexed { idx, ex ->
                                    val highlighted = remember(ex.de, word.german, genderAccent) {
                                        highlightWordInSentence(ex.de, word.german, genderAccent)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = extended.subtleFill,
                                        border = BorderStroke(1.dp, extended.cardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (onSpeakExampleText != null) {
                                                    onSpeakExampleText(ex.de)
                                                } else if (idx == 0) {
                                                    onSpeakExample()
                                                } else {
                                                    onSpeakExample()
                                                }
                                            }
                                            .testTag("flashcard_example_item_$idx")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = highlighted,
                                                    fontFamily = LoraFontFamily,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 19.sp
                                                )
                                                if (ex.uk.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = ex.uk,
                                                        fontFamily = ManropeFontFamily,
                                                        fontSize = 11.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (onSpeakExampleText != null) {
                                                        onSpeakExampleText(ex.de)
                                                    } else {
                                                        onSpeakExample()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("flashcard_example_speak_$idx")
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(genderBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Прослухати приклад ${idx + 1}",
                                                        tint = genderAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Prompt / Arrows
                    if (showNavigationArrows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPrev,
                                enabled = currentIndex > 0,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(extended.subtleFill)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Попереднє",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (currentIndex > 0) 1f else 0.3f)
                                )
                            }

                            Text(
                                text = "${currentIndex + 1} / $totalCount",
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            IconButton(
                                onClick = onNext,
                                enabled = currentIndex < totalCount - 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(extended.subtleFill)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Наступне",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (currentIndex < totalCount - 1) 1f else 0.3f)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Чи запам'ятали ви це слово?",
                            fontFamily = ManropeFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun highlightWordInSentence(
    sentence: String,
    targetWord: String,
    accentColor: Color
): AnnotatedString = buildAnnotatedString {
    val cleanTarget = targetWord.trim()
    if (cleanTarget.isEmpty()) {
        append(sentence)
        return@buildAnnotatedString
    }
    val pattern = Regex(
        "(?<![A-Za-zÄÖÜäöüß])${Regex.escape(cleanTarget)}(?:e|en|er|es|em|n|s)?(?![A-Za-zÄÖÜäöüß])",
        RegexOption.IGNORE_CASE
    )
    val match = pattern.find(sentence)
    if (match == null) {
        append(sentence)
        return@buildAnnotatedString
    }
    append(sentence.substring(0, match.range.first))
    withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
        append(sentence.substring(match.range.first, match.range.last + 1))
    }
    append(sentence.substring(match.range.last + 1))
}

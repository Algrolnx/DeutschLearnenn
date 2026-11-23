package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.WordEntity
import com.example.data.repository.CachedStockPhoto
import com.example.data.repository.StockPhotoRepository
import com.example.ui.components.illustrations.WordVectorArtwork
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays a clean 4:3 Pixabay photograph with local caching and zero vector overdraw once loaded.
 * - Vector artwork is ONLY rendered as an offline/unavailable fallback (never underneath a loaded photo).
 * - Photo cycling ("Інше") is strictly gated behind `allowPhotoChange`, which is only enabled during
 *   Stage 1's initial pass before completion so learners always retain the same visual anchor afterwards.
 * - Uses sized Coil requests matching `StockPhotoRepository.prefetchWord` so prefetched cards hit RAM
 *   cache on frame 1 without spinner flicker or crossfade lag.
 */
@Composable
fun WordPhotoSurface(
    word: WordEntity,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showFallbackIcon: Boolean = true,
    fitWholeSubject: Boolean = false,
    allowPhotoChange: Boolean = false,
    isThumbnail: Boolean = false,
    onPhotoReadyChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val extended = LocalAppExtendedColors.current
    val repository = remember(context) { StockPhotoRepository.getInstance(context) }
    val hasApiKey = remember(repository) { repository.hasConfiguredApiKey() }
    val scope = rememberCoroutineScope()

    var resolvedPhoto by remember(word.id, word.german, word.subtopicId) {
        mutableStateOf(repository.getCachedSync(word))
    }
    val initialMemCached = remember(word.id, word.german, word.subtopicId, resolvedPhoto?.imageUrl, isThumbnail) {
        val url = resolvedPhoto?.imageUrl ?: return@remember false
        val memKey = if (isThumbnail) "$url#thumb" else url
        repository.isInMemoryCache(url, memKey)
    }
    var isFetchingUrl by remember(word.id, word.german, word.subtopicId) {
        mutableStateOf(resolvedPhoto == null && hasApiKey)
    }
    var isImageLoaded by remember(word.id, word.german, word.subtopicId) {
        mutableStateOf(initialMemCached)
    }
    var showLoadingSpinner by remember(word.id, word.german, word.subtopicId) {
        mutableStateOf(false)
    }
    var photoUnavailable by remember(word.id, word.german, word.subtopicId) {
        mutableStateOf(!hasApiKey && resolvedPhoto == null)
    }
    var needsRefreshAfterError by remember(word.id, word.german, word.subtopicId) {
        mutableStateOf(false)
    }
    var hasRetriedExpiredUrl by remember(word.id, word.german, word.subtopicId) {
        mutableStateOf(false)
    }

    LaunchedEffect(word.id, word.german, word.subtopicId) {
        if (resolvedPhoto == null && hasApiKey) {
            isFetchingUrl = true
            val fetched = repository.resolvePhoto(word, highPriority = !isThumbnail)
            resolvedPhoto = fetched
            photoUnavailable = fetched == null
            isFetchingUrl = false
        }
        onPhotoReadyChange(true)
    }

    // Avoid distracting 1-frame spinner flicker for fast RAM/disk cached images
    val isPendingLoad = isFetchingUrl || (resolvedPhoto != null && !isImageLoaded && !photoUnavailable)
    LaunchedEffect(isPendingLoad, word.id, resolvedPhoto?.imageUrl) {
        if (isPendingLoad) {
            delay(140L)
            showLoadingSpinner = true
        } else {
            showLoadingSpinner = false
        }
    }

    LaunchedEffect(word.id, word.german, word.subtopicId, needsRefreshAfterError) {
        if (needsRefreshAfterError && !hasRetriedExpiredUrl && hasApiKey) {
            hasRetriedExpiredUrl = true
            isFetchingUrl = true
            val refreshed = repository.invalidateAndRefreshPhoto(word)
            resolvedPhoto = refreshed
            photoUnavailable = refreshed == null
            isFetchingUrl = false
        }
        needsRefreshAfterError = false
    }

    Box(
        modifier = modifier
            .background(extended.subtleFill)
            .semantics {
                contentDescription = "${word.german} — ${word.translationUk}"
            },
        contentAlignment = Alignment.Center
    ) {
        // Render offline vector artwork ONLY when no photo is available (offline / no key / failed load)
        if (photoUnavailable && !isImageLoaded) {
            WordVectorArtwork(
                word = word,
                accentColor = accentColor,
                isDark = extended.isDark,
                modifier = Modifier.fillMaxSize()
            )
        }

        val activePhoto: CachedStockPhoto? = resolvedPhoto
        if (activePhoto != null) {
            val imageRequest = remember(activePhoto.imageUrl, isThumbnail) {
                val memKey = if (isThumbnail) "${activePhoto.imageUrl}#thumb" else activePhoto.imageUrl
                val alreadyInRam = repository.isInMemoryCache(activePhoto.imageUrl, memKey)
                ImageRequest.Builder(context)
                    .data(activePhoto.imageUrl)
                    .apply {
                        if (isThumbnail) {
                            size(StockPhotoRepository.THUMB_DECODE_SIZE, StockPhotoRepository.THUMB_DECODE_SIZE)
                        } else {
                            size(StockPhotoRepository.CARD_DECODE_WIDTH, StockPhotoRepository.CARD_DECODE_HEIGHT)
                        }
                    }
                    .memoryCacheKey(memKey)
                    .crossfade(if (alreadyInRam) 0 else 100)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                imageLoader = repository.imageLoader,
                contentDescription = "${word.german} — ${word.translationUk}",
                contentScale = if (fitWholeSubject) ContentScale.Fit else ContentScale.Crop,
                onSuccess = {
                    isImageLoaded = true
                    photoUnavailable = false
                },
                onError = {
                    isImageLoaded = false
                    if (!hasRetriedExpiredUrl && hasApiKey) {
                        needsRefreshAfterError = true
                    } else {
                        photoUnavailable = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Photo switching is strictly allowed ONLY during the initial Stage 1 pass
            if (allowPhotoChange && isImageLoaded && hasApiKey) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(width = 68.dp, height = 48.dp)
                        .clickable(enabled = !isFetchingUrl) {
                            scope.launch {
                                isFetchingUrl = true
                                val next = repository.cycleNextPhoto(word)
                                if (next != null) {
                                    resolvedPhoto = next
                                }
                                isFetchingUrl = false
                            }
                        }
                        .testTag("cycle_word_photo_button")
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.56f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Інше фото",
                                tint = Color.White.copy(alpha = 0.94f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Інше",
                                fontFamily = MonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    }
                }
            }
        }

        if (showLoadingSpinner && isPendingLoad) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Vocabulary card visual banner for quizzes and article trainer (photo is always locked here).
 */
@Composable
fun WordIllustrationView(
    word: WordEntity,
    modifier: Modifier = Modifier,
    height: Dp = 164.dp,
    showBadges: Boolean = true
) {
    val extended = LocalAppExtendedColors.current
    val genderColor = getGenderColor(word.article)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, extended.cardBorder), RoundedCornerShape(16.dp))
    ) {
        WordPhotoSurface(
            word = word,
            accentColor = genderColor,
            showFallbackIcon = true,
            fitWholeSubject = false,
            allowPhotoChange = false,
            modifier = Modifier.fillMaxSize()
        )

        if (showBadges) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.58f)
                ) {
                    Text(
                        text = word.level.uppercase(),
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                if (word.article.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = genderColor
                    ) {
                        Text(
                            text = word.article.lowercase(),
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

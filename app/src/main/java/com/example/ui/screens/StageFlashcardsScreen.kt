package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TopicCatalog
import com.example.data.repository.StockPhotoRepository
import com.example.ui.LearningViewModel
import com.example.ui.components.WordFlashcard
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageFlashcardsScreen(
    subtopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extended = LocalAppExtendedColors.current
    val subtopic = TopicCatalog.findSubtopicById(subtopicId)
    val words by viewModel.stage1Words.collectAsStateWithLifecycle()
    val currentIndex by viewModel.stage1Index.collectAsStateWithLifecycle()
    val isFlipped by viewModel.stage1Flipped.collectAsStateWithLifecycle()
    val isFinished by viewModel.stage1Finished.collectAsStateWithLifecycle()
    val allowPhotoChange by viewModel.stage1AllowPhotoChange.collectAsStateWithLifecycle()
    val unlockedSrs by viewModel.lastCompletedUnlockedSrs.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    val currentWord = words.getOrNull(currentIndex)
    val progress = if (words.isNotEmpty()) (currentIndex + 1).toFloat() / words.size else 0f
    val isSessionActive = !isFinished && currentWord != null

    val requestExit: () -> Unit = {
        if (isSessionActive) {
            showExitDialog = true
        } else {
            viewModel.navigateBack()
        }
    }

    BackHandler {
        requestExit()
    }

    if (showExitDialog) {
        StageExitConfirmationDialog(
            onConfirmExit = {
                showExitDialog = false
                viewModel.navigateBack()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    LaunchedEffect(currentIndex, currentWord?.id, isFinished) {
        if (currentWord != null && !isFinished) {
            viewModel.speakIfAuto(currentWord.displayWithArticle)
            val repo = StockPhotoRepository.getInstance(context)
            repo.prefetchWordsAsync(
                listOfNotNull(
                    words.getOrNull(currentIndex + 1),
                    words.getOrNull(currentIndex + 2),
                    words.getOrNull(currentIndex + 3)
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Етап 1: Lernkarten · Картки",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = subtopic?.titleDe ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = requestExit,
                        modifier = Modifier.size(48.dp).testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (words.isNotEmpty() && !isFinished) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "${currentIndex + 1} / ${words.size}",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = extended.subtleFill
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isFinished || currentWord == null) {
                    val nextEnabled = viewModel.getNextEnabledStage(1)
                    Spacer(modifier = Modifier.weight(0.3f))
                    StageCompletedSuccessCard(
                        stageNumber = 1,
                        nextStageNumber = nextEnabled,
                        title = "Етап 1 пройдено!",
                        subtitle = when {
                            unlockedSrs -> "Усі 8 етапів пройдено на 100%! Через 6 годин слова з'являться у вкладці «Повторення»."
                            nextEnabled != null -> "Ви ознайомилися з усіма словами на 100%. Далі відкрито Етап $nextEnabled!"
                            else -> "Етап пройдено на 100%! Щоб слова потрапили в «Повторення», пройдіть усі 8 етапів підтеми."
                        },
                        onNextStage = { viewModel.startNextEnabledStage(subtopicId, 1) },
                        onBackToHub = { viewModel.navigateBack() }
                    )
                    Spacer(modifier = Modifier.weight(0.7f))
                } else {
                    WordFlashcard(
                        word = currentWord,
                        isFlipped = isFlipped,
                        currentIndex = currentIndex,
                        totalCount = words.size,
                        onFlip = { viewModel.flipStage1Card() },
                        onSpeakWord = { slow -> viewModel.speak(currentWord.displayWithArticle, slow) },
                        onSpeakPlural = {
                            val pl = currentWord.plural.trim()
                            if (pl.isNotBlank() && pl != "-") {
                                viewModel.speak(pl)
                            }
                        },
                        onSpeakExample = {
                            if (currentWord.exampleDe.isNotBlank()) {
                                viewModel.speak(currentWord.exampleDe)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(currentWord) },
                        showNavigationArrows = false,
                        showSlowSpeedButton = true,
                        allowPhotoChange = allowPhotoChange,
                        cardTestTag = "stage1_card",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isFlipped) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.answerStage1ShowAgain(subtopicId) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, StreakAmber),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = StreakAmber
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("show_again_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Показати ще раз", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.answerStage1Remembered(subtopicId) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("remembered_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Запам'ятав", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.flipStage1Card() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("flip_card_button")
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Перевернути картку", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StageCompletedSuccessCard(
    stageNumber: Int,
    title: String,
    subtitle: String,
    onNextStage: () -> Unit,
    onBackToHub: () -> Unit,
    nextStageNumber: Int? = if (stageNumber < 8) stageNumber + 1 else null,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontFamily = LoraFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onNextStage,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    if (nextStageNumber != null) "Перейти до Етапу $nextStageNumber" else "Завершити тему",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBackToHub,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Повернутися до теми", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun StageExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Вийти з етапу?",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "Поточний раунд буде перервано, і етап доведеться пройти спочатку. Вже оцінені слова залишаться збереженими у вашому прогресі.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_exit_stage_button")
            ) {
                Text("Вийти", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_exit_stage_button")
            ) {
                Text("Продовжити", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun RetryMistakeBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StreakAmber.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, StreakAmber.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = StreakAmber,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Повтор помилки",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StreakAmber
            )
        }
    }
}

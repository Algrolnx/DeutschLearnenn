package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.components.GenderBadge
import com.example.ui.components.SpeakerButton
import com.example.ui.components.WordIllustrationView
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageMultipleChoiceScreen(
    subtopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val questions by viewModel.stage3Questions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.stage3Index.collectAsStateWithLifecycle()
    val selectedAnswer by viewModel.stage3SelectedAnswer.collectAsStateWithLifecycle()
    val score by viewModel.stage3Score.collectAsStateWithLifecycle()
    val isFinished by viewModel.stage3Finished.collectAsStateWithLifecycle()
    val unlockedSrs by viewModel.lastCompletedUnlockedSrs.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    val currentQ = questions.getOrNull(currentIndex)
    val progress = if (questions.isNotEmpty()) (currentIndex.toFloat() / questions.size) else 0f
    val isSessionActive = !isFinished && currentQ != null

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Етап 3: Wortwahl · Тест",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (currentQ != null) "Питання ${currentIndex + 1} з ${questions.size}" else "Завершено",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = requestExit, modifier = Modifier.size(48.dp).testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Бали: $score",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
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
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
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

                Spacer(modifier = Modifier.height(16.dp))

                if (isFinished || currentQ == null) {
                    val nextEnabled = viewModel.getNextEnabledStage(3)
                    StageCompletedSuccessCard(
                        stageNumber = 3,
                        nextStageNumber = nextEnabled,
                        title = "Етап 3 пройдено!",
                        subtitle = when {
                            unlockedSrs -> "Усі обрані етапи пройдено на 100%! Через 6 годин слова з'являться у вкладці «Повторення»."
                            nextEnabled != null -> "Усі ${questions.count { !it.isRetry }} слів засвоєно на 100%! Далі відкрито Етап $nextEnabled."
                            else -> "Етап пройдено на 100%! Щоб слова потрапили в «Повторення», пройдіть усі обрані етапи підтеми."
                        },
                        onNextStage = { viewModel.startNextEnabledStage(subtopicId, 3) },
                        onBackToHub = { viewModel.navigateBack() }
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, extended.cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (currentQ.isRetry) {
                                RetryMistakeBadge()
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            WordIllustrationView(
                                word = currentQ.word,
                                height = 152.dp,
                                showBadges = false
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Оберіть правильний переклад:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (currentQ.word.article.isNotBlank()) {
                                    GenderBadge(article = currentQ.word.article)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = currentQ.word.german,
                                    fontFamily = LoraFontFamily,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (currentQ.word.transcription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentQ.word.transcription,
                                    fontFamily = MonoFontFamily,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            SpeakerButton(
                                onSpeak = { slow -> viewModel.speak(currentQ.word.displayWithArticle, slow) },
                                size = 46.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val optionLabels = listOf("A", "B", "C", "D")
                        currentQ.options.forEachIndexed { index, option ->
                            val isSelected = selectedAnswer == index
                            val isCorrect = index == currentQ.correctIndex

                            val (bgColor, borderColor, textColor) = when {
                                selectedAnswer == null -> Triple(
                                    MaterialTheme.colorScheme.surface,
                                    extended.cardBorder,
                                    MaterialTheme.colorScheme.onSurface
                                )
                                isCorrect -> Triple(SuccessGreen.copy(alpha = 0.14f), SuccessGreen, SuccessGreen)
                                isSelected -> Triple(ErrorRed.copy(alpha = 0.14f), ErrorRed, ErrorRed)
                                else -> Triple(
                                    MaterialTheme.colorScheme.surface,
                                    extended.cardBorder.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bgColor)
                                    .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                    .clickable(enabled = selectedAnswer == null) {
                                        viewModel.answerStage3(subtopicId, index)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        selectedAnswer != null && isCorrect -> SuccessGreen
                                                        selectedAnswer != null && isSelected -> ErrorRed
                                                        else -> extended.subtleFill
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = optionLabels.getOrElse(index) { "${index + 1}" },
                                                fontFamily = MonoFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedAnswer != null && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = option,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected || (selectedAnswer != null && isCorrect)) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor
                                        )
                                    }

                                    if (selectedAnswer != null) {
                                        if (isCorrect) Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                        else if (isSelected) Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }

                    if (selectedAnswer != null) {
                        if (selectedAnswer != currentQ.correctIndex) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Помилка · слово повернеться в кінці етапу для повторення",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.nextStage3(subtopicId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (currentIndex + 1 < questions.size) "Далі · Weiter" else "Результати",
                                fontFamily = LoraFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

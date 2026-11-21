package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.components.GenderBadge
import com.example.ui.components.SpeakerButton
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StageSpellingScreen(
    subtopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val words by viewModel.stage6Words.collectAsStateWithLifecycle()
    val currentIndex by viewModel.stage6Index.collectAsStateWithLifecycle()
    val letterPool by viewModel.stage6LetterPool.collectAsStateWithLifecycle()
    val currentInput by viewModel.stage6Input.collectAsStateWithLifecycle()
    val status by viewModel.stage6Status.collectAsStateWithLifecycle()
    val isFinished by viewModel.stage6Finished.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    val currentWord = words.getOrNull(currentIndex)
    val progress = if (words.isNotEmpty()) (currentIndex.toFloat() / words.size) else 0f
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Етап 6: Правопис · Schreiben",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (currentWord != null) "Слово ${currentIndex + 1} з ${words.size}" else "Завершено",
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
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

                Spacer(modifier = Modifier.height(10.dp))

                if (isFinished || currentWord == null) {
                    StageCompletedSuccessCard(
                        stageNumber = 6,
                        title = "Усі 6 етапів пройдено!",
                        subtitle = "Вітаємо! Ви повністю засвоїли всі слова цієї підтеми та перевели їх у статус закріплених.",
                        onNextStage = { viewModel.navigateBack() },
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
                                .padding(18.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (currentWord.article.isNotBlank()) {
                                GenderBadge(article = currentWord.article)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                text = currentWord.translationUk,
                                fontFamily = LoraFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentWord.translationEn,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (currentWord.transcription.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentWord.transcription,
                                    fontFamily = MonoFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            SpeakerButton(
                                onSpeak = { slow -> viewModel.speak(currentWord.displayWithArticle, slow) },
                                size = 46.dp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val boxBorder = when (status) {
                            true -> SuccessGreen
                            false -> ErrorRed
                            null -> extended.cardBorder
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when (status) {
                                        true -> SuccessGreen.copy(alpha = 0.12f)
                                        false -> ErrorRed.copy(alpha = 0.12f)
                                        null -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(2.dp, boxBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentInput.isEmpty()) "Натискайте літери..." else currentInput,
                                fontFamily = MonoFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = when (status) {
                                    true -> SuccessGreen
                                    false -> ErrorRed
                                    null -> if (currentInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        if (status == true) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Richtig! Чудово!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else if (status == false) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Підказка: ${currentWord.german}", color = ErrorRed, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            listOf('ä', 'ö', 'ü', 'ß', 'Ä', 'Ö', 'Ü').forEach { umlaut ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { viewModel.addStage6Letter(umlaut) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = umlaut.toString(),
                                        fontFamily = LoraFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            letterPool.forEach { char ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, extended.cardBorder, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.addStage6Letter(char) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char.toString(),
                                        fontFamily = LoraFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.removeStage6Letter() },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Видалити символ", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = { viewModel.clearStage6() },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Очистити", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }

                            if (status == true) {
                                Button(
                                    onClick = { viewModel.nextStage6(subtopicId) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (currentIndex + 1 < words.size) "Далі · Weiter" else "Завершити",
                                        fontFamily = LoraFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

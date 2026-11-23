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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.Stage6InputMode
import com.example.ui.Stage6WordStatus
import com.example.ui.components.GenderBadge
import com.example.ui.components.SpeakerButton
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StageMissingLettersScreen(
    subtopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val questions by viewModel.stage8Questions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.stage8Index.collectAsStateWithLifecycle()
    val currentInput by viewModel.stage8Input.collectAsStateWithLifecycle()
    val status by viewModel.stage8Status.collectAsStateWithLifecycle()
    val attemptsLeft by viewModel.stage8AttemptsLeft.collectAsStateWithLifecycle()
    val isFinished by viewModel.stage8Finished.collectAsStateWithLifecycle()
    val inputMode by viewModel.stage6InputMode.collectAsStateWithLifecycle()
    val unlockedSrs by viewModel.lastCompletedUnlockedSrs.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    val currentQ = questions.getOrNull(currentIndex)
    val progress = if (questions.isNotEmpty()) (currentIndex.toFloat() / questions.size) else 1f
    val isSessionActive = !isFinished && currentQ != null
    val isWordResolved = status.isResolved

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
                            text = "Етап 8: Пропущені літери · Lückenwort",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (currentQ != null) "Слово ${currentIndex + 1} з ${questions.size}" else "Завершено",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = requestExit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isSessionActive) {
                        val attemptsColor = when (attemptsLeft) {
                            3 -> MaterialTheme.colorScheme.primary
                            2 -> StreakAmber
                            else -> ErrorRed
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(attemptsColor.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("stage8_attempts_badge")
                        ) {
                            Text(
                                text = "Спроби: $attemptsLeft / ${LearningViewModel.MAX_STAGE6_ATTEMPTS}",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = attemptsColor,
                                fontSize = 12.sp
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
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .imePadding()
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

                Spacer(modifier = Modifier.height(12.dp))

                if (isFinished || currentQ == null) {
                    StageCompletedSuccessCard(
                        stageNumber = 8,
                        nextStageNumber = null,
                        title = if (unlockedSrs) "Підтему завершено на 100%!" else "Етап 8 пройдено!",
                        subtitle = if (unlockedSrs) {
                            "Вітаємо! Усі 8 етапів пройдено на 100% — слова додано в цикл запам'ятовування (перше повторення через 6 годин у вкладці «Повторення»)."
                        } else {
                            "Етап 8 пройдено на 100%! Щоб слова потрапили у вкладку «Повторення», пройдіть усі 8 етапів цієї підтеми."
                        },
                        onNextStage = { viewModel.navigateBack() },
                        onBackToHub = { viewModel.navigateBack() }
                    )
                } else {
                    val currentWord = currentQ.word
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (currentWord.article.isNotBlank()) {
                                    GenderBadge(article = currentWord.article)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Text(
                                    text = currentWord.translationUk,
                                    fontFamily = LoraFontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = currentWord.translationEn,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
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

                        // Visual word slots with missing letters highlighted
                        MissingLettersWordDisplay(
                            word = currentWord.german.trim(),
                            blankIndices = currentQ.blankIndices,
                            userInput = currentInput,
                            status = status
                        )

                        if (inputMode == Stage6InputMode.KEYBOARD) {
                            val boxBorder = when (status) {
                                Stage6WordStatus.CORRECT -> SuccessGreen
                                Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                                Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                Stage6WordStatus.TYPING -> extended.cardBorder
                            }

                            OutlinedTextField(
                                value = currentInput,
                                onValueChange = { viewModel.updateStage8TypedText(it) },
                                enabled = !isWordResolved,
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        text = "Введіть ${currentQ.blankIndices.size} пропущені літери або все слово...",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 13.sp
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = MonoFontFamily,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (status) {
                                        Stage6WordStatus.CORRECT -> SuccessGreen
                                        Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                                        Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                        Stage6WordStatus.TYPING -> MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done,
                                    autoCorrectEnabled = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { viewModel.submitStage8TypedAnswer() }
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (status == Stage6WordStatus.TYPING) MaterialTheme.colorScheme.primary else boxBorder,
                                    unfocusedBorderColor = boxBorder,
                                    disabledBorderColor = boxBorder,
                                    disabledTextColor = when (status) {
                                        Stage6WordStatus.CORRECT -> SuccessGreen
                                        Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                                        Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = when (status) {
                                        Stage6WordStatus.WRONG_TRY -> ErrorRed.copy(alpha = 0.08f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    disabledContainerColor = when (status) {
                                        Stage6WordStatus.CORRECT -> SuccessGreen.copy(alpha = 0.12f)
                                        Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellowBg
                                        Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed.copy(alpha = 0.12f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stage8_keyboard_input")
                            )
                        }

                        when (status) {
                            Stage6WordStatus.CORRECT -> {
                                Text(
                                    text = "Richtig! ${currentWord.displayWithArticle}",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Stage6WordStatus.CORRECT_UMLAUT_WARNING -> {
                                UmlautCorrectedFeedbackCard(word = currentWord)
                            }
                            Stage6WordStatus.WRONG_TRY -> {
                                Text(
                                    text = if (currentInput.isNotEmpty()) {
                                        "Невірно · вірні літери збережено, продовжуйте далі (спроб: $attemptsLeft)"
                                    } else {
                                        "Невірно · спробуйте ще або натисніть «Підказка» (спроб: $attemptsLeft)"
                                    },
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Stage6WordStatus.OUT_OF_ATTEMPTS -> {
                                Text(
                                    text = "Спроби вичерпано · Правильно: ${currentWord.displayWithArticle}. Слово повернеться в кінці етапу.",
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Stage6WordStatus.TYPING -> Unit
                        }

                        if (!isWordResolved) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    listOf('ä', 'ö', 'ü', 'ß').forEach { umlaut ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .clickable { viewModel.insertStage8Umlaut(umlaut) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = umlaut.toString(),
                                                fontFamily = LoraFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 17.sp
                                            )
                                        }
                                    }
                                }

                                if (inputMode == Stage6InputMode.LETTER_TILES) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        currentQ.letterPool.forEach { char ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, extended.cardBorder, RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.addStage8Letter(char) },
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
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isWordResolved) {
                        Button(
                            onClick = { viewModel.nextStage8MissingLetters(subtopicId) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("stage8_next_button")
                        ) {
                            Text(
                                text = if (currentIndex + 1 < questions.size) "Далі · Weiter" else "Завершити",
                                fontFamily = LoraFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else if (inputMode == Stage6InputMode.KEYBOARD) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.submitStage8TypedAnswer() },
                                enabled = currentInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(48.dp)
                                    .testTag("stage8_check_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Перевірити",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { viewModel.useStage8Hint() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StreakAmber.copy(alpha = 0.20f),
                                    contentColor = StreakAmber
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stage8_hint_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Підказка однієї літери",
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Підказка",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.removeStage8Letter() },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(48.dp)
                                    .testTag("stage8_backspace_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Видалити символ",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { viewModel.clearStage8() },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stage8_clear_button")
                            ) {
                                Text(
                                    text = "Очистити",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { viewModel.useStage8Hint() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StreakAmber.copy(alpha = 0.20f),
                                    contentColor = StreakAmber
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(48.dp)
                                    .testTag("stage8_hint_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Підказка однієї літери",
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Підказка",
                                    fontSize = 12.sp,
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

/**
 * Reusable visual display of a German word with interactive blank slots for Stage 8 and Mixed Review.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MissingLettersWordDisplay(
    word: String,
    blankIndices: List<Int>,
    userInput: String,
    status: Stage6WordStatus,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val blankRankByIndex = remember(blankIndices) {
        blankIndices.withIndex().associate { (rank, charIndex) -> charIndex to rank }
    }
    val activeRank = userInput.length.coerceAtMost(blankIndices.size)

    val containerBg = when (status) {
        Stage6WordStatus.CORRECT -> SuccessGreen.copy(alpha = 0.10f)
        Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellowBg
        Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed.copy(alpha = 0.10f)
        Stage6WordStatus.TYPING -> MaterialTheme.colorScheme.surface
    }
    val containerBorder = when (status) {
        Stage6WordStatus.CORRECT -> SuccessGreen
        Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
        Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
        Stage6WordStatus.TYPING -> extended.cardBorder
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, containerBorder, RoundedCornerShape(16.dp))
            .testTag("missing_letters_display")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Вставте пропущені літери (${blankIndices.size}):",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                word.forEachIndexed { idx, ch ->
                    val rank = blankRankByIndex[idx]
                    if (ch == ' ') {
                        Spacer(modifier = Modifier.width(12.dp))
                    } else if (rank == null) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ch.toString(),
                                fontFamily = MonoFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        val filledChar: Char? = when {
                            status == Stage6WordStatus.OUT_OF_ATTEMPTS || status == Stage6WordStatus.CORRECT -> ch
                            status == Stage6WordStatus.CORRECT_UMLAUT_WARNING -> ch
                            userInput.length <= blankIndices.size -> userInput.getOrNull(rank)
                            else -> userInput.getOrNull(idx)
                        }
                        val isNextActive = !status.isResolved && rank == activeRank
                        val slotBorder = when {
                            status == Stage6WordStatus.CORRECT -> SuccessGreen
                            status == Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                            status == Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                            isNextActive -> MaterialTheme.colorScheme.primary
                            filledChar != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            else -> extended.cardBorder
                        }
                        val slotBg = when {
                            status == Stage6WordStatus.CORRECT -> SuccessGreen.copy(alpha = 0.16f)
                            status == Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow.copy(alpha = 0.18f)
                            status == Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed.copy(alpha = 0.14f)
                            isNextActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else -> extended.subtleFill
                        }
                        val slotTextColor = when {
                            status == Stage6WordStatus.CORRECT -> SuccessGreen
                            status == Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                            status == Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 32.dp, height = 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(slotBg)
                                .border(
                                    width = if (isNextActive) 2.dp else 1.2.dp,
                                    color = slotBorder,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filledChar?.toString() ?: "_",
                                fontFamily = MonoFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (filledChar != null) slotTextColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

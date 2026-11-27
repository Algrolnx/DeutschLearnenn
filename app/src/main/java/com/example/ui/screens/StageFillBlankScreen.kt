package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FillBlankInputMode
import com.example.ui.LearningViewModel
import com.example.ui.Stage6WordStatus
import com.example.ui.components.SpeakerButton
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageFillBlankScreen(
    subtopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val questions by viewModel.stage5Questions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.stage5Index.collectAsStateWithLifecycle()
    val selectedOption by viewModel.stage5SelectedOption.collectAsStateWithLifecycle()
    val inputMode by viewModel.stage5InputMode.collectAsStateWithLifecycle()
    val keyboardInput by viewModel.stage5KeyboardInput.collectAsStateWithLifecycle()
    val keyboardStatus by viewModel.stage5KeyboardStatus.collectAsStateWithLifecycle()
    val keyboardAttemptsLeft by viewModel.stage5KeyboardAttemptsLeft.collectAsStateWithLifecycle()
    val keyboardErrorHint by viewModel.stage5KeyboardErrorHint.collectAsStateWithLifecycle()
    val score by viewModel.stage5Score.collectAsStateWithLifecycle()
    val isFinished by viewModel.stage5Finished.collectAsStateWithLifecycle()
    val unlockedSrs by viewModel.lastCompletedUnlockedSrs.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    val currentQ = questions.getOrNull(currentIndex)
    val progress = if (questions.isNotEmpty()) (currentIndex.toFloat() / questions.size) else 0f
    val isSessionActive = !isFinished && currentQ != null
    val isAnswered = selectedOption != null || keyboardStatus.isResolved

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
                            text = "Етап 6: Lückentext · Пропуски",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (currentQ != null) "Речення ${currentIndex + 1} з ${questions.size}" else "Завершено",
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
                    val nextEnabled = viewModel.getNextEnabledStage(6)
                    StageCompletedSuccessCard(
                        stageNumber = 6,
                        nextStageNumber = nextEnabled,
                        title = "Етап 6 пройдено!",
                        subtitle = when {
                            unlockedSrs -> "Усі обрані етапи пройдено на 100%! Через 6 годин слова з'являться у вкладці «Повторення»."
                            nextEnabled != null -> "Усі речення доповнено на 100%! Далі відкрито Етап $nextEnabled."
                            else -> "Етап пройдено на 100%! Щоб слова потрапили в «Повторення», пройдіть усі обрані етапи підтеми."
                        },
                        onNextStage = { viewModel.startNextEnabledStage(subtopicId, 6) },
                        onBackToHub = { viewModel.navigateBack() }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
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
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (currentQ.isRetry) {
                                    RetryMistakeBadge()
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (inputMode == FillBlankInputMode.KEYBOARD) {
                                            "Впишіть пропущене німецьке слово:"
                                        } else {
                                            "Доповніть речення пропущеним словом:"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val primaryColor = MaterialTheme.colorScheme.primary
                                val displaySentence = remember(
                                    isAnswered,
                                    currentQ.sentenceWithBlank,
                                    currentQ.fullSentence,
                                    currentQ.matchedToken,
                                    primaryColor
                                ) {
                                    buildFillBlankAnnotatedSentence(
                                        sentenceWithBlank = currentQ.sentenceWithBlank,
                                        fullSentence = currentQ.fullSentence,
                                        matchedToken = currentQ.matchedToken,
                                        isAnswered = isAnswered,
                                        blankColor = primaryColor,
                                        filledColor = SuccessGreen
                                    )
                                }

                                Text(
                                    text = displaySentence,
                                    fontFamily = LoraFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 27.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = currentQ.translation,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                if (isAnswered) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SpeakerButton(
                                        onSpeak = { slow -> viewModel.speak(currentQ.fullSentence, slow) },
                                        size = 46.dp
                                    )
                                }
                            }
                        }

                        if (inputMode == FillBlankInputMode.KEYBOARD) {
                            FillBlankKeyboardPanel(
                                currentInput = keyboardInput,
                                status = keyboardStatus,
                                attemptsLeft = keyboardAttemptsLeft,
                                errorHint = keyboardErrorHint,
                                correctAnswerDisplay = currentQ.matchedToken.ifBlank { currentQ.word.german },
                                baseWordHint = currentQ.word.translationUk,
                                onInputChange = { viewModel.updateStage5KeyboardInput(it) },
                                onInsertChar = { viewModel.insertStage5KeyboardChar(it) },
                                onHint = { viewModel.useStage5KeyboardHint() },
                                onSubmit = { viewModel.submitStage5KeyboardAnswer() }
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                currentQ.options.forEach { option ->
                                    val isSelected = selectedOption == option
                                    val isCorrect = option.equals(currentQ.correctOption, ignoreCase = true)

                                    val (bgColor, borderColor, textColor) = when {
                                        !isAnswered -> Triple(
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
                                            .height(58.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(bgColor)
                                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                            .clickable(enabled = !isAnswered) {
                                                viewModel.answerStage5(subtopicId, option)
                                            }
                                            .padding(horizontal = 18.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option,
                                                fontFamily = LoraFontFamily,
                                                fontSize = 17.sp,
                                                fontWeight = if (isSelected || (isAnswered && isCorrect)) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor
                                            )

                                            if (isAnswered) {
                                                if (isCorrect) Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                                else if (isSelected) Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isAnswered) {
                        val wasWrong = if (inputMode == FillBlankInputMode.KEYBOARD && keyboardStatus.isResolved) {
                            keyboardStatus == Stage6WordStatus.OUT_OF_ATTEMPTS
                        } else {
                            selectedOption != null && !selectedOption.equals(currentQ.correctOption, ignoreCase = true)
                        }
                        if (wasWrong) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Помилка · слово повернеться в кінці етапу в іншому реченні",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.nextStage5(subtopicId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("fillblank_next_button"),
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

@Composable
internal fun FillBlankModeToggle(
    mode: FillBlankInputMode,
    onSelectMode: (FillBlankInputMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = extended.subtleFill,
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val optionsSelected = mode == FillBlankInputMode.OPTIONS
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (optionsSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (optionsSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSelectMode(FillBlankInputMode.OPTIONS) }
                    .testTag("fillblank_mode_options")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewAgenda,
                        contentDescription = null,
                        tint = if (optionsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Варіанти",
                        fontFamily = ManropeFontFamily,
                        fontSize = 13.sp,
                        fontWeight = if (optionsSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (optionsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val keyboardSelected = mode == FillBlankInputMode.KEYBOARD
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (keyboardSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (keyboardSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onSelectMode(FillBlankInputMode.KEYBOARD) }
                    .testTag("fillblank_mode_keyboard")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = if (keyboardSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Клавіатура",
                        fontFamily = ManropeFontFamily,
                        fontSize = 13.sp,
                        fontWeight = if (keyboardSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (keyboardSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun FillBlankKeyboardPanel(
    currentInput: String,
    status: Stage6WordStatus,
    attemptsLeft: Int,
    errorHint: String?,
    correctAnswerDisplay: String,
    baseWordHint: String,
    onInputChange: (String) -> Unit,
    onInsertChar: (Char) -> Unit,
    onHint: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val isResolved = status.isResolved

    // Keep cursor synced to the end whenever programmatic hint/umlaut/prefix reset updates currentInput
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = currentInput, selection = TextRange(currentInput.length)))
    }
    LaunchedEffect(currentInput) {
        if (textFieldValue.text != currentInput) {
            textFieldValue = TextFieldValue(text = currentInput, selection = TextRange(currentInput.length))
        }
    }

    val borderColor = when (status) {
        Stage6WordStatus.CORRECT -> SuccessGreen
        Stage6WordStatus.CORRECT_UMLAUT_WARNING -> StreakAmber
        Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
        Stage6WordStatus.TYPING -> MaterialTheme.colorScheme.primary
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Слово: $baseWordHint",
                fontFamily = ManropeFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = extended.subtleFill
            ) {
                Text(
                    text = "Спроби: $attemptsLeft / ${LearningViewModel.MAX_STAGE6_ATTEMPTS}",
                    fontFamily = MonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (attemptsLeft <= 1) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Anti-T9 keyboard input: KeyboardType.Password + autoCorrectEnabled = false disables predictive bar
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { updated ->
                if (!isResolved) {
                    textFieldValue = updated
                    onInputChange(updated.text)
                }
            },
            enabled = !isResolved,
            singleLine = true,
            placeholder = {
                Text(
                    text = "Введіть пропущене слово...",
                    fontFamily = ManropeFontFamily,
                    fontSize = 15.sp
                )
            },
            textStyle = TextStyle(
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (!isResolved) onSubmit() }
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = when (status) {
                    Stage6WordStatus.CORRECT -> SuccessGreen.copy(alpha = 0.12f)
                    Stage6WordStatus.CORRECT_UMLAUT_WARNING -> StreakAmber.copy(alpha = 0.12f)
                    Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surface
                },
                focusedBorderColor = borderColor,
                unfocusedBorderColor = if (status == Stage6WordStatus.WRONG_TRY) ErrorRed else extended.cardBorder,
                disabledBorderColor = borderColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fillblank_keyboard_input")
        )

        if (!isResolved) {
            // Quick German umlaut bar + 1-letter hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf('ä', 'ö', 'ü', 'ß').forEach { umlaut ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, extended.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onInsertChar(umlaut) }
                            .testTag("fillblank_umlaut_$umlaut")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = umlaut.toString(),
                                fontFamily = LoraFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onHint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("fillblank_hint_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Підказка",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Літера", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!errorHint.isNullOrBlank()) {
                Text(
                    text = errorHint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ErrorRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onSubmit,
                enabled = currentInput.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("fillblank_submit_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Перевірити", fontFamily = LoraFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            val (bannerBg, bannerBorder, bannerText, bannerColor) = when (status) {
                Stage6WordStatus.CORRECT -> Quadruple(
                    SuccessGreen.copy(alpha = 0.14f),
                    SuccessGreen,
                    "Вірно! «$correctAnswerDisplay»",
                    SuccessGreen
                )
                Stage6WordStatus.CORRECT_UMLAUT_WARNING -> Quadruple(
                    StreakAmber.copy(alpha = 0.14f),
                    StreakAmber,
                    "Зараховано! Зверніть увагу на умлаут/ß: «$correctAnswerDisplay»",
                    StreakAmber
                )
                else -> Quadruple(
                    ErrorRed.copy(alpha = 0.14f),
                    ErrorRed,
                    "Правильна відповідь: «$correctAnswerDisplay»",
                    ErrorRed
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bannerBg,
                border = BorderStroke(1.dp, bannerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = bannerText,
                    fontFamily = ManropeFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = bannerColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

internal fun buildFillBlankAnnotatedSentence(
    sentenceWithBlank: String,
    fullSentence: String,
    matchedToken: String,
    isAnswered: Boolean,
    blankColor: Color,
    filledColor: Color
): AnnotatedString = buildAnnotatedString {
    if (!isAnswered) {
        val blankMarker = "_________"
        val blankIndex = sentenceWithBlank.indexOf(blankMarker)
        if (blankIndex >= 0) {
            append(sentenceWithBlank.substring(0, blankIndex))
            withStyle(
                SpanStyle(
                    color = blankColor,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append(blankMarker)
            }
            append(sentenceWithBlank.substring(blankIndex + blankMarker.length))
        } else {
            append(sentenceWithBlank)
        }
    } else {
        val token = matchedToken.trim()
        val matchRange = if (token.isNotEmpty()) {
            val pattern = Regex(
                "(?<![A-Za-zÄÖÜäöüß])${Regex.escape(token)}(?![A-Za-zÄÖÜäöüß])",
                RegexOption.IGNORE_CASE
            )
            pattern.find(fullSentence)?.range ?: run {
                val idx = fullSentence.indexOf(token, ignoreCase = true)
                if (idx >= 0) idx until (idx + token.length) else null
            }
        } else {
            null
        }
        if (matchRange != null) {
            append(fullSentence.substring(0, matchRange.first))
            withStyle(
                SpanStyle(
                    color = filledColor,
                    fontWeight = FontWeight.ExtraBold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(fullSentence.substring(matchRange.first, matchRange.last + 1))
            }
            append(fullSentence.substring(matchRange.last + 1))
        } else {
            append(fullSentence)
        }
    }
}

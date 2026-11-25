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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WordEntity
import com.example.ui.LearningViewModel
import com.example.ui.Stage6InputMode
import com.example.ui.Stage6WordStatus
import com.example.ui.components.GenderBadge
import com.example.ui.components.SpeakerButton
import com.example.ui.components.getGenderColor
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
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
    val selectedArticle by viewModel.stage6SelectedArticle.collectAsStateWithLifecycle()
    val errorHint by viewModel.stage6ErrorHint.collectAsStateWithLifecycle()
    val status by viewModel.stage6Status.collectAsStateWithLifecycle()
    val attemptsLeft by viewModel.stage6AttemptsLeft.collectAsStateWithLifecycle()
    val isFinished by viewModel.stage6Finished.collectAsStateWithLifecycle()
    val inputMode by viewModel.stage6InputMode.collectAsStateWithLifecycle()
    val retryIndices by viewModel.stage6RetryIndices.collectAsStateWithLifecycle()
    val unlockedSrs by viewModel.lastCompletedUnlockedSrs.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    val currentWord = words.getOrNull(currentIndex)
    val isRetryWord = currentIndex in retryIndices
    val progress = if (words.isNotEmpty()) (currentIndex.toFloat() / words.size) else 0f
    val isSessionActive = !isFinished && currentWord != null
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
                            text = "Етап 7: Правопис · Schreiben",
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
                                .testTag("stage6_attempts_badge")
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

                if (isFinished || currentWord == null) {
                    val nextEnabled = viewModel.getNextEnabledStage(7)
                    StageCompletedSuccessCard(
                        stageNumber = 7,
                        nextStageNumber = nextEnabled,
                        title = if (unlockedSrs) "Підтему завершено на 100%!" else "Етап 7 пройдено!",
                        subtitle = when {
                            unlockedSrs -> "Вітаємо! Усі 8 етапів пройдено на 100% — слова додано в цикл запам'ятовування (перше повторення через 6 годин у вкладці «Повторення»)."
                            nextEnabled != null -> "Усі слова написано на 100%! Далі відкрито фінальний Етап $nextEnabled."
                            else -> "Етап 7 пройдено на 100%! Щоб слова потрапили у вкладку «Повторення», пройдіть усі 8 етапів цієї підтеми."
                        },
                        onNextStage = { viewModel.startNextEnabledStage(subtopicId, 7) },
                        onBackToHub = { viewModel.navigateBack() }
                    )
                } else {
                    val requiresArticle = LearningViewModel.requiresArticleInSpelling(currentWord)
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
                                if (isRetryWord) {
                                    RetryMistakeBadge()
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (isWordResolved && currentWord.article.isNotBlank()) {
                                    GenderBadge(article = currentWord.article)
                                    Spacer(modifier = Modifier.height(6.dp))
                                } else if (requiresArticle) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Потрібен артикль: der / die / das",
                                            fontFamily = MonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Text(
                                    text = currentWord.translationUk,
                                    fontFamily = LoraFontFamily,
                                    fontSize = 23.sp,
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
                                    onSpeak = { slow ->
                                        val spoken = if (requiresArticle && !isWordResolved) {
                                            currentWord.german
                                        } else {
                                            currentWord.displayWithArticle
                                        }
                                        viewModel.speak(spoken, slow)
                                    },
                                    size = 46.dp
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val boxBorder = when (status) {
                                Stage6WordStatus.CORRECT -> SuccessGreen
                                Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                                Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                Stage6WordStatus.TYPING -> extended.cardBorder
                            }

                            if (inputMode == Stage6InputMode.KEYBOARD) {
                                var textFieldValue by remember(currentWord.id, currentIndex) {
                                    mutableStateOf(
                                        TextFieldValue(
                                            text = currentInput,
                                            selection = TextRange(currentInput.length)
                                        )
                                    )
                                }
                                LaunchedEffect(currentInput) {
                                    if (textFieldValue.text != currentInput) {
                                        textFieldValue = TextFieldValue(
                                            text = currentInput,
                                            selection = TextRange(currentInput.length)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = textFieldValue,
                                    onValueChange = { newValue ->
                                        textFieldValue = newValue
                                        viewModel.updateStage6TypedText(newValue.text)
                                    },
                                    enabled = !isWordResolved,
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            text = if (requiresArticle) {
                                                "Введіть з артиклем (der / die / das)..."
                                            } else {
                                                "Введіть слово..."
                                            },
                                            fontFamily = MonoFontFamily,
                                            fontSize = 15.sp
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = MonoFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (status) {
                                            Stage6WordStatus.CORRECT -> SuccessGreen
                                            Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                                            Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                            Stage6WordStatus.TYPING -> MaterialTheme.colorScheme.onSurface
                                        }
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done,
                                        autoCorrectEnabled = false
                                    ),
                                    visualTransformation = VisualTransformation.None,
                                    keyboardActions = KeyboardActions(
                                        onDone = { viewModel.submitStage6TypedAnswer() }
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
                                        .testTag("stage6_keyboard_input")
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(62.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when (status) {
                                                Stage6WordStatus.CORRECT -> SuccessGreen.copy(alpha = 0.12f)
                                                Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellowBg
                                                Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed.copy(alpha = 0.12f)
                                                Stage6WordStatus.TYPING -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .border(2.dp, boxBorder, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (requiresArticle) {
                                            val shownArticle = when {
                                                status == Stage6WordStatus.CORRECT || status == Stage6WordStatus.OUT_OF_ATTEMPTS ->
                                                    currentWord.article.lowercase()
                                                else -> selectedArticle
                                            }
                                            val badgeColor = if (shownArticle != null) {
                                                getGenderColor(shownArticle)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (shownArticle != null) {
                                                    badgeColor.copy(alpha = 0.16f)
                                                } else {
                                                    extended.subtleFill
                                                }
                                            ) {
                                                Text(
                                                    text = shownArticle ?: "___",
                                                    fontFamily = MonoFontFamily,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }

                                        Text(
                                            text = if (currentInput.isEmpty()) "Натискайте літери..." else currentInput,
                                            fontFamily = MonoFontFamily,
                                            fontSize = if (currentInput.isEmpty()) 17.sp else 21.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = if (currentInput.isEmpty()) 0.sp else 1.5.sp,
                                            color = when (status) {
                                                Stage6WordStatus.CORRECT -> SuccessGreen
                                                Stage6WordStatus.CORRECT_UMLAUT_WARNING -> extended.warningYellow
                                                Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                                Stage6WordStatus.TYPING -> if (currentInput.isEmpty()) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            when (status) {
                                Stage6WordStatus.CORRECT -> {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Richtig! ${currentWord.displayWithArticle}",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Stage6WordStatus.CORRECT_UMLAUT_WARNING -> {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    UmlautCorrectedFeedbackCard(word = currentWord)
                                }
                                Stage6WordStatus.WRONG_TRY -> {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val msg = errorHint?.let { "$it (спроб: $attemptsLeft)" }
                                        ?: if (currentInput.isNotEmpty()) {
                                            "Невірно · правильний початок збережено, продовжуйте далі (спроб: $attemptsLeft)"
                                        } else {
                                            "Невірно · спробуйте з початку або натисніть «Підказка» (спроб: $attemptsLeft)"
                                        }
                                    Text(
                                        text = msg,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Stage6WordStatus.OUT_OF_ATTEMPTS -> {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Спроби вичерпано · Правильно: ${currentWord.displayWithArticle}. Слово повернеться в кінці етапу.",
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Stage6WordStatus.TYPING -> {
                                    errorHint?.takeIf { it.isNotBlank() }?.let { hint ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = hint,
                                            color = StreakAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        if (!isWordResolved) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (inputMode == Stage6InputMode.LETTER_TILES && requiresArticle) {
                                    ArticleTileSelectorRow(
                                        selectedArticle = selectedArticle,
                                        onSelect = { viewModel.selectStage6Article(it) },
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                }

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
                                                .clickable { viewModel.insertStage6Umlaut(umlaut) },
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
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isWordResolved) {
                        Button(
                            onClick = { viewModel.nextStage6(subtopicId) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("stage6_next_button")
                        ) {
                            Text(
                                text = if (currentIndex + 1 < words.size) "Далі · Weiter" else "Завершити",
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
                                onClick = { viewModel.submitStage6TypedAnswer() },
                                enabled = currentInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(48.dp)
                                    .testTag("stage6_check_button")
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
                                onClick = { viewModel.useStage6Hint() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StreakAmber.copy(alpha = 0.20f),
                                    contentColor = StreakAmber
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stage6_hint_button")
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
                                onClick = { viewModel.removeStage6Letter() },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(48.dp)
                                    .testTag("stage6_backspace_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Видалити символ",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { viewModel.clearStage6() },
                                colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stage6_clear_button")
                            ) {
                                Text(
                                    text = "Очистити",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { viewModel.useStage6Hint() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StreakAmber.copy(alpha = 0.20f),
                                    contentColor = StreakAmber
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(48.dp)
                                    .testTag("stage6_hint_button")
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

@Composable
internal fun UmlautCorrectedFeedbackCard(
    word: WordEntity,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val highlightedWord = remember(word.displayWithArticle, extended.warningYellow) {
        val specialChars = setOf('ä', 'ö', 'ü', 'ß', 'Ä', 'Ö', 'Ü')
        buildAnnotatedString {
            word.displayWithArticle.forEach { ch ->
                if (ch in specialChars) {
                    withStyle(
                        SpanStyle(
                            color = extended.warningYellow,
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(ch)
                    }
                } else {
                    append(ch)
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = extended.warningYellowBg,
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, extended.warningYellow, RoundedCornerShape(14.dp))
            .testTag("umlaut_warning_feedback_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Зараховано! Зверніть увагу на умлаут:",
                color = extended.warningYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = highlightedWord,
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun ArticleTileSelectorRow(
    selectedArticle: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "der" to extended.genderDer,
            "die" to extended.genderDie,
            "das" to extended.genderDas
        ).forEach { (article, color) ->
            val isSelected = selectedArticle.equals(article, ignoreCase = true)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) color.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else extended.cardBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(article) }
                    .testTag("spelling_article_chip_$article"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = article,
                    fontFamily = MonoFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

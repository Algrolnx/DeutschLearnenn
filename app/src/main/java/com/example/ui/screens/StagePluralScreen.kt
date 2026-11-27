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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.Screen
import com.example.ui.components.GenderBadge
import com.example.ui.components.SpeakerButton
import com.example.ui.components.WordIllustrationView
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.util.GrammarRuleExplainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StagePluralScreen(
    subtopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val questions by viewModel.stage5PluralQuestions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.stage5PluralIndex.collectAsStateWithLifecycle()
    val selectedPlural by viewModel.stage5PluralSelected.collectAsStateWithLifecycle()
    val score by viewModel.stage5PluralScore.collectAsStateWithLifecycle()
    val isFinished by viewModel.stage5PluralFinished.collectAsStateWithLifecycle()
    val autoSkipped by viewModel.stage5PluralAutoSkipped.collectAsStateWithLifecycle()
    val unlockedSrs by viewModel.lastCompletedUnlockedSrs.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }

    val currentQ = questions.getOrNull(currentIndex)
    val progress = if (questions.isNotEmpty()) (currentIndex.toFloat() / questions.size) else 1f
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
                            text = "Етап 5: Plural-Trainer · Множина",
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
                    if (questions.isNotEmpty()) {
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
                    val nextEnabled = viewModel.getNextEnabledStage(5)
                    StageCompletedSuccessCard(
                        stageNumber = 5,
                        nextStageNumber = nextEnabled,
                        title = if (autoSkipped) "Етап 5 зараховано автоматично" else "Етап 5 пройдено!",
                        subtitle = when {
                            autoSkipped && unlockedSrs -> "У цій підтемі немає іменників із формою множини — етап зараховано, і всі обрані етапи завершено на 100%!"
                            autoSkipped && nextEnabled != null -> "У цій підтемі немає іменників із формою множини (наприклад, дієслова чи прикметники) — етап зараховано. Далі відкрито Етап $nextEnabled."
                            autoSkipped -> "У цій підтемі немає іменників із формою множини — етап зараховано автоматично."
                            unlockedSrs -> "Усі обрані етапи пройдено на 100%! Через 6 годин слова з'являться у вкладці «Повторення»."
                            nextEnabled != null -> "Усі форми множини засвоєно на 100%! Далі відкрито Етап $nextEnabled."
                            else -> "Етап пройдено на 100%! Щоб слова потрапили в «Повторення», пройдіть усі обрані етапи підтеми."
                        },
                        onNextStage = { viewModel.startNextEnabledStage(subtopicId, 5) },
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
                            if (currentQ.isRetry) {
                                RetryMistakeBadge()
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            WordIllustrationView(
                                word = currentQ.word,
                                height = 136.dp,
                                showBadges = false
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DynamicFeed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Оберіть правильну форму множини (Plural):",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (selectedPlural != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = extended.warningYellowBg,
                                    modifier = Modifier.border(1.dp, extended.warningYellow.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = "Множина: ${currentQ.correctPlural}",
                                        fontFamily = LoraFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.warningYellow,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
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

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${currentQ.word.translationUk} · ${currentQ.word.translationEn}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            SpeakerButton(
                                onSpeak = { slow ->
                                    val spokenText = if (selectedPlural != null) {
                                        currentQ.correctPlural
                                    } else {
                                        currentQ.word.displayWithArticle
                                    }
                                    viewModel.speak(spokenText, slow)
                                },
                                size = 46.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentQ.options.forEachIndexed { idx, option ->
                            val isSelected = selectedPlural == option
                            val isCorrect = option.equals(currentQ.correctPlural, ignoreCase = true)

                            val (bgColor, borderColor, textColor) = when {
                                selectedPlural == null -> Triple(
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
                                    .clickable(enabled = selectedPlural == null) {
                                        viewModel.answerStage5Plural(subtopicId, option)
                                    }
                                    .padding(horizontal = 18.dp)
                                    .testTag("plural_option_$idx"),
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
                                        fontSize = 18.sp,
                                        fontWeight = if (isSelected || (selectedPlural != null && isCorrect)) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )

                                    if (selectedPlural != null) {
                                        if (isCorrect) Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                        else if (isSelected) Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed)
                                    }
                                }
                            }
                        }

                        if (selectedPlural != null) {
                            val pluralHint = remember(currentQ.word.id, currentQ.word.german, currentQ.word.plural) {
                                GrammarRuleExplainer.explainPlural(currentQ.word)
                            }
                            if (pluralHint != null) {
                                InlineGrammarHintCard(
                                    hint = pluralHint,
                                    onOpenGrammar = { topicId ->
                                        viewModel.navigateTo(Screen.GrammarGuide(topicId))
                                    }
                                )
                            }
                        }
                    }

                    if (selectedPlural != null) {
                        if (!selectedPlural.equals(currentQ.correctPlural, ignoreCase = true)) {
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
                            onClick = { viewModel.nextStage5Plural(subtopicId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("plural_next_button"),
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

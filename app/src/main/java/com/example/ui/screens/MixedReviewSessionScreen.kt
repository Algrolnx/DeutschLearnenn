package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.MixedReviewItem
import com.example.ui.Stage6WordStatus
import com.example.ui.components.GenderBadge
import com.example.ui.components.SpeakerButton
import com.example.ui.components.WordFlashcard
import com.example.ui.components.WordIllustrationView
import com.example.ui.components.getGenderColor
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen
import com.example.util.SpacedRepetition

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MixedReviewSessionScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val items by viewModel.mixedReviewItems.collectAsStateWithLifecycle()
    val currentIndex by viewModel.mixedReviewIndex.collectAsStateWithLifecycle()
    val score by viewModel.mixedReviewScore.collectAsStateWithLifecycle()
    val isFinished by viewModel.mixedReviewFinished.collectAsStateWithLifecycle()
    val isEarlyPractice by viewModel.mixedReviewIsEarlyPractice.collectAsStateWithLifecycle()
    val isCardFlipped by viewModel.mixedCardFlipped.collectAsStateWithLifecycle()
    val selectedOptionIndex by viewModel.mixedSelectedOptionIndex.collectAsStateWithLifecycle()
    val selectedString by viewModel.mixedSelectedString.collectAsStateWithLifecycle()
    val spellingInput by viewModel.mixedSpellingInput.collectAsStateWithLifecycle()
    val spellingStatus by viewModel.mixedSpellingStatus.collectAsStateWithLifecycle()
    val spellingAttemptsLeft by viewModel.mixedSpellingAttemptsLeft.collectAsStateWithLifecycle()
    val lastFeedback by viewModel.mixedLastFeedback.collectAsStateWithLifecycle()
    val dashboard by viewModel.reviewDashboard.collectAsStateWithLifecycle()

    var showExitDialog by remember { mutableStateOf(false) }

    val currentItem = items.getOrNull(currentIndex)
    val progress = if (items.isNotEmpty()) (currentIndex.toFloat() / items.size) else 0f
    val isSessionActive = !isFinished && currentItem != null

    val requestExit: () -> Unit = {
        if (isSessionActive) {
            showExitDialog = true
        } else {
            viewModel.navigateBack()
        }
    }

    BackHandler { requestExit() }

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
                            text = if (isEarlyPractice) "Тренування · Мікс етапів" else "Повторення · Мікс етапів",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (currentItem != null && !isFinished) {
                                "${shortStageNameUk(currentItem.stageType)} · ${currentIndex + 1} з ${items.size}"
                            } else {
                                "Раунд завершено"
                            },
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
                    if (currentItem != null && !isFinished) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 14.dp)
                        ) {
                            Text(
                                text = "Повтор ${currentItem.srsStep}/${SpacedRepetition.TARGET_REPETITIONS}",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
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
                    progress = { if (isFinished) 1f else progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = extended.subtleFill
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (isFinished || currentItem == null) {
                    Spacer(modifier = Modifier.weight(0.25f))
                    MixedReviewCompletedCard(
                        score = score,
                        total = items.size,
                        reviewsDoneToday = dashboard.reviewsDoneToday,
                        dailyLimit = dashboard.dailyLimit,
                        canStartAnotherRound = dashboard.remainingDailyQuota > 0 && dashboard.dueCount > 0,
                        onNextRound = { viewModel.startMixedReviewSession(earlyPractice = false) },
                        onDone = { viewModel.navigateBack() }
                    )
                    Spacer(modifier = Modifier.weight(0.75f))
                } else {
                    when (currentItem) {
                        is MixedReviewItem.FlashcardReview -> {
                            WordFlashcard(
                                word = currentItem.word,
                                isFlipped = isCardFlipped,
                                currentIndex = currentIndex,
                                totalCount = items.size,
                                onFlip = { viewModel.flipMixedFlashcard() },
                                onSpeakWord = { slow -> viewModel.speak(currentItem.word.displayWithArticle, slow) },
                                onSpeakExample = {
                                    if (currentItem.word.exampleDe.isNotBlank()) {
                                        viewModel.speak(currentItem.word.exampleDe)
                                    }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(currentItem.word) },
                                showNavigationArrows = false,
                                showSlowSpeedButton = true,
                                allowPhotoChange = false,
                                cardTestTag = "mixed_flashcard",
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isCardFlipped) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.answerMixedFlashcard(remembered = false) },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, StreakAmber),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StreakAmber),
                                        modifier = Modifier.weight(1f).height(52.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ще повторити", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.answerMixedFlashcard(remembered = true) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                                        modifier = Modifier.weight(1f).height(52.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(19.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Пам'ятаю", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.flipMixedFlashcard() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Перевернути картку", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }

                        is MixedReviewItem.ListeningReview -> {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, extended.cardBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Прослухайте та оберіть слово:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(74.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .clickable { viewModel.speak(currentItem.word.displayWithArticle) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Прослухати", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { viewModel.speak(currentItem.word.displayWithArticle, false) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) { Text("Звичайно", fontWeight = FontWeight.SemiBold) }
                                        FilledTonalButton(
                                            onClick = { viewModel.speak(currentItem.word.displayWithArticle, true) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) { Text("0.75x", fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            MixedOptionIndexList(
                                options = currentItem.options,
                                selectedIndex = selectedOptionIndex,
                                correctIndex = currentItem.correctIndex,
                                onSelect = { viewModel.answerMixedOptionIndex(it) },
                                modifier = Modifier.weight(1f)
                            )

                            MixedNextFooter(
                                isAnswered = selectedOptionIndex != null,
                                feedback = lastFeedback,
                                isLast = currentIndex + 1 >= items.size,
                                onNext = { viewModel.nextMixedReviewItem() }
                            )
                        }

                        is MixedReviewItem.MultipleChoiceReview -> {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, extended.cardBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    WordIllustrationView(word = currentItem.word, height = 136.dp, showBadges = false)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        if (currentItem.word.article.isNotBlank()) {
                                            GenderBadge(article = currentItem.word.article)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = currentItem.word.german,
                                            fontFamily = LoraFontFamily,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SpeakerButton(
                                        onSpeak = { slow -> viewModel.speak(currentItem.word.displayWithArticle, slow) },
                                        size = 44.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            MixedOptionIndexList(
                                options = currentItem.options,
                                selectedIndex = selectedOptionIndex,
                                correctIndex = currentItem.correctIndex,
                                onSelect = { viewModel.answerMixedOptionIndex(it) },
                                modifier = Modifier.weight(1f)
                            )

                            MixedNextFooter(
                                isAnswered = selectedOptionIndex != null,
                                feedback = lastFeedback,
                                isLast = currentIndex + 1 >= items.size,
                                onNext = { viewModel.nextMixedReviewItem() }
                            )
                        }

                        is MixedReviewItem.ArticleReview -> {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, extended.cardBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    WordIllustrationView(word = currentItem.word, height = 136.dp, showBadges = false)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Оберіть правильний артикль:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (selectedString != null) "${currentItem.correctArticle} ${currentItem.word.german}" else "___ ${currentItem.word.german}",
                                        fontFamily = LoraFontFamily,
                                        fontSize = 29.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedString != null) {
                                            if (selectedString.equals(currentItem.correctArticle, ignoreCase = true)) SuccessGreen else ErrorRed
                                        } else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${currentItem.word.translationUk} · ${currentItem.word.translationEn}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ArticleChoiceButton(
                                    article = "der",
                                    genderLabel = "Чоловічий рід (Maskulin)",
                                    color = extended.genderDer,
                                    isSelected = selectedString == "der",
                                    isCorrect = currentItem.correctArticle == "der",
                                    showResult = selectedString != null,
                                    onClick = { viewModel.answerMixedArticle("der") }
                                )
                                ArticleChoiceButton(
                                    article = "die",
                                    genderLabel = "Жіночий рід (Feminin)",
                                    color = extended.genderDie,
                                    isSelected = selectedString == "die",
                                    isCorrect = currentItem.correctArticle == "die",
                                    showResult = selectedString != null,
                                    onClick = { viewModel.answerMixedArticle("die") }
                                )
                                ArticleChoiceButton(
                                    article = "das",
                                    genderLabel = "Середній рід (Neutrum)",
                                    color = extended.genderDas,
                                    isSelected = selectedString == "das",
                                    isCorrect = currentItem.correctArticle == "das",
                                    showResult = selectedString != null,
                                    onClick = { viewModel.answerMixedArticle("das") }
                                )
                            }

                            MixedNextFooter(
                                isAnswered = selectedString != null,
                                feedback = lastFeedback,
                                isLast = currentIndex + 1 >= items.size,
                                onNext = { viewModel.nextMixedReviewItem() }
                            )
                        }

                        is MixedReviewItem.FillBlankReview -> {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, extended.cardBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Доповніть речення:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (selectedString != null) currentItem.word.exampleDe else currentItem.sentenceWithBlank,
                                        fontFamily = LoraFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 27.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentItem.translation,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                currentItem.options.forEach { option ->
                                    val isSelected = selectedString == option
                                    val isCorrect = option.equals(currentItem.correctOption, ignoreCase = true)
                                    val (bgColor, borderColor, textColor) = when {
                                        selectedString == null -> Triple(MaterialTheme.colorScheme.surface, extended.cardBorder, MaterialTheme.colorScheme.onSurface)
                                        isCorrect -> Triple(SuccessGreen.copy(alpha = 0.14f), SuccessGreen, SuccessGreen)
                                        isSelected -> Triple(ErrorRed.copy(alpha = 0.14f), ErrorRed, ErrorRed)
                                        else -> Triple(MaterialTheme.colorScheme.surface, extended.cardBorder.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(bgColor)
                                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                            .clickable(enabled = selectedString == null) { viewModel.answerMixedFillBlank(option) }
                                            .padding(horizontal = 16.dp),
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
                                                fontWeight = if (isSelected || (selectedString != null && isCorrect)) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor
                                            )
                                            if (selectedString != null) {
                                                if (isCorrect) Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                                else if (isSelected) Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed)
                                            }
                                        }
                                    }
                                }
                            }

                            MixedNextFooter(
                                isAnswered = selectedString != null,
                                feedback = lastFeedback,
                                isLast = currentIndex + 1 >= items.size,
                                onNext = { viewModel.nextMixedReviewItem() }
                            )
                        }

                        is MixedReviewItem.SpellingReview -> {
                            val isResolved = spellingStatus == Stage6WordStatus.CORRECT || spellingStatus == Stage6WordStatus.OUT_OF_ATTEMPTS
                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
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
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (currentItem.word.article.isNotBlank()) {
                                                GenderBadge(article = currentItem.word.article)
                                            } else {
                                                Spacer(modifier = Modifier.width(1.dp))
                                            }
                                            Text(
                                                text = "Спроби: $spellingAttemptsLeft / ${LearningViewModel.MAX_STAGE6_ATTEMPTS}",
                                                fontFamily = MonoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (spellingAttemptsLeft == 1) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentItem.word.translationUk,
                                            fontFamily = LoraFontFamily,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = currentItem.word.translationEn,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (currentItem.word.transcription.isNotBlank()) {
                                            Text(
                                                text = currentItem.word.transcription,
                                                fontFamily = MonoFontFamily,
                                                fontStyle = FontStyle.Italic,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        SpeakerButton(
                                            onSpeak = { slow -> viewModel.speak(currentItem.word.displayWithArticle, slow) },
                                            size = 44.dp
                                        )
                                    }
                                }

                                val boxBorder = when (spellingStatus) {
                                    Stage6WordStatus.CORRECT -> SuccessGreen
                                    Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                    Stage6WordStatus.TYPING -> extended.cardBorder
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when (spellingStatus) {
                                                Stage6WordStatus.CORRECT -> SuccessGreen.copy(alpha = 0.12f)
                                                Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed.copy(alpha = 0.12f)
                                                Stage6WordStatus.TYPING -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .border(2.dp, boxBorder, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (currentItem.word.article.isNotBlank()) {
                                            val genderColor = getGenderColor(currentItem.word.article)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = genderColor.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = currentItem.word.article.lowercase(),
                                                    fontFamily = MonoFontFamily,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = genderColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Text(
                                            text = if (spellingInput.isEmpty()) "Натискайте літери..." else spellingInput,
                                            fontFamily = MonoFontFamily,
                                            fontSize = if (spellingInput.isEmpty()) 16.sp else 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (spellingStatus) {
                                                Stage6WordStatus.CORRECT -> SuccessGreen
                                                Stage6WordStatus.WRONG_TRY, Stage6WordStatus.OUT_OF_ATTEMPTS -> ErrorRed
                                                Stage6WordStatus.TYPING -> if (spellingInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }

                                if (spellingStatus == Stage6WordStatus.WRONG_TRY) {
                                    Text(
                                        text = "Невірно · залишилось спроб: $spellingAttemptsLeft",
                                        color = ErrorRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (spellingStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) {
                                    Text(
                                        text = "Правильно: ${currentItem.word.displayWithArticle}",
                                        color = ErrorRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (!isResolved) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        listOf('ä', 'ö', 'ü', 'ß', 'Ä', 'Ö', 'Ü').forEach { umlaut ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .clickable { viewModel.addMixedSpellingLetter(umlaut) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(umlaut.toString(), fontFamily = LoraFontFamily, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        currentItem.letterPool.forEach { char ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, extended.cardBorder, RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.addMixedSpellingLetter(char) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(char.toString(), fontFamily = LoraFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (isResolved) {
                                MixedNextFooter(
                                    isAnswered = true,
                                    feedback = lastFeedback,
                                    isLast = currentIndex + 1 >= items.size,
                                    onNext = { viewModel.nextMixedReviewItem() }
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.removeMixedSpellingLetter() },
                                        colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.8f).height(48.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Видалити", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = { viewModel.clearMixedSpelling() },
                                        colors = ButtonDefaults.buttonColors(containerColor = extended.subtleFill),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Очистити", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Button(
                                        onClick = { viewModel.useMixedSpellingHint() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StreakAmber.copy(alpha = 0.20f),
                                            contentColor = StreakAmber
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.25f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Підказка", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MixedOptionIndexList(
    options: List<String>,
    selectedIndex: Int?,
    correctIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val labels = listOf("A", "B", "C", "D")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            val isCorrect = index == correctIndex
            val (bgColor, borderColor, textColor) = when {
                selectedIndex == null -> Triple(MaterialTheme.colorScheme.surface, extended.cardBorder, MaterialTheme.colorScheme.onSurface)
                isCorrect -> Triple(SuccessGreen.copy(alpha = 0.14f), SuccessGreen, SuccessGreen)
                isSelected -> Triple(ErrorRed.copy(alpha = 0.14f), ErrorRed, ErrorRed)
                else -> Triple(MaterialTheme.colorScheme.surface, extended.cardBorder.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable(enabled = selectedIndex == null) { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        selectedIndex != null && isCorrect -> SuccessGreen
                                        selectedIndex != null && isSelected -> ErrorRed
                                        else -> extended.subtleFill
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labels.getOrElse(index) { "${index + 1}" },
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedIndex != null && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected || (selectedIndex != null && isCorrect)) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                    if (selectedIndex != null) {
                        if (isCorrect) Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                        else if (isSelected) Icon(Icons.Default.Close, contentDescription = null, tint = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun MixedNextFooter(
    isAnswered: Boolean,
    feedback: String?,
    isLast: Boolean,
    onNext: () -> Unit
) {
    if (!isAnswered) return
    val extended = LocalAppExtendedColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!feedback.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = extended.subtleFill,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedback,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("mixed_next_button")
        ) {
            Text(
                text = if (!isLast) "Далі · Weiter" else "Підсумок раунду",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        }
    }
}

@Composable
private fun MixedReviewCompletedCard(
    score: Int,
    total: Int,
    reviewsDoneToday: Int,
    dailyLimit: Int,
    canStartAnotherRound: Boolean,
    onNextRound: () -> Unit,
    onDone: () -> Unit
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Celebration, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(34.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Раунд повторення завершено!",
                fontFamily = LoraFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Результат: $score з $total · Сьогодні повторено: $reviewsDoneToday / $dailyLimit слів",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (canStartAnotherRound) {
                Button(
                    onClick = onNextRound,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Ще один раунд", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = onDone,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("До вкладки Повторення", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

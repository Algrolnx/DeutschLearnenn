package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ImportantSentence
import com.example.ui.LearningViewModel
import com.example.ui.components.SpeakerButton
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceSituationScreen(
    level: String,
    situationId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val group = remember(level, situationId) {
        viewModel.getSituationGroup(level, situationId)
    }
    val learnedIds by viewModel.learnedSentenceIds.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteSentenceIds.collectAsStateWithLifecycle()

    var isTrainingMode by remember { mutableStateOf(false) }
    var trainingQueue by remember(level, situationId) {
        mutableStateOf(group?.sentences.orEmpty())
    }
    var trainingIndex by remember { mutableIntStateOf(0) }
    var isAnswerRevealed by remember { mutableStateOf(false) }

    val startTraining: () -> Unit = {
        val all = group?.sentences.orEmpty()
        val unlearned = all.filter { it.id !in learnedIds }
        trainingQueue = (if (unlearned.isNotEmpty()) unlearned else all).shuffled()
        trainingIndex = 0
        isAnswerRevealed = false
        isTrainingMode = true
    }

    BackHandler {
        if (isTrainingMode) {
            isTrainingMode = false
        } else {
            viewModel.navigateBack()
        }
    }

    val sentences = group?.sentences.orEmpty()
    val learnedInGroup = sentences.count { it.id in learnedIds }
    val groupProgress = if (sentences.isNotEmpty()) {
        (learnedInGroup.toFloat() / sentences.size).coerceIn(0f, 1f)
    } else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = group?.titleDe ?: "Wichtige Sätze",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isTrainingMode) {
                                "Тренування · ${group?.titleUk ?: ""}"
                            } else {
                                "${group?.titleUk ?: ""} · Рівень $level"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isTrainingMode) isTrainingMode = false else viewModel.navigateBack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sentence_situation_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (!isTrainingMode && sentences.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = startTraining,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .height(38.dp)
                                .testTag("sentence_train_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Тренувати", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isTrainingMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 14.dp)
                        ) {
                            val shownIdx = (trainingIndex + 1).coerceAtMost(trainingQueue.size.coerceAtLeast(1))
                            Text(
                                text = "$shownIdx / ${trainingQueue.size}",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
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
        if (isTrainingMode) {
            val currentSentence = trainingQueue.getOrNull(trainingIndex)
            val trainProgress = if (trainingQueue.isNotEmpty()) {
                (trainingIndex.toFloat() / trainingQueue.size).coerceIn(0f, 1f)
            } else 1f

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
                        progress = { if (currentSentence == null) 1f else trainProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = extended.subtleFill
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (currentSentence == null) {
                        Spacer(modifier = Modifier.weight(0.25f))
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Блок пройдено!",
                                    fontFamily = LoraFontFamily,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ви відпрацювали всі речення ситуації «${group?.titleUk ?: ""}».",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { isTrainingMode = false },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("Повернутися до списку", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = startTraining,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Потренувати ще раз", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(0.75f))
                    } else {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, extended.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clickable {
                                    if (!isAnswerRevealed) {
                                        isAnswerRevealed = true
                                        viewModel.speak(currentSentence.german)
                                    }
                                }
                                .testTag("sentence_trainer_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Скажіть німецькою · ${currentSentence.level}",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = currentSentence.ukrainian,
                                        fontFamily = LoraFontFamily,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 28.sp
                                    )

                                    if (isAnswerRevealed) {
                                        Spacer(modifier = Modifier.height(18.dp))
                                        HorizontalDivider(
                                            modifier = Modifier.width(64.dp),
                                            color = extended.cardBorder
                                        )
                                        Spacer(modifier = Modifier.height(18.dp))
                                        Text(
                                            text = currentSentence.german,
                                            fontFamily = LoraFontFamily,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 27.sp
                                        )
                                        if (currentSentence.noteUk.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = extended.subtleFill,
                                                border = BorderStroke(1.dp, extended.cardBorder)
                                            ) {
                                                Text(
                                                    text = currentSentence.noteUk,
                                                    fontFamily = ManropeFontFamily,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        SpeakerButton(
                                            onSpeak = { slow -> viewModel.speak(currentSentence.german, slow) },
                                            size = 48.dp,
                                            showSlowButton = true
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(22.dp))
                                        Text(
                                            text = "Спробуйте перекласти вголос, а потім перевірте себе",
                                            fontFamily = ManropeFontFamily,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Text(
                                    text = if (isAnswerRevealed) "Оцініть, наскільки легко згадали фразу" else "Натисніть на картку або кнопку нижче",
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isAnswerRevealed) {
                            Button(
                                onClick = {
                                    isAnswerRevealed = true
                                    viewModel.speak(currentSentence.german)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("sentence_reveal_button")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Показати німецькою", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        trainingQueue = trainingQueue + currentSentence
                                        trainingIndex += 1
                                        isAnswerRevealed = false
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, StreakAmber),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StreakAmber),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("sentence_repeat_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ще раз", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (currentSentence.id !in learnedIds) {
                                            viewModel.toggleSentenceLearned(currentSentence.id)
                                        }
                                        trainingIndex += 1
                                        isAnswerRevealed = false
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SuccessGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("sentence_know_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Знаю", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extended.cardBorder),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Прогрес блоку",
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$learnedInGroup / ${sentences.size} вивчено",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (learnedInGroup == sentences.size && sentences.isNotEmpty()) SuccessGreen else MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { groupProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (learnedInGroup == sentences.size && sentences.isNotEmpty()) SuccessGreen else MaterialTheme.colorScheme.primary,
                                trackColor = extended.subtleFill
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = sentences,
                    key = { _, s -> "sent_${s.id}" }
                ) { index, sentence ->
                    ImportantSentenceCard(
                        index = index + 1,
                        sentence = sentence,
                        isLearned = sentence.id in learnedIds,
                        isFavorite = sentence.id in favoriteIds,
                        onSpeak = { slow -> viewModel.speak(sentence.german, slow) },
                        onToggleLearned = { viewModel.toggleSentenceLearned(sentence.id) },
                        onToggleFavorite = { viewModel.toggleSentenceFavorite(sentence.id) },
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ImportantSentenceCard(
    index: Int,
    sentence: ImportantSentence,
    isLearned: Boolean,
    isFavorite: Boolean,
    onSpeak: (slow: Boolean) -> Unit,
    onToggleLearned: () -> Unit,
    onToggleFavorite: () -> Unit,
    showMetaBadges: Boolean = false,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLearned) SuccessGreen.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLearned) SuccessGreen.copy(alpha = 0.35f) else extended.cardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSpeak(false) }
            .testTag("sentence_card_${sentence.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = index.toString().padStart(2, '0'),
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (showMetaBadges) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = sentence.level,
                                fontFamily = MonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = sentence.situationUk,
                            fontFamily = ManropeFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sentence_fav_${sentence.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Видалити з обраного" else "Додати в обране",
                            tint = if (isFavorite) extended.genderDie else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleLearned,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sentence_learned_${sentence.id}")
                    ) {
                        Icon(
                            imageVector = if (isLearned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isLearned) "Позначити як невивчене" else "Позначити як вивчене",
                            tint = if (isLearned) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = sentence.german,
                fontFamily = LoraFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 23.sp,
                modifier = Modifier.padding(end = 6.dp)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = sentence.ukrainian,
                fontFamily = ManropeFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp,
                modifier = Modifier.padding(end = 6.dp)
            )

            if (sentence.noteUk.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = extended.subtleFill,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sentence.noteUk,
                            fontFamily = ManropeFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSpeak(false) }
                        .testTag("sentence_speak_${sentence.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Вимовити речення",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Слухати",
                            fontFamily = ManropeFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = extended.subtleFill,
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSpeak(true) }
                        .testTag("sentence_speak_slow_${sentence.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "0.75x",
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

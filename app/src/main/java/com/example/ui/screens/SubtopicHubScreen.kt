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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TopicCatalog
import com.example.data.model.WordEntity
import com.example.ui.LearningViewModel
import com.example.ui.components.GenderBadge
import com.example.ui.components.SpeakerButton
import com.example.ui.components.TopicPixelIcon
import com.example.ui.components.WordPhotoSurface
import com.example.ui.components.getGenderColor
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtopicHubScreen(
    subtopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val extended = LocalAppExtendedColors.current
    val subtopic = TopicCatalog.findSubtopicById(subtopicId) ?: return
    val parentTopic = TopicCatalog.findMainTopicById(subtopic.mainTopicId)
    val topicColor = parentTopic?.color ?: MaterialTheme.colorScheme.primary

    val words by viewModel.hubWords.collectAsStateWithLifecycle()
    val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val enabledStages by viewModel.enabledStages.collectAsStateWithLifecycle()

    LaunchedEffect(subtopicId, selectedLevel) {
        viewModel.loadHubWords(subtopicId, selectedLevel)
    }

    val progress = remember(allProgress, subtopicId) {
        viewModel.getProgressForSubtopic(subtopicId)
    }
    val completedStageSet = remember(allProgress, subtopicId, selectedLevel) {
        viewModel.getCompletedStageSetForSubtopic(subtopicId)
    }

    val visibleStages = remember(enabledStages) { enabledStages.sorted() }
    val firstEnabledStage = visibleStages.firstOrNull() ?: 1
    val effectiveUnlockedStage = maxOf(progress.unlockedStage, firstEnabledStage)
    val nextStage = visibleStages.firstOrNull { it !in completedStageSet && it <= effectiveUnlockedStage }
        ?: visibleStages.firstOrNull { it >= effectiveUnlockedStage }
        ?: visibleStages.lastOrNull()
        ?: 1
    val completedVisibleCount = visibleStages.count { it in completedStageSet }
    val allVisibleCompleted = visibleStages.isNotEmpty() && completedVisibleCount == visibleStages.size
    val stageFraction = if (visibleStages.isNotEmpty()) {
        (completedVisibleCount.toFloat() / visibleStages.size).coerceIn(0f, 1f)
    } else {
        0f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subtopic.titleDe,
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${subtopic.titleUk} · Рівень $selectedLevel (${words.size} слів)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.size(48.dp).testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (progress.stars > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StreakAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = StreakAmber, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${progress.stars}", fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold, color = StreakAmber, fontSize = 12.sp)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.size(48.dp).testTag("hub_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Налаштування",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, extended.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.startStageByNumber(subtopicId, nextStage) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_continue_journey")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getStageActionTitle(nextStage),
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                TopicPixelIcon(
                                    iconKey = subtopic.iconKey,
                                    accentColor = topicColor,
                                    innerPadding = 7.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subtopic.titleDe,
                                        fontFamily = LoraFontFamily,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${subtopic.titleUk} · ${subtopic.titleEn}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clickable { viewModel.openSettings() }
                                    .testTag("hub_level_badge")
                            ) {
                                Text(
                                    text = "$selectedLevel · ${words.size} слів",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Пройдено етапів: $completedVisibleCount з ${visibleStages.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(stageFraction * 100).toInt()}%",
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (allVisibleCompleted) SuccessGreen else topicColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { stageFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (allVisibleCompleted) SuccessGreen else topicColor,
                            trackColor = topicColor.copy(alpha = 0.14f)
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (visibleStages.size == 8) {
                            "Lernreise · 8 Етапів закріплення"
                        } else {
                            "Lernreise · Активні етапи (${visibleStages.size} з 8)"
                        },
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (visibleStages.size == 8) {
                            "Пройдіть усі 8 етапів на 100%, щоб відкрити інтервальне повторення"
                        } else {
                            "Частину етапів вимкнено · для «Повторення» потрібні всі 8 етапів на 100%"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    visibleStages.forEachIndexed { idx, stageNum ->
                        val isUnlocked = effectiveUnlockedStage >= stageNum
                        val isCompleted = stageNum in completedStageSet
                        val (titleDe, titleUk, icon) = when (stageNum) {
                            1 -> Triple("Lernkarten", "Знайомство з картками та множиною (${words.size} слів)", Icons.Default.ViewCarousel)
                            2 -> Triple("Hören & Verstehen", "Слухання та сприйняття на слух", Icons.Default.Headphones)
                            3 -> Triple("Wortwahl (Quiz)", "Вибір перекладу (4 варіанти)", Icons.Default.Quiz)
                            4 -> Triple("Artikel-Trainer", "Рід іменників (der / die / das)", Icons.Default.Shield)
                            5 -> Triple("Plural-Trainer", "Форми множини іменників (die ...)", Icons.Default.DynamicFeed)
                            6 -> Triple("Lückentext", "Доповнення пропусків у реченнях", Icons.Default.Extension)
                            7 -> Triple("Buchstabieren & Schreiben", "Правопис слова з артиклем (літери або клавіатура)", Icons.Default.Edit)
                            else -> Triple("Gesamttest · Фінальний мікс", "Проходження усіх етапів разом для закріплення", Icons.Default.AutoAwesome)
                        }
                        StageRoadmapNode(
                            stageNumber = stageNum,
                            titleDe = titleDe,
                            titleUk = titleUk,
                            icon = icon,
                            isUnlocked = isUnlocked,
                            isCompleted = isCompleted,
                            onClick = { if (isUnlocked) viewModel.startStageByNumber(subtopicId, stageNum) }
                        )
                        if (idx < visibleStages.lastIndex) {
                            RoadmapConnector(isCompleted = isCompleted)
                        }
                    }
                }
            }
        }
    }

    if (isSettingsOpen) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeSettings() }
        )
    }
}

@Composable
fun SubtopicWordCardItem(
    word: WordEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSpeak: (slow: Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else extended.cardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("subtopic_word_card_${word.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WordPhotoSurface(
                word = word,
                accentColor = getGenderColor(word.article),
                showFallbackIcon = true,
                isThumbnail = true,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (word.article.isNotBlank()) {
                        GenderBadge(article = word.article)
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = word.german,
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (word.transcription.isNotBlank() || word.plural.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            if (word.transcription.isNotBlank()) append(word.transcription)
                            if (word.transcription.isNotBlank() && word.plural.isNotBlank()) append(" · ")
                            if (word.plural.isNotBlank()) append("Pl: ${word.plural}")
                        },
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = word.translationUk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = word.translationEn,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (word.exampleDe.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "„${word.exampleDe}“",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("word_fav_${word.id}")
                ) {
                    Icon(
                        imageVector = if (word.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (word.isFavorite) "Видалити з обраного" else "Додати в обране",
                        tint = if (word.isFavorite) extended.genderDie else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                SpeakerButton(
                    onSpeak = onSpeak,
                    size = 44.dp,
                    showSlowButton = false
                )
            }
        }
    }
}

@Composable
fun StageRoadmapNode(
    stageNumber: Int,
    titleDe: String,
    titleUk: String,
    icon: ImageVector,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val (nodeBgColor, iconColor) = when {
        isCompleted -> SuccessGreen to Color.White
        isUnlocked -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        else -> extended.subtleFill to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            width = if (isUnlocked && !isCompleted) 1.5.dp else 1.dp,
            color = if (isUnlocked && !isCompleted) MaterialTheme.colorScheme.primary else extended.cardBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("stage_node_$stageNumber")
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(nodeBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = "Пройдено", tint = Color.White, modifier = Modifier.size(22.dp))
                } else if (!isUnlocked) {
                    Icon(Icons.Default.Lock, contentDescription = "Заблоковано", tint = iconColor, modifier = Modifier.size(18.dp))
                } else {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Етап $stageNumber: $titleDe",
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Star, contentDescription = null, tint = StreakAmber, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = titleUk,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isUnlocked && !isCompleted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Вчити", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RoadmapConnector(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 35.dp)
                .width(2.dp)
                .height(12.dp)
                .background(if (isCompleted) SuccessGreen else LocalAppExtendedColors.current.cardBorder)
        )
    }
}

fun getStageActionTitle(stage: Int): String = when (stage) {
    1 -> "Почати Етап 1: Картки слів"
    2 -> "Перейти до Етапу 2: Слухання"
    3 -> "Перейти до Етапу 3: Тест"
    4 -> "Перейти до Етапу 4: Тренажер родів"
    5 -> "Перейти до Етапу 5: Множина"
    6 -> "Перейти до Етапу 6: Пропуски в реченні"
    7 -> "Перейти до Етапу 7: Правопис з артиклем"
    8 -> "Перейти до Етапу 8: Фінальний мікс"
    else -> "Почати навчання"
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Subtopic
import com.example.data.model.TopicCatalog
import com.example.ui.LearningViewModel
import com.example.ui.Screen
import com.example.ui.components.TopicPixelIcon
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtopicsListScreen(
    mainTopicId: String,
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val extended = LocalAppExtendedColors.current
    val mainTopic = TopicCatalog.findMainTopicById(mainTopicId) ?: return
    val subtopicLevelCounts by viewModel.subtopicLevelCounts.collectAsStateWithLifecycle()
    val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()

    val countMap = remember(subtopicLevelCounts, selectedLevel) {
        val exact = subtopicLevelCounts
            .filter { it.level.equals(selectedLevel, ignoreCase = true) }
            .associate { it.subtopicId to minOf(it.cnt, 15) }
        val totals = subtopicLevelCounts.groupBy { it.subtopicId }
            .mapValues { (_, list) -> minOf(list.sumOf { it.cnt }, 12) }
        exact to totals
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = mainTopic.titleDe,
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${mainTopic.titleUk} · Рівень $selectedLevel",
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
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.size(48.dp).testTag("subtopics_settings_button")
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
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 10.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Підтеми розділу · ${mainTopic.subtopics.size}",
                                fontFamily = LoraFontFamily,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Оберіть тему для карток та 8 етапів",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = mainTopic.color.copy(alpha = 0.14f),
                            modifier = Modifier.clickable { viewModel.openSettings() }
                        ) {
                            Text(
                                text = "Рівень $selectedLevel",
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainTopic.color,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            items(mainTopic.subtopics, key = { it.id }) { subtopic ->
                val progress = allProgress.firstOrNull { it.subtopicId == subtopic.id }
                val completedStages = progress?.completedStages ?: 0
                val stars = progress?.stars ?: 0
                val wordsCount = countMap.first[subtopic.id] ?: countMap.second[subtopic.id] ?: 12

                SubtopicRowCard(
                    subtopic = subtopic,
                    topicColor = mainTopic.color,
                    completedStages = completedStages,
                    stars = stars,
                    wordsCount = wordsCount,
                    selectedLevel = selectedLevel,
                    onClick = {
                        viewModel.navigateTo(Screen.SubtopicHub(subtopic.id))
                    },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
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
fun SubtopicRowCard(
    subtopic: Subtopic,
    topicColor: Color,
    completedStages: Int,
    stars: Int,
    wordsCount: Int = 12,
    selectedLevel: String = "A1",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("subtopic_card_${subtopic.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (completedStages >= 8) SuccessGreen else topicColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopicPixelIcon(
                        iconKey = subtopic.iconKey,
                        accentColor = if (completedStages >= 8) SuccessGreen else topicColor,
                        innerPadding = 6.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = subtopic.titleDe,
                                fontFamily = LoraFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = topicColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$selectedLevel · $wordsCount слів",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = topicColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${subtopic.titleUk} · ${subtopic.titleEn}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Приклади: ${subtopic.description}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..8) {
                            val isDone = i <= completedStages
                            Box(
                                modifier = Modifier
                                    .padding(end = 3.dp)
                                    .size(width = 14.dp, height = 5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isDone) {
                                            if (completedStages >= 8) SuccessGreen else topicColor
                                        } else {
                                            extended.cardBorder
                                        }
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${completedStages.coerceAtMost(8)} / 8",
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (completedStages >= 8) SuccessGreen else topicColor
                        )
                    }

                    if (stars > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StreakAmber.copy(alpha = 0.14f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = StreakAmber, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$stars",
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StreakAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

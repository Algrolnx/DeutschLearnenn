package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MainTopic
import com.example.data.model.TopicCatalog
import com.example.ui.LearningViewModel
import com.example.ui.MainTab
import com.example.ui.Screen
import com.example.ui.components.TopicPixelIcon
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopicsScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current

    val totalWordCount by viewModel.totalWordCount.collectAsStateWithLifecycle()
    val favoriteWords by viewModel.favoriteWords.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val allProgress by viewModel.allProgress.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedMainTab.collectAsStateWithLifecycle()
    val reviewDashboard by viewModel.reviewDashboard.collectAsStateWithLifecycle()

    val showFavoritesOnly = selectedTab == MainTab.FAVORITES

    val progressByTopic = remember(allProgress) {
        val progressBySubtopic = allProgress.associateBy { it.subtopicId }
        TopicCatalog.mainTopics.associate { topic ->
            val subtopicsCount = topic.subtopics.size
            val completedStages = topic.subtopics.sumOf { sub ->
                progressBySubtopic[sub.id]?.completedStages ?: 0
            }
            val totalPossibleStages = subtopicsCount * 6
            val ratio = if (totalPossibleStages > 0) completedStages.toFloat() / totalPossibleStages else 0f
            topic.id to ratio
        }
    }
    val isSearching = searchQuery.trim().length >= 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DeutschLern",
                            fontFamily = LoraFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Німецька щодня · $totalWordCount слів",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(StreakAmber.copy(alpha = 0.14f))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Серія днів",
                            tint = StreakAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streakDays",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = StreakAmber,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Progress) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Статистика",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_button")
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == MainTab.TOPICS,
                    onClick = { viewModel.selectMainTab(MainTab.TOPICS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Теми"
                        )
                    },
                    label = { Text("Теми", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_topics")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.REVIEW,
                    onClick = { viewModel.selectMainTab(MainTab.REVIEW) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (reviewDashboard.availableTodayCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = if (reviewDashboard.availableTodayCount > 99) "99+" else "${reviewDashboard.availableTodayCount}",
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Повторення"
                            )
                        }
                    },
                    label = { Text("Повторення", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_review")
                )

                NavigationBarItem(
                    selected = selectedTab == MainTab.FAVORITES,
                    onClick = { viewModel.selectMainTab(MainTab.FAVORITES) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.FAVORITES) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Обране"
                        )
                    },
                    label = { Text("Обране", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = extended.genderDie,
                        selectedTextColor = extended.genderDie,
                        indicatorColor = extended.genderDieBg
                    ),
                    modifier = Modifier.testTag("nav_tab_favorites")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (selectedTab == MainTab.REVIEW) {
            ReviewTabContent(
                viewModel = viewModel,
                contentPadding = innerPadding
            )
        } else {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Instant Search Bar across all 6,000+ words (SQL indexed)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Пошук серед $totalWordCount слів (DE / UA / EN)...",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Пошук",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистити",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = extended.cardBorder
                    ),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .testTag("search_words_input")
                )
            }

            // Section Header + Level Pill + Topics / Favorites Filter
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isSearching -> "Знайдено слів (${searchResults.size})"
                                    showFavoritesOnly -> "Обрані слова (${favoriteWords.size})"
                                    else -> "Програма за темами"
                                },
                                fontFamily = LoraFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    isSearching -> "Оберіть слово, щоб відкрити його тему"
                                    showFavoritesOnly -> "Збережені картки для швидкого повторення"
                                    else -> "16 тематичних розділів один за одним"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clickable { viewModel.openSettings() }
                                .testTag("level_settings_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Рівень $selectedLevel",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            when {
                isSearching -> {
                    if (searchResults.isEmpty()) {
                        item {
                            EmptyWordsPlaceholderCard(
                                title = "Нічого не знайдено",
                                subtitle = "Спробуйте інший запит німецькою, українською або англійською",
                                modifier = Modifier.widthIn(max = 600.dp)
                            )
                        }
                    } else {
                        items(
                            items = searchResults,
                            key = { "search_${it.id}_${it.german}" }
                        ) { word ->
                            SubtopicWordCardItem(
                                word = word,
                                isSelected = false,
                                onClick = {
                                    viewModel.speak(word.displayWithArticle)
                                    viewModel.navigateTo(Screen.SubtopicHub(word.subtopicId))
                                },
                                onSpeak = { slow -> viewModel.speak(word.displayWithArticle, slow) },
                                onToggleFavorite = { viewModel.toggleFavorite(word) },
                                modifier = Modifier.widthIn(max = 600.dp)
                            )
                        }
                    }
                }

                showFavoritesOnly -> {
                    if (favoriteWords.isEmpty()) {
                        item {
                            EmptyWordsPlaceholderCard(
                                title = "Ще немає обраних слів",
                                subtitle = "Натисніть іконку серця на картці слова, щоб зберегти його сюди",
                                modifier = Modifier.widthIn(max = 600.dp)
                            )
                        }
                    } else {
                        items(
                            items = favoriteWords,
                            key = { "fav_${it.id}_${it.german}" }
                        ) { word ->
                            SubtopicWordCardItem(
                                word = word,
                                isSelected = false,
                                onClick = {
                                    viewModel.speak(word.displayWithArticle)
                                    viewModel.navigateTo(Screen.SubtopicHub(word.subtopicId))
                                },
                                onSpeak = { slow -> viewModel.speak(word.displayWithArticle, slow) },
                                onToggleFavorite = { viewModel.toggleFavorite(word) },
                                modifier = Modifier.widthIn(max = 600.dp)
                            )
                        }
                    }
                }

                else -> {
                    itemsIndexed(
                        items = TopicCatalog.mainTopics,
                        key = { _, topic -> topic.id }
                    ) { index, mainTopic ->
                        val subtopicsCount = mainTopic.subtopics.size
                        val progress = progressByTopic[mainTopic.id] ?: 0f

                        EditorialTopicRowCard(
                            index = index + 1,
                            mainTopic = mainTopic,
                            subtopicsCount = subtopicsCount,
                            progress = progress,
                            onClick = {
                                viewModel.navigateTo(Screen.SubtopicsList(mainTopic.id))
                            },
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
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
private fun EmptyWordsPlaceholderCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Flowing Sequential Editorial Row for Main Topics (replaces blocky 2-column dashboard tiles).
 */
@Composable
fun EditorialTopicRowCard(
    index: Int,
    mainTopic: MainTopic,
    subtopicsCount: Int,
    progress: Float,
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
            .testTag("main_topic_${mainTopic.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Crisp Canvas Pixel-Art Sprite icon for each theme
            TopicPixelIcon(
                iconKey = mainTopic.iconKey,
                accentColor = mainTopic.color,
                innerPadding = 8.dp,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = index.toString().padStart(2, '0'),
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainTopic.color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mainTopic.titleDe,
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "$subtopicsCount підтем",
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = mainTopic.titleUk,
                    fontFamily = ManropeFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = mainTopic.color,
                        trackColor = mainTopic.color.copy(alpha = 0.14f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainTopic.color
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

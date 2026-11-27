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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MainTopic
import com.example.data.model.SentenceSituationGroup
import com.example.data.model.TopicCatalog
import com.example.data.repository.VocabularyRepository
import com.example.ui.LearningViewModel
import com.example.ui.MainTab
import com.example.ui.Screen
import com.example.ui.TopicsContentMode
import com.example.ui.components.TopicPixelIcon
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

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
    val topicsContentMode by viewModel.topicsContentMode.collectAsStateWithLifecycle()
    val learnedSentenceIds by viewModel.learnedSentenceIds.collectAsStateWithLifecycle()
    val favoriteSentenceIds by viewModel.favoriteSentenceIds.collectAsStateWithLifecycle()
    val enabledStages by viewModel.enabledStages.collectAsStateWithLifecycle()

    val showFavoritesOnly = selectedTab == MainTab.FAVORITES
    val isSentencesMode = !showFavoritesOnly && topicsContentMode == TopicsContentMode.SENTENCES

    val levelSituationGroups = remember(selectedLevel) {
        viewModel.getSituationGroupsForLevel(selectedLevel)
    }
    val levelSentencesCount = remember(levelSituationGroups) {
        levelSituationGroups.sumOf { it.sentences.size }
    }
    val levelLearnedSentencesCount = remember(levelSituationGroups, learnedSentenceIds) {
        levelSituationGroups.sumOf { group -> group.sentences.count { it.id in learnedSentenceIds } }
    }
    val sentenceSearchResults = remember(searchQuery, selectedLevel, isSentencesMode) {
        if (isSentencesMode && searchQuery.trim().length >= 2) {
            viewModel.searchSentences(searchQuery, selectedLevel)
        } else {
            emptyList()
        }
    }

    val progressByTopic = remember(allProgress, selectedLevel, enabledStages) {
        val activeStages = enabledStages.ifEmpty { VocabularyRepository.ALL_STAGES }
        val stagesPerSubtopic = activeStages.size.coerceAtLeast(1)
        TopicCatalog.mainTopics.associate { topic ->
            val subtopicsCount = topic.subtopics.size
            val completedStages = topic.subtopics.sumOf { sub ->
                viewModel.getCompletedStageSetForSubtopic(sub.id).count { it in activeStages }
            }
            val totalPossibleStages = subtopicsCount * stagesPerSubtopic
            val ratio = if (totalPossibleStages > 0) (completedStages.toFloat() / totalPossibleStages).coerceIn(0f, 1f) else 0f
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
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
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
                    selected = selectedTab == MainTab.GRAMMAR,
                    onClick = { viewModel.selectMainTab(MainTab.GRAMMAR) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Граматика"
                        )
                    },
                    label = { Text("Граматика", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_grammar")
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
                    label = { Text("Обране", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
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
        } else if (selectedTab == MainTab.GRAMMAR) {
            GrammarGuideContent(
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
            // Instant Search Bar (words or essential sentences depending on mode)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = if (isSentencesMode) {
                                "Пошук серед 600 важливих речень (DE / UA)..."
                            } else {
                                "Пошук серед $totalWordCount слів (DE / UA / EN)..."
                            },
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

            // 2-Button Segmented Switcher inside Теми: Слова (default) | Речення
            if (!showFavoritesOnly) {
                item {
                    TopicsContentModeSwitcher(
                        mode = topicsContentMode,
                        level = selectedLevel,
                        onSelectMode = { viewModel.setTopicsContentMode(it) },
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                    )
                }
            }

            // Section Header + Level Pill
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
                                    isSearching && isSentencesMode -> "Знайдено речень (${sentenceSearchResults.size})"
                                    isSearching -> "Знайдено слів (${searchResults.size})"
                                    showFavoritesOnly -> "Обрані слова (${favoriteWords.size})"
                                    isSentencesMode -> "Важливі речення · $selectedLevel"
                                    else -> "Програма за темами"
                                },
                                fontFamily = LoraFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    isSearching && isSentencesMode -> "Натисніть на речення, щоб прослухати вимову"
                                    isSearching -> "Оберіть слово, щоб відкрити його тему"
                                    showFavoritesOnly -> "Збережені картки для швидкого повторення"
                                    isSentencesMode -> "$levelSentencesCount практичних фраз у ${levelSituationGroups.size} життєвих ситуаціях"
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
                isSearching && isSentencesMode -> {
                    if (sentenceSearchResults.isEmpty()) {
                        item {
                            EmptyWordsPlaceholderCard(
                                title = "Речень не знайдено",
                                subtitle = "Спробуйте інше слово німецькою або українською (наприклад: Termin, лікар, оренда)",
                                modifier = Modifier.widthIn(max = 600.dp)
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = sentenceSearchResults,
                            key = { _, s -> "search_sent_${s.id}" }
                        ) { idx, sentence ->
                            ImportantSentenceCard(
                                index = idx + 1,
                                sentence = sentence,
                                isLearned = sentence.id in learnedSentenceIds,
                                isFavorite = sentence.id in favoriteSentenceIds,
                                onSpeak = { slow -> viewModel.speak(sentence.german, slow) },
                                onToggleLearned = { viewModel.toggleSentenceLearned(sentence.id) },
                                onToggleFavorite = { viewModel.toggleSentenceFavorite(sentence.id) },
                                showMetaBadges = true,
                                modifier = Modifier.widthIn(max = 600.dp)
                            )
                        }
                    }
                }

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

                isSentencesMode -> {
                    item {
                        val levelRatio = if (levelSentencesCount > 0) {
                            (levelLearnedSentencesCount.toFloat() / levelSentencesCount).coerceIn(0f, 1f)
                        } else 0f
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
                                        text = "Вивчено на рівні $selectedLevel",
                                        fontFamily = ManropeFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$levelLearnedSentencesCount / $levelSentencesCount",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (levelLearnedSentencesCount == levelSentencesCount && levelSentencesCount > 0) SuccessGreen else MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { levelRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (levelLearnedSentencesCount == levelSentencesCount && levelSentencesCount > 0) SuccessGreen else MaterialTheme.colorScheme.primary,
                                    trackColor = extended.subtleFill
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = levelSituationGroups,
                        key = { _, group -> "${group.level}_${group.situationId}" }
                    ) { idx, group ->
                        val learnedInGroup = group.sentences.count { it.id in learnedSentenceIds }
                        EditorialSentenceSituationCard(
                            index = idx + 1,
                            group = group,
                            learnedCount = learnedInGroup,
                            onClick = {
                                viewModel.navigateTo(Screen.SentenceSituation(group.level, group.situationId))
                            },
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
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

@Composable
private fun TopicsContentModeSwitcher(
    mode: TopicsContentMode,
    level: String,
    onSelectMode: (TopicsContentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
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
            val wordsSelected = mode == TopicsContentMode.WORDS
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (wordsSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (wordsSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(TopicsContentMode.WORDS) }
                    .testTag("topics_mode_words")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = if (wordsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Слова",
                        fontFamily = ManropeFontFamily,
                        fontSize = 14.sp,
                        fontWeight = if (wordsSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (wordsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val sentencesSelected = mode == TopicsContentMode.SENTENCES
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (sentencesSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shadowElevation = if (sentencesSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(TopicsContentMode.SENTENCES) }
                    .testTag("topics_mode_sentences")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = if (sentencesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Речення · $level",
                        fontFamily = ManropeFontFamily,
                        fontSize = 14.sp,
                        fontWeight = if (sentencesSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (sentencesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialSentenceSituationCard(
    index: Int,
    group: SentenceSituationGroup,
    learnedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val total = group.sentences.size.coerceAtLeast(1)
    val progress = (learnedCount.toFloat() / total).coerceIn(0f, 1f)
    val isDone = learnedCount >= total
    val accent = if (isDone) SuccessGreen else MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, if (isDone) SuccessGreen.copy(alpha = 0.4f) else extended.cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("sentence_situation_${group.situationId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString().padStart(2, '0'),
                    fontFamily = MonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.titleDe,
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${group.sentences.size} речень",
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = group.titleUk,
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
                        color = accent,
                        trackColor = accent.copy(alpha = 0.14f)
                    )
                    Text(
                        text = "$learnedCount/$total",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
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

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LearningViewModel
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen
import com.example.util.GrammarAccentKind
import com.example.util.GrammarGuideCatalog
import com.example.util.GrammarRuleCardData
import com.example.util.GrammarTopicSection
import com.example.util.WordGrammarHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarGuideScreen(
    viewModel: LearningViewModel,
    initialTopicId: String? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { viewModel.navigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Довідник граматики",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Deutsche Grammatik · Чому так пишеться?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("grammar_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        GrammarGuideContent(
            viewModel = viewModel,
            contentPadding = innerPadding,
            initialTopicId = initialTopicId
        )
    }
}

@Composable
fun GrammarGuideContent(
    viewModel: LearningViewModel,
    contentPadding: PaddingValues,
    initialTopicId: String? = null,
    modifier: Modifier = Modifier
) {
    val topics = GrammarGuideCatalog.topics
    val validInitial = initialTopicId?.takeIf { id -> topics.any { it.id == id } }
    var selectedTopicId by rememberSaveable(validInitial) { mutableStateOf<String?>(validInitial) }
    val activeTopic = selectedTopicId?.let { id -> topics.firstOrNull { it.id == id } }

    // Intercept system back button when viewing a specific topic inside the Grammar tab menu
    BackHandler(enabled = activeTopic != null && validInitial == null) {
        selectedTopicId = null
    }

    AnimatedContent(
        targetState = activeTopic,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "grammarMenuDetailTransition",
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { topic ->
        if (topic == null) {
            GrammarTopicMenuList(
                topics = topics,
                contentPadding = contentPadding,
                onSelectTopic = { selectedTopicId = it.id }
            )
        } else {
            val currentIndex = topics.indexOfFirst { it.id == topic.id }.coerceAtLeast(0)
            val nextTopic = topics.getOrNull((currentIndex + 1) % topics.size)
                ?.takeIf { it.id != topic.id }

            GrammarTopicDetailView(
                topic = topic,
                topicIndex = currentIndex,
                totalTopics = topics.size,
                nextTopic = nextTopic,
                contentPadding = contentPadding,
                onBackToMenu = { selectedTopicId = null },
                onSelectNextTopic = { next -> selectedTopicId = next.id },
                onSpeak = { text -> viewModel.speak(text) }
            )
        }
    }
}

@Composable
private fun GrammarTopicMenuList(
    topics: List<GrammarTopicSection>,
    contentPadding: PaddingValues,
    onSelectTopic: (GrammarTopicSection) -> Unit
) {
    val extended = LocalAppExtendedColors.current
    val totalRules = topics.sumOf { it.rules.size }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .testTag("grammar_menu_list")
    ) {
        // Menu Header Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Меню граматики",
                            fontFamily = LoraFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Оберіть тему для пояснень із живими озвученими прикладами",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = extended.subtleFill
                        ) {
                            Text(
                                text = "${topics.size} тем · $totalRules правил · A1–B2",
                                fontFamily = MonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Menu Items
        itemsIndexed(
            items = topics,
            key = { _, topic -> topic.id }
        ) { index, topic ->
            GrammarTopicMenuCard(
                index = index + 1,
                topic = topic,
                onClick = { onSelectTopic(topic) },
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GrammarTopicMenuCard(
    index: Int,
    topic: GrammarTopicSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val accentColor = resolveGrammarAccentColor(topic.accentKind)
    val indexLabel = index.toString().padStart(2, '0')
    val rulesLabel = formatRulesCountUk(topic.rules.size)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("grammar_menu_item_${topic.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = indexLabel,
                    fontFamily = MonoFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = topic.titleUk,
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = topic.levelBadge,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = topic.titleDe,
                    fontFamily = MonoFontFamily,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = topic.subtitleUk,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = rulesLabel,
                    fontFamily = MonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Відкрити тему ${topic.titleUk}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun GrammarTopicDetailView(
    topic: GrammarTopicSection,
    topicIndex: Int,
    totalTopics: Int,
    nextTopic: GrammarTopicSection?,
    contentPadding: PaddingValues,
    onBackToMenu: () -> Unit,
    onSelectNextTopic: (GrammarTopicSection) -> Unit,
    onSpeak: (String) -> Unit
) {
    val extended = LocalAppExtendedColors.current
    val accentColor = resolveGrammarAccentColor(topic.accentKind)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 10.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .testTag("grammar_guide_list")
    ) {
        // Back-to-Menu Navigation Bar
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onBackToMenu)
                        .testTag("grammar_back_to_menu_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад до меню граматики",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Меню граматики",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "Тема ${topicIndex + 1} з $totalTopics · ${topic.levelBadge}",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Intro Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f)),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topic.titleUk,
                                fontFamily = LoraFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = topic.titleDe,
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = topic.introUk,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Grammar Rule Cards
        items(
            items = topic.rules,
            key = { "${topic.id}_${it.badge}_${it.title}" }
        ) { rule ->
            GrammarRuleCard(
                rule = rule,
                onSpeak = onSpeak,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            )
        }

        // Footer Navigation (Back to Menu / Next Topic)
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToMenu,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "До меню тем",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                if (nextTopic != null) {
                    Button(
                        onClick = { onSelectNextTopic(nextTopic) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .height(48.dp)
                            .testTag("grammar_next_topic_button")
                    ) {
                        Text(
                            text = nextTopic.shortTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Наступна тема",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun resolveGrammarAccentColor(kind: GrammarAccentKind): Color {
    val extended = LocalAppExtendedColors.current
    return when (kind) {
        GrammarAccentKind.PRIMARY -> MaterialTheme.colorScheme.primary
        GrammarAccentKind.DER -> extended.genderDer
        GrammarAccentKind.DIE -> extended.genderDie
        GrammarAccentKind.DAS -> extended.genderDas
        GrammarAccentKind.AMBER -> StreakAmber
        GrammarAccentKind.SUCCESS -> SuccessGreen
    }
}

private fun formatRulesCountUk(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "$count правило · з озвученням"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "$count правила · з озвученням"
    else -> "$count правил · з озвученням"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrammarRuleCard(
    rule: GrammarRuleCardData,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val accentColor = resolveGrammarAccentColor(rule.accentKind)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = rule.badge,
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = rule.title,
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rule.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )

            if (rule.markers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rule.markers.forEach { marker ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = extended.subtleFill
                        ) {
                            Text(
                                text = marker,
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (rule.examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rule.examples.forEach { ex ->
                        // Speak the full German example (for "A → B", speak "A, B")
                        val spokenText = ex.german.replace("→", ",")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(extended.subtleFill)
                                .clickable { onSpeak(spokenText) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Прослухати приклад",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ex.german,
                                    fontFamily = LoraFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = ex.translationUk,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = ex.highlight,
                                    fontFamily = MonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
 * Compact inline "Why this ending/article?" explanation card shown right after answering
 * in Stage 4 (Articles), Stage 5 (Plural), Stage 8 (Final Mix), and Spaced Repetition.
 */
@Composable
fun InlineGrammarHintCard(
    hint: WordGrammarHint,
    onOpenGrammar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = extended.warningYellowBg,
        border = BorderStroke(1.dp, extended.warningYellow.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("inline_grammar_hint_card")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = extended.warningYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = hint.ruleTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = extended.warningYellow.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = hint.badge,
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = extended.warningYellow,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint.explanation,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenGrammar(hint.topicId) }
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hint.topicId == "plural") "Усі правила множини" else "Усі правила родів",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

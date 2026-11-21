package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.theme.LeitnerBox1
import com.example.ui.theme.LeitnerBox2
import com.example.ui.theme.LeitnerBox3
import com.example.ui.theme.LeitnerBox4
import com.example.ui.theme.LeitnerBox5
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val extended = LocalAppExtendedColors.current
    val totalWords by viewModel.totalWordCount.collectAsStateWithLifecycle()
    val boxCountsList by viewModel.boxCounts.collectAsStateWithLifecycle()
    val levelStatsList by viewModel.levelStats.collectAsStateWithLifecycle()
    val favoriteWords by viewModel.favoriteWords.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()

    val boxMap = remember(boxCountsList) { boxCountsList.associate { it.box to it.cnt } }
    val box1Count = boxMap[1] ?: 0
    val box2Count = boxMap[2] ?: 0
    val box3Count = boxMap[3] ?: 0
    val box4Count = boxMap[4] ?: 0
    val box5Count = boxMap[5] ?: 0

    val masteredCount = box4Count + box5Count
    val favoritesCount = favoriteWords.size
    val masteryPercentage = if (totalWords > 0) ((masteredCount.toFloat() / totalWords) * 100).toInt() else 0

    val levelMap = remember(levelStatsList) { levelStatsList.associateBy { it.level.uppercase() } }
    val a1 = levelMap["A1"]
    val a2 = levelMap["A2"]
    val b1 = levelMap["B1"]
    val b2 = levelMap["B2"]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistik & Fortschritt · Прогрес",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.size(48.dp).testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tactile Bookplate Mastery Summary Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = extended.heroBannerBg),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ЗАГАЛЬНЕ ЗАСВОЄННЯ",
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.heroBannerContent.copy(alpha = 0.72f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$masteryPercentage%",
                                fontFamily = LoraFontFamily,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.heroBannerContent
                            )
                            Text(
                                text = "$masteredCount з $totalWords слів закріплено в пам'яті",
                                fontSize = 12.sp,
                                color = extended.heroBannerContent.copy(alpha = 0.8f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(extended.heroBannerContent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = extended.heroBannerContent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Metrics Row
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Слова",
                        subtitle = "У базі",
                        value = "$totalWords",
                        icon = Icons.Default.School,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Обране",
                        subtitle = "Збережені",
                        value = "$favoritesCount",
                        icon = Icons.Default.Favorite,
                        color = extended.genderDie,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Серія",
                        subtitle = "Днів поспіль",
                        value = "$streakDays",
                        icon = Icons.Default.LocalFireDepartment,
                        color = StreakAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Leitner 5-Box Distribution
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Leitner-Boxen · Інтервальна пам'ять",
                                fontFamily = LoraFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Розподіл словникового запасу за коробками Лейтнера",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LeitnerBoxProgressRow(boxName = "Box 1: Нові слова", count = box1Count, total = totalWords, color = LeitnerBox1)
                        Spacer(modifier = Modifier.height(10.dp))
                        LeitnerBoxProgressRow(boxName = "Box 2: Вивчаються (+3 дні)", count = box2Count, total = totalWords, color = LeitnerBox2)
                        Spacer(modifier = Modifier.height(10.dp))
                        LeitnerBoxProgressRow(boxName = "Box 3: Повторюються (+7 днів)", count = box3Count, total = totalWords, color = LeitnerBox3)
                        Spacer(modifier = Modifier.height(10.dp))
                        LeitnerBoxProgressRow(boxName = "Box 4: Закріплені (+14 днів)", count = box4Count, total = totalWords, color = LeitnerBox4)
                        Spacer(modifier = Modifier.height(10.dp))
                        LeitnerBoxProgressRow(boxName = "Box 5: Засвоєні назавжди (+30 днів)", count = box5Count, total = totalWords, color = LeitnerBox5)
                    }
                }
            }

            // CEFR Levels Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "GER-Niveaus · Рівні мови (CEFR)",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        LevelMasteryRow(
                            level = "A1 - Початковий",
                            count = a1?.totalCount ?: 0,
                            mastered = a1?.masteredCount ?: 0,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LevelMasteryRow(
                            level = "A2 - Базовий",
                            count = a2?.totalCount ?: 0,
                            mastered = a2?.masteredCount ?: 0,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LevelMasteryRow(
                            level = "B1 - Середній",
                            count = b1?.totalCount ?: 0,
                            mastered = b1?.masteredCount ?: 0,
                            color = Color(0xFF6B5B95)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LevelMasteryRow(
                            level = "B2 - Поглиблений",
                            count = b2?.totalCount ?: 0,
                            mastered = b2?.masteredCount ?: 0,
                            color = StreakAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LeitnerBoxProgressRow(
    boxName: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = boxName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "$count",
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun LevelMasteryRow(
    level: String,
    count: Int,
    mastered: Int,
    color: Color
) {
    val progress = if (count > 0) mastered.toFloat() / count else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = level, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "$mastered / $count",
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.14f)
        )
    }
}

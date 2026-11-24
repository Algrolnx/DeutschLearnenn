package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.MainTab
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen
import com.example.util.SpacedRepetition

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewTabContent(
    viewModel: LearningViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    val dashboard by viewModel.reviewDashboard.collectAsStateWithLifecycle()
    val enabledStages by viewModel.enabledStages.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshReviewDashboard()
    }

    val dailyProgress = if (dashboard.dailyLimit > 0) {
        (dashboard.reviewsDoneToday.toFloat() / dashboard.dailyLimit).coerceIn(0f, 1f)
    } else {
        0f
    }
    val dailyGoalReached = dashboard.remainingDailyQuota <= 0
    val nextRoundCount = minOf(LearningViewModel.MIXED_ROUND_SIZE, dashboard.availableTodayCount)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("review_tab_content")
    ) {
        // Editorial Header
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Повторення та пам'ять",
                    fontFamily = LoraFontFamily,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Інтервальний мікс усіх етапів без перевантаження",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero Action Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (dashboard.availableTodayCount > 0 && !dailyGoalReached) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    } else {
                        extended.cardBorder
                    }
                ),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            dailyGoalReached -> SuccessGreen.copy(alpha = 0.14f)
                                            dashboard.availableTodayCount > 0 -> MaterialTheme.colorScheme.primaryContainer
                                            else -> extended.subtleFill
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        dailyGoalReached -> Icons.Default.CheckCircle
                                        dashboard.availableTodayCount > 0 -> Icons.Default.Shuffle
                                        else -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        dailyGoalReached -> SuccessGreen
                                        dashboard.availableTodayCount > 0 -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = when {
                                        dashboard.enrolledCount == 0 -> "Ще немає слів у повторенні"
                                        dailyGoalReached -> "Денну норму виконано!"
                                        dashboard.availableTodayCount > 0 -> "Готово до повторення: ${dashboard.availableTodayCount}"
                                        else -> "На зараз усе повторено"
                                    },
                                    fontFamily = LoraFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when {
                                        dashboard.enrolledCount == 0 -> "Пройдіть усі 8 етапів першої підтеми на 100% — слова з'являться тут через 6 годин"
                                        dailyGoalReached -> "Мозку потрібен час на закріплення. Нова порція завтра!"
                                        dashboard.availableTodayCount > 0 -> "Короткий раунд до $nextRoundCount слів у перемішку з різними етапами"
                                        dashboard.nextReviewTimestamp != null -> "Наступна порція: ${SpacedRepetition.formatTimeUntilUk(dashboard.nextReviewTimestamp!!)}"
                                        else -> "Усі слова чекають свого інтервалу"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calm Daily Cap Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Сьогодні повторено",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dashboard.reviewsDoneToday} / ${dashboard.dailyLimit} слів",
                            fontFamily = MonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dailyGoalReached) SuccessGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (dailyGoalReached) SuccessGreen else MaterialTheme.colorScheme.primary,
                        trackColor = extended.subtleFill
                    )

                    if (dashboard.deferredToTomorrowCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StreakAmber.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Щоб не перевантажувати пам'ять, ще ${dashboard.deferredToTomorrowCount} слів спокійно перенесено на наступні дні.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        dashboard.enrolledCount == 0 -> {
                            Button(
                                onClick = { viewModel.selectMainTab(MainTab.TOPICS) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("review_go_to_topics_button")
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Обрати тему для навчання",
                                    fontFamily = LoraFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        !dailyGoalReached && dashboard.availableTodayCount > 0 -> {
                            Button(
                                onClick = { viewModel.startMixedReviewSession(earlyPractice = false) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_mixed_review_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Почати повторення ($nextRoundCount слів)",
                                    fontFamily = LoraFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        !dailyGoalReached && dashboard.enrolledCount > 0 -> {
                            OutlinedButton(
                                onClick = { viewModel.startMixedReviewSession(earlyPractice = true) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("start_early_review_button")
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Додаткове тренування (до 10 слів)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        else -> {
                            OutlinedButton(
                                onClick = { viewModel.openSettings() },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, extended.cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("review_adjust_limit_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Змінити денний ліміт (${dashboard.dailyLimit}/день)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compact 3-Metric Strip
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                ReviewMetricTile(
                    value = "${dashboard.enrolledCount}",
                    label = "У циклі",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ReviewMetricTile(
                    value = "${dashboard.mastered16Count}",
                    label = "Закріплено\n(16/16)",
                    accent = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                ReviewMetricTile(
                    value = "${dashboard.dailyLimit}",
                    label = "Ліміт / день",
                    accent = StreakAmber,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.openSettings() }
                )
            }
        }

        // Active Mixed Stages Strip
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Етапи в міксі (${enabledStages.size} з 8)",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Налаштувати",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.openSettings() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("review_configure_stages_link")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..8).forEach { stage ->
                            val isActive = stage in enabledStages
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else extended.subtleFill,
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else extended.cardBorder
                                )
                            ) {
                                Text(
                                    text = shortStageNameUk(stage),
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 16-Repetition Spaced Cycle Explainer
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, extended.cardBorder),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Цикл запам'ятовування (16 повторень)",
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Перше повторення приходить через 6 годин у той самий день, далі через 1 день, 2 дні, 4 дні, 6, 9, 12, 16, 21, 28, 35, 45, 60, 75, 90 та 120 днів. Помилка лише м'яко повертає слово на 2 кроки назад без втрати всього прогресу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("6 год", "1 д", "2 д", "4 д", "9 д", "28 д", "120 д").forEach { stepLabel ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = extended.subtleFill,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stepLabel,
                                    fontFamily = MonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
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
private fun ReviewMetricTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val extended = LocalAppExtendedColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

fun shortStageNameUk(stage: Int): String = when (stage) {
    1 -> "1. Картки"
    2 -> "2. Слухання"
    3 -> "3. Тест"
    4 -> "4. Артиклі"
    5 -> "5. Множина"
    6 -> "6. Речення"
    7 -> "7. Правопис"
    8 -> "8. Фінальний мікс"
    else -> "Етап $stage"
}

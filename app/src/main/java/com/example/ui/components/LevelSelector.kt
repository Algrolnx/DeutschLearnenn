package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen

data class CefrLevelInfo(
    val code: String,
    val titleUk: String,
    val subtitle: String,
    val accentColor: Color
)

val CefrLevels = listOf(
    CefrLevelInfo("A1", "Початковий", "10-12 базових слів", BrandBluePrimary),
    CefrLevelInfo("A2", "Базовий", "12-14 щоденних слів", Color(0xFF0288D1)),
    CefrLevelInfo("B1", "Середній", "12-14 розширених слів", Color(0xFF689F38)),
    CefrLevelInfo("B2", "Поглиблений", "12-14 просунутих слів", Color(0xFF7B1FA2))
)

/**
 * Modern Segmented Level Selector Bar for MainTopicsScreen & SubtopicsListScreen.
 * Prominently lets users choose A1, A2, B1, B2 to comfortably study bite-sized sets.
 */
@Composable
fun MainLevelSelectorBar(
    selectedLevel: String,
    onSelectLevel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Рівень складності:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "до 10-15 слів у темі",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Level Buttons Grid/Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CefrLevels.forEach { info ->
                    val isSelected = info.code.equals(selectedLevel, ignoreCase = true)
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) info.accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        animationSpec = tween(200),
                        label = "level_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "level_text"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { onSelectLevel(info.code) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("level_btn_${info.code.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = info.code,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = contentColor
                                )
                            }
                            Text(
                                text = info.titleUk,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = contentColor.copy(alpha = if (isSelected) 0.95f else 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Level Tabs for SubtopicHubScreen.
 * Shows available levels in this subtopic with exact word count in each.
 */
@Composable
fun SubtopicLevelTabs(
    availableLevels: List<String>,
    selectedLevel: String,
    onSelectLevel: (String) -> Unit,
    getWordCount: (String) -> Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        availableLevels.forEach { level ->
            val isSelected = level.equals(selectedLevel, ignoreCase = true)
            val count = getWordCount(level)
            val info = CefrLevels.firstOrNull { it.code.equals(level, ignoreCase = true) }
            val accent = info?.accentColor ?: BrandBluePrimary

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                animationSpec = tween(200),
                label = "hub_level_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "hub_level_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onSelectLevel(level) }
                    .padding(vertical = 8.dp)
                    .testTag("hub_level_tab_${level.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = level,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                    Text(
                        text = "$count слів",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = if (isSelected) 0.9f else 0.7f)
                    )
                }
            }
        }
    }
}

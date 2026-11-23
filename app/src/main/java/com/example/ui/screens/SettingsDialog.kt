package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LearningViewModel
import com.example.ui.Stage6InputMode
import com.example.ui.components.CefrLevels
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LocalAppExtendedColors
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StreakAmber
import com.example.ui.theme.SuccessGreen
import com.example.util.VoiceToneProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    viewModel: LearningViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val extended = LocalAppExtendedColors.current

    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val autoPronounce by viewModel.autoPronounce.collectAsStateWithLifecycle()
    val useHdVoice by viewModel.ttsManager.useHdVoice.collectAsStateWithLifecycle()
    val isSlowMode by viewModel.ttsManager.isSlowMode.collectAsStateWithLifecycle()
    val voiceTone by viewModel.ttsManager.voiceTone.collectAsStateWithLifecycle()
    val availableVoices by viewModel.ttsManager.availableVoices.collectAsStateWithLifecycle()
    val selectedVoiceName by viewModel.ttsManager.selectedVoiceName.collectAsStateWithLifecycle()
    val totalWords by viewModel.totalWordCount.collectAsStateWithLifecycle()
    val ioStatus by viewModel.ioStatusMessage.collectAsStateWithLifecycle()
    val enabledStages by viewModel.enabledStages.collectAsStateWithLifecycle()
    val dailyReviewLimit by viewModel.dailyReviewLimit.collectAsStateWithLifecycle()
    val stage6InputMode by viewModel.stage6InputMode.collectAsStateWithLifecycle()
    val offlineState by viewModel.offlineDownloadState.collectAsStateWithLifecycle()

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.exportVocabularyJson(stream)
            }
        }
    }

    val exportTsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/tab-separated-values")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.exportVocabularyTsv(stream)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.let { stream ->
                viewModel.importVocabularyStream(stream)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, extended.cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .safeDrawingPadding()
                .padding(vertical = 12.dp)
                .testTag("settings_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Pinned Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Налаштування",
                                fontFamily = LoraFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Einstellungen · Персоналізація",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_settings_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                HorizontalDivider(color = extended.cardBorder)

                // Scrollable Settings Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {

                // Section 0: Theme Selection (Light, Dark, Sepia, Sage, System)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Тема оформлення",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = themeMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else extended.subtleFill,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else extended.cardBorder
                            ),
                            modifier = Modifier
                                .clickable { viewModel.setThemeMode(mode) }
                                .testTag("theme_option_${mode.name.lowercase()}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                // Two-tone palette swatch preview
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(mode.previewBg)
                                        .border(1.5.dp, mode.previewAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(mode.previewAccent)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = mode.titleUk,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.subtitle,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = extended.cardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: CEFR Difficulty Level
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Рівень складності (CEFR)",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "У кожній підтемі відображається до 10–15 слів обраного рівня.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CefrLevels.forEach { info ->
                        val isSelected = info.code.equals(selectedLevel, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) info.accentColor.copy(alpha = 0.12f) else extended.subtleFill,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) info.accentColor else extended.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectLevel(info.code) }
                                .testTag("select_level_${info.code.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) info.accentColor else MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = info.code,
                                            fontFamily = MonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = info.titleUk,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = info.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(info.accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = extended.cardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1.2: Offline Level Preload (Photos + HD Audio)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Офлайн-завантаження рівнів",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Попередньо завантажте фото та HD-вимову обраного рівня, щоб займатися без інтернету. Можна зупинити й продовжити пізніше.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (offlineState.isRunning) {
                    val dlProgress = if (offlineState.totalWords > 0) {
                        (offlineState.processedWords.toFloat() / offlineState.totalWords).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offline_download_progress_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = offlineState.statusMessage ?: "Завантаження рівня ${offlineState.activeLevel}...",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${(dlProgress * 100).toInt()}%",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { dlProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (offlineState.isPausedForRateLimit) StreakAmber else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Фото: ${offlineState.photosReady} · Аудіо: ${offlineState.audioReady}",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { viewModel.cancelOfflineLevelDownload() },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("cancel_offline_download_button")
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Зупинити", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (!offlineState.statusMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessGreen.copy(alpha = 0.13f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = offlineState.statusMessage!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val summaryByLevel = offlineState.levelSummaries.associateBy { it.level.uppercase() }
                    CefrLevels.forEach { info ->
                        val lvl = info.code.uppercase()
                        val status = summaryByLevel[lvl]
                        val cachedCount = status?.cachedWords ?: 0
                        val totalCount = status?.totalWords ?: 0
                        val isComplete = status?.isComplete == true
                        val isThisDownloading = offlineState.isRunning && offlineState.activeLevel == lvl

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = extended.subtleFill,
                            border = BorderStroke(
                                1.dp,
                                if (isComplete) SuccessGreen.copy(alpha = 0.45f) else extended.cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(info.accentColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lvl,
                                            fontFamily = MonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = info.accentColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Рівень $lvl",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (totalCount > 0) {
                                                "У кеші: $cachedCount / $totalCount слів"
                                            } else {
                                                "Перевірка кешу..."
                                            },
                                            fontFamily = MonoFontFamily,
                                            fontSize = 11.sp,
                                            color = if (isComplete) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isComplete) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SuccessGreen.copy(alpha = 0.14f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Готово", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.startOfflineLevelDownload(lvl) },
                                        enabled = !offlineState.isRunning,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("offline_download_${lvl.lowercase()}")
                                    ) {
                                        Text(
                                            text = when {
                                                isThisDownloading -> "Йде..."
                                                cachedCount > 0 -> "Дозавантажити"
                                                else -> "Завантажити"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = extended.cardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 1.5: Customizable Learning & Review Stages
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Етапи навчання та повторення",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Recommendation Callout above toggles
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StreakAmber.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, StreakAmber.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stages_recommendation_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = StreakAmber,
                            modifier = Modifier.size(18.dp).padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Рекомендовано залишити всі 8 етапів",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Поєднання карток, слухання, тестів, артиклів, множини, контексту, правопису й пропущених літер активує різні типи пам'яті та забезпечує найміцніше запам'ятовування слів. Слова потрапляють у цикл «Повторення» лише після проходження всіх 8 етапів підтеми на 100%.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val stageOptions = listOf(
                    Triple(1, "Етап 1 · Картки (Lernkarten)", "Знайомство зі словом, фото, множиною та озвученням"),
                    Triple(2, "Етап 2 · Слухання (Hören)", "Сприйняття німецької мови на слух"),
                    Triple(3, "Етап 3 · Тест (Wortwahl)", "Вибір перекладу з 4 варіантів"),
                    Triple(4, "Етап 4 · Артиклі (der / die / das)", "Тренажер граматичного роду іменників"),
                    Triple(5, "Етап 5 · Множина (Plural-Trainer)", "Форми множини іменників (die ...)"),
                    Triple(6, "Етап 6 · Пропуски (Lückentext)", "Підстановка слова в живе речення"),
                    Triple(7, "Етап 7 · Правопис (Schreiben)", "Повне написання слова (літери або клавіатура · 3 спроби)"),
                    Triple(8, "Етап 8 · Пропущені літери (Lückenwort)", "Вставка пропущених літер у слові (3 спроби)")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stageOptions.forEach { (stageNum, stageTitle, stageDesc) ->
                        val isEnabled = stageNum in enabledStages
                        val isLastActive = isEnabled && enabledStages.size <= 1
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = extended.subtleFill,
                            border = BorderStroke(
                                1.dp,
                                if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else extended.cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = stageTitle,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stageDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isEnabled,
                                    enabled = !isLastActive,
                                    onCheckedChange = { viewModel.toggleStageEnabled(stageNum) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("stage_toggle_$stageNum")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stage 7 & 8 Input Mode Selector (Letter Tiles vs Keyboard)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Введення на Етапах 7 і 8",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Оберіть зручний спосіб написання слів на Етапах 7–8 та в повторенні.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isTiles = stage6InputMode == Stage6InputMode.LETTER_TILES
                    val isKeyboard = stage6InputMode == Stage6InputMode.KEYBOARD
                    FilterChip(
                        selected = isTiles,
                        onClick = { viewModel.setStage6InputMode(Stage6InputMode.LETTER_TILES) },
                        label = {
                            Text(
                                text = "Натискати літери",
                                fontSize = 12.sp,
                                fontWeight = if (isTiles) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stage6_mode_tiles")
                    )
                    FilterChip(
                        selected = isKeyboard,
                        onClick = { viewModel.setStage6InputMode(Stage6InputMode.KEYBOARD) },
                        label = {
                            Text(
                                text = "З клавіатури",
                                fontSize = 12.sp,
                                fontWeight = if (isKeyboard) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stage6_mode_keyboard")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Daily Review Limit Selector (Anti-overload cap)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Денний ліміт повторень",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Захищає від перевтоми: зайві слова спокійно переносяться на наступні дні без тиску.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LearningViewModel.DAILY_LIMIT_PRESETS.forEach { preset ->
                        val selected = dailyReviewLimit == preset
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setDailyReviewLimit(preset) },
                            label = {
                                Text(
                                    text = if (preset == 60) "$preset · Опт." else "$preset / день",
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("daily_limit_$preset")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = extended.cardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Natural Voice Engine
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Вимова та голос",
                        fontFamily = LoraFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // HD Neural Cached Voice Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extended.subtleFill,
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = "Природний HD-голос (кеш на пристрої)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Жива німецька вимова з миттєвим кешуванням; офлайн-TTS як резерв",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useHdVoice,
                            onCheckedChange = { viewModel.ttsManager.setHdVoiceEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("hd_voice_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle: Auto-pronounce
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extended.subtleFill,
                    border = BorderStroke(1.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = "Озвучувати при відкритті картки",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Автоматично читає слово з артиклем на Етапі 1",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = autoPronounce,
                            onCheckedChange = { viewModel.setAutoPronounce(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("auto_pronounce_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Voice Tone Selector
                Text(
                    text = "Тембр офлайн-голосу:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceToneProfile.entries.forEach { profile ->
                        val selected = voiceTone == profile
                        FilterChip(
                            selected = selected,
                            onClick = {
                                viewModel.ttsManager.setVoiceTone(profile)
                                viewModel.speak("Hallo, der Apfel")
                            },
                            label = {
                                Text(
                                    text = profile.titleUk,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (availableVoices.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Доступні системні голоси:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableVoices.forEach { option ->
                            val isSelected = selectedVoiceName == option.name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.ttsManager.selectVoice(option.name)
                                    viewModel.speak("Guten Tag!")
                                },
                                label = {
                                    Text(
                                        text = option.displayLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Voice & Slow Toggle Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewModel.speak("Guten Tag, der Apfel schmeckt gut!")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("test_audio_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Тест голосу", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.ttsManager.toggleSlowMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSlowMode) StreakAmber.copy(alpha = 0.22f) else extended.subtleFill,
                            contentColor = if (isSlowMode) StreakAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("toggle_slow_mode_button")
                    ) {
                        Text(
                            text = if (isSlowMode) "Повільно (0.75x)" else "Темп: 1.0x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = extended.cardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Export & Import Dictionary (Zero-permission SAF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "База слів (Експорт / Імпорт)",
                            fontFamily = LoraFontFamily,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$totalWords слів",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Зберігайте резервну копію словника та прогресу або додавайте власні слова через файли JSON / TSV.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { exportJsonLauncher.launch("deutschlern_words.json") },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, extended.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_json_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Експорт JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { exportTsvLauncher.launch("deutschlern_words.tsv") },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, extended.cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_tsv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Експорт TSV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/tab-separated-values", "text/plain", "*/*")
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.subtleFill,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_words_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Імпортувати слова (JSON / TSV)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (ioStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (ioStatus!!.isError) ErrorRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ioStatus!!.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (ioStatus!!.isError) ErrorRed else SuccessGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = extended.cardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Section 4: Image Source Attribution (moved cleanly off flashcard photos)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extended.subtleFill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pixabay_attribution_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Джерело фотографій: Pixabay",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Фото для карток надано Pixabay (pixabay.com) і кешуються локально на пристрої.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                }

                HorizontalDivider(color = extended.cardBorder)

                // Pinned Done Button safely above phone navigation bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("done_settings_button")
                    ) {
                        Text(
                            text = "Зберегти та закрити",
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

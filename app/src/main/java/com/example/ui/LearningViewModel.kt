package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.BoxCount
import com.example.data.db.LevelStats
import com.example.data.db.SubtopicLevelCount
import com.example.data.model.SubtopicProgressEntity
import com.example.data.model.WordEntity
import com.example.data.repository.StockPhotoRepository
import com.example.data.repository.VocabularyRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.ReviewRating
import com.example.util.SpacedRepetition
import com.example.util.TtsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

sealed class Screen {
    object MainTopics : Screen()
    data class SubtopicsList(val mainTopicId: String) : Screen()
    data class SubtopicHub(val subtopicId: String) : Screen()

    // 8-Stage Learning Progression
    data class Stage1Flashcards(val subtopicId: String) : Screen()
    data class Stage2Listening(val subtopicId: String) : Screen()
    data class Stage3MultipleChoice(val subtopicId: String) : Screen()
    data class Stage4ArticleTrainer(val subtopicId: String) : Screen()
    data class Stage5PluralTrainer(val subtopicId: String) : Screen()
    data class Stage6FillBlank(val subtopicId: String) : Screen()
    data class Stage7Spelling(val subtopicId: String) : Screen()
    data class Stage8MissingLetters(val subtopicId: String) : Screen()

    object MixedReview : Screen()
    object Progress : Screen()
}

enum class MainTab {
    TOPICS,
    REVIEW,
    FAVORITES
}

data class ReviewDashboardState(
    val dueCount: Int = 0,
    val enrolledCount: Int = 0,
    val mastered16Count: Int = 0,
    val nextReviewTimestamp: Long? = null,
    val reviewsDoneToday: Int = 0,
    val dailyLimit: Int = LearningViewModel.DEFAULT_DAILY_REVIEW_LIMIT,
    val isLoading: Boolean = false
) {
    val remainingDailyQuota: Int get() = (dailyLimit - reviewsDoneToday).coerceAtLeast(0)
    val availableTodayCount: Int get() = minOf(dueCount, remainingDailyQuota)
    val deferredToTomorrowCount: Int get() = (dueCount - remainingDailyQuota).coerceAtLeast(0)
}

sealed class MixedReviewItem {
    abstract val word: WordEntity
    abstract val srsStep: Int
    abstract val stageType: Int
    abstract val isRetry: Boolean

    data class FlashcardReview(
        override val word: WordEntity,
        override val srsStep: Int,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 1
    }

    data class ListeningReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val options: List<String>,
        val correctIndex: Int,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 2
    }

    data class MultipleChoiceReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val options: List<String>,
        val correctIndex: Int,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 3
    }

    data class ArticleReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val correctArticle: String,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 4
    }

    data class PluralReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val options: List<String>,
        val correctPlural: String,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 5
    }

    data class FillBlankReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val sentenceWithBlank: String,
        val translation: String,
        val options: List<String>,
        val correctOption: String,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 6
    }

    data class SpellingReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val letterPool: List<Char>,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 7
    }

    data class MissingLettersReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val blankIndices: List<Int>,
        val letterPool: List<Char>,
        override val isRetry: Boolean = false
    ) : MixedReviewItem() {
        override val stageType: Int = 8
    }
}

data class MultipleChoiceQ(
    val word: WordEntity,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val isRetry: Boolean = false
)

data class ListeningQ(
    val word: WordEntity,
    val options: List<String>,
    val correctIndex: Int,
    val isRetry: Boolean = false
)

data class ArticleQ(
    val word: WordEntity,
    val correctArticle: String,
    val isRetry: Boolean = false
)

data class PluralQ(
    val word: WordEntity,
    val options: List<String>,
    val correctPlural: String,
    val isRetry: Boolean = false
)

data class FillBlankQ(
    val word: WordEntity,
    val sentenceWithBlank: String,
    val translation: String,
    val options: List<String>,
    val correctOption: String,
    val isRetry: Boolean = false
)

data class MissingLettersQ(
    val word: WordEntity,
    val blankIndices: List<Int>,
    val letterPool: List<Char>,
    val isRetry: Boolean = false
)

data class IoStatusMessage(
    val text: String,
    val isError: Boolean = false
)

enum class Stage6WordStatus {
    TYPING,
    WRONG_TRY,
    CORRECT,
    CORRECT_UMLAUT_WARNING,
    OUT_OF_ATTEMPTS;

    val isResolved: Boolean
        get() = this == CORRECT || this == CORRECT_UMLAUT_WARNING || this == OUT_OF_ATTEMPTS
}

enum class Stage6InputMode {
    LETTER_TILES,
    KEYBOARD
}

data class LevelOfflineStatus(
    val level: String,
    val cachedWords: Int,
    val totalWords: Int
) {
    val isComplete: Boolean get() = totalWords > 0 && cachedWords >= totalWords
}

data class OfflineDownloadState(
    val activeLevel: String? = null,
    val processedWords: Int = 0,
    val totalWords: Int = 0,
    val photosReady: Int = 0,
    val audioReady: Int = 0,
    val isPausedForRateLimit: Boolean = false,
    val statusMessage: String? = null,
    val isRunning: Boolean = false,
    val levelSummaries: List<LevelOfflineStatus> = emptyList()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LearningViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
    private val repository: VocabularyRepository
    private val stockPhotoRepository: StockPhotoRepository = StockPhotoRepository.getInstance(application)
    val ttsManager: TtsManager = TtsManager(application)

    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.MainTopics))
    val currentScreen: StateFlow<Screen> = MutableStateFlow<Screen>(Screen.MainTopics).apply {
        viewModelScope.launch {
            _screenStack.collect { stack ->
                value = stack.lastOrNull() ?: Screen.MainTopics
            }
        }
    }

    // Lightweight aggregate & filtered DB flows (never holds 6,000 full rows in Compose memory)
    val totalWordCount: StateFlow<Int>
    val favoriteWords: StateFlow<List<WordEntity>>
    val boxCounts: StateFlow<List<BoxCount>>
    val levelStats: StateFlow<List<LevelStats>>
    val subtopicLevelCounts: StateFlow<List<SubtopicLevelCount>>
    val allProgress: StateFlow<List<SubtopicProgressEntity>>

    private val _streakDays = MutableStateFlow(5)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    // === Theme Selection ===
    private val initialTheme = AppThemeMode.entries.firstOrNull {
        it.name == prefs.getString("app_theme_mode", AppThemeMode.LIGHT.name)
    } ?: AppThemeMode.LIGHT

    private val _themeMode = MutableStateFlow(initialTheme)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    // === Selected CEFR Level (A1, A2, B1, B2) ===
    private val _selectedLevel = MutableStateFlow(prefs.getString("selected_cefr_level", "A1") ?: "A1")
    val selectedLevel: StateFlow<String> = _selectedLevel.asStateFlow()

    fun selectLevel(level: String) {
        val norm = level.uppercase()
        _selectedLevel.value = norm
        prefs.edit().putString("selected_cefr_level", norm).apply()
        _activeCardIndexInHub.value = 0
    }

    // === Settings & Audio Pronunciation ===
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    fun openSettings() {
        _isSettingsOpen.value = true
        refreshOfflineLevelSummaries()
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
        _ioStatusMessage.value = null
    }

    // === Stage 6 Input Mode (Letter Tiles vs Keyboard) ===
    private val initialStage6Mode = Stage6InputMode.entries.firstOrNull {
        it.name == prefs.getString("stage6_input_mode", Stage6InputMode.LETTER_TILES.name)
    } ?: Stage6InputMode.LETTER_TILES

    private val _stage6InputMode = MutableStateFlow(initialStage6Mode)
    val stage6InputMode: StateFlow<Stage6InputMode> = _stage6InputMode.asStateFlow()

    fun setStage6InputMode(mode: Stage6InputMode) {
        _stage6InputMode.value = mode
        prefs.edit().putString("stage6_input_mode", mode.name).apply()
    }

    // === Offline Level Preload (Photos + HD Pronunciation) ===
    private var offlineDownloadJob: Job? = null
    private val _offlineDownloadState = MutableStateFlow(OfflineDownloadState())
    val offlineDownloadState: StateFlow<OfflineDownloadState> = _offlineDownloadState.asStateFlow()

    fun refreshOfflineLevelSummaries() {
        viewModelScope.launch {
            val summaries = computeOfflineLevelSummaries()
            _offlineDownloadState.value = _offlineDownloadState.value.copy(levelSummaries = summaries)
        }
    }

    private suspend fun computeOfflineLevelSummaries(): List<LevelOfflineStatus> = withContext(Dispatchers.IO) {
        val levels = listOf("A1", "A2", "B1", "B2")
        val checkPhotos = stockPhotoRepository.hasConfiguredApiKey()
        levels.map { lvl ->
            val words = repository.getWordsByLevelOnce(lvl)
            val cached = words.count { w ->
                val audioOk = ttsManager.isHdAudioCached(w.displayWithArticle)
                val photoOk = !checkPhotos || stockPhotoRepository.isPhotoDiskCached(w)
                audioOk && photoOk
            }
            LevelOfflineStatus(level = lvl, cachedWords = cached, totalWords = words.size)
        }
    }

    fun startOfflineLevelDownload(level: String) {
        val normLevel = level.uppercase()
        if (_offlineDownloadState.value.isRunning) {
            offlineDownloadJob?.cancel()
        }
        offlineDownloadJob = viewModelScope.launch {
            val allLevelWords = repository.getWordsByLevelOnce(normLevel)
            if (allLevelWords.isEmpty()) {
                _offlineDownloadState.value = _offlineDownloadState.value.copy(
                    isRunning = false,
                    activeLevel = null,
                    statusMessage = "Немає слів для рівня $normLevel"
                )
                return@launch
            }

            // Order subtopic-first (up to 15 words per subtopic first) so active lessons cache earliest
            val grouped = allLevelWords.groupBy { it.subtopicId }
            val priorityBatch = grouped.values.flatMap { it.take(15) }
            val priorityIds = priorityBatch.mapTo(HashSet()) { it.id }
            val orderedWords = priorityBatch + allLevelWords.filter { it.id !in priorityIds }

            val hasPhotoApi = stockPhotoRepository.hasConfiguredApiKey()
            var photosReady = 0
            var audioReady = 0

            _offlineDownloadState.value = _offlineDownloadState.value.copy(
                activeLevel = normLevel,
                processedWords = 0,
                totalWords = orderedWords.size,
                photosReady = 0,
                audioReady = 0,
                isPausedForRateLimit = false,
                isRunning = true,
                statusMessage = "Підготовка рівня $normLevel..."
            )

            try {
                for ((index, word) in orderedWords.withIndex()) {
                    if (!isActive) break

                    // 1. Preload HD German audio (with article, bare noun for Stage 4, and plural for Stage 5)
                    val audioWithArt = ttsManager.prefetchHdAudio(word.displayWithArticle)
                    if (word.article.isNotBlank() && !word.german.equals(word.displayWithArticle, ignoreCase = true)) {
                        ttsManager.prefetchHdAudio(word.german)
                    }
                    if (isPluralizableNoun(word)) {
                        ttsManager.prefetchHdAudio(word.plural.trim())
                    }
                    if (audioWithArt) audioReady++

                    // 2. Preload photo URL + Coil JPEG bytes into disk cache with Pixabay 429 pacing
                    if (hasPhotoApi) {
                        val alreadyDiskCached = stockPhotoRepository.isPhotoDiskCached(word)
                        if (alreadyDiskCached) {
                            photosReady++
                        } else {
                            val hadUrl = stockPhotoRepository.getCachedSync(word) != null
                            if (!hadUrl) {
                                val waitMs = stockPhotoRepository.remainingCooldownMs()
                                if (waitMs > 0L) {
                                    val waitSec = ((waitMs + 999L) / 1000L).coerceAtLeast(1L)
                                    _offlineDownloadState.value = _offlineDownloadState.value.copy(
                                        isPausedForRateLimit = true,
                                        statusMessage = "Пауза через ліміт Pixabay (~$waitSec с)..."
                                    )
                                    delay(waitMs + 500L)
                                    _offlineDownloadState.value = _offlineDownloadState.value.copy(
                                        isPausedForRateLimit = false,
                                        statusMessage = "Завантаження рівня $normLevel..."
                                    )
                                }
                            }
                            val ok = stockPhotoRepository.preloadWordForOffline(word)
                            if (ok) photosReady++
                            // Gentle pacing only when we had to hit the Pixabay API
                            if (!hadUrl && stockPhotoRepository.remainingCooldownMs() == 0L) {
                                delay(650L)
                            }
                        }
                    }

                    val done = index + 1
                    _offlineDownloadState.value = _offlineDownloadState.value.copy(
                        processedWords = done,
                        photosReady = photosReady,
                        audioReady = audioReady,
                        isPausedForRateLimit = false,
                        statusMessage = "Рівень $normLevel: $done / ${orderedWords.size} слів"
                    )
                }

                val refreshed = computeOfflineLevelSummaries()
                _offlineDownloadState.value = _offlineDownloadState.value.copy(
                    isRunning = false,
                    isPausedForRateLimit = false,
                    activeLevel = null,
                    levelSummaries = refreshed,
                    statusMessage = "Рівень $normLevel збережено для офлайн-занять (фото: $photosReady, аудіо: $audioReady)"
                )
            } catch (_: CancellationException) {
                val refreshed = computeOfflineLevelSummaries()
                _offlineDownloadState.value = _offlineDownloadState.value.copy(
                    isRunning = false,
                    isPausedForRateLimit = false,
                    activeLevel = null,
                    levelSummaries = refreshed,
                    statusMessage = "Завантаження $normLevel зупинено · вже збережене лишилось у кеші"
                )
            } catch (e: Exception) {
                val refreshed = computeOfflineLevelSummaries()
                _offlineDownloadState.value = _offlineDownloadState.value.copy(
                    isRunning = false,
                    isPausedForRateLimit = false,
                    activeLevel = null,
                    levelSummaries = refreshed,
                    statusMessage = "Помилка завантаження: ${e.message ?: "перевірте інтернет"}"
                )
            }
        }
    }

    fun cancelOfflineLevelDownload() {
        offlineDownloadJob?.cancel()
        offlineDownloadJob = null
    }

    private val _autoPronounce = MutableStateFlow(prefs.getBoolean("tts_auto_pronounce", true))
    val autoPronounce: StateFlow<Boolean> = _autoPronounce.asStateFlow()

    fun setAutoPronounce(enabled: Boolean) {
        _autoPronounce.value = enabled
        prefs.edit().putBoolean("tts_auto_pronounce", enabled).apply()
    }

    fun speakIfAuto(text: String, slow: Boolean = false) {
        if (_autoPronounce.value) {
            speak(text, slow)
        }
    }

    // === Main Bottom Navigation Tab ===
    private val _selectedMainTab = MutableStateFlow(MainTab.TOPICS)
    val selectedMainTab: StateFlow<MainTab> = _selectedMainTab.asStateFlow()

    fun selectMainTab(tab: MainTab) {
        _selectedMainTab.value = tab
        if (tab == MainTab.REVIEW) {
            refreshReviewDashboard()
        }
    }

    // === Customizable Enabled Learning & Review Stages (1..8) ===
    private fun loadEnabledStages(): Set<Int> {
        val raw = prefs.getString("enabled_stages_csv", null) ?: return ALL_STAGES
        val parsed = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..8 }
            .toMutableSet()
        // One-time upgrade so returning 6-stage users automatically receive the new stages 7 & 8 enabled
        if (!prefs.getBoolean("stages_migrated_to_8", false)) {
            parsed.add(7)
            parsed.add(8)
            prefs.edit()
                .putString("enabled_stages_csv", parsed.sorted().joinToString(","))
                .putBoolean("stages_migrated_to_8", true)
                .apply()
        }
        return parsed.ifEmpty { ALL_STAGES }
    }

    private val _enabledStages = MutableStateFlow(loadEnabledStages())
    val enabledStages: StateFlow<Set<Int>> = _enabledStages.asStateFlow()

    fun toggleStageEnabled(stage: Int) {
        if (stage !in 1..8) return
        val current = _enabledStages.value
        val updated = if (stage in current) {
            if (current.size <= 1) return // Always keep at least one stage active
            current - stage
        } else {
            current + stage
        }
        _enabledStages.value = updated
        prefs.edit()
            .putString("enabled_stages_csv", updated.sorted().joinToString(","))
            .putBoolean("stages_migrated_to_8", true)
            .apply()
    }

    fun getFirstEnabledStage(): Int = _enabledStages.value.minOrNull() ?: 1

    fun getNextEnabledStage(afterStage: Int): Int? = nextEnabledStage(_enabledStages.value, afterStage)

    fun startStageByNumber(subtopicId: String, stage: Int) {
        when (stage) {
            1 -> startStage1(subtopicId)
            2 -> startStage2(subtopicId)
            3 -> startStage3(subtopicId)
            4 -> startStage4(subtopicId)
            5 -> startStage5Plural(subtopicId)
            6 -> startStage5(subtopicId)
            7 -> startStage6(subtopicId)
            8 -> startStage8MissingLetters(subtopicId)
        }
    }

    fun startNextEnabledStage(subtopicId: String, afterStage: Int) {
        val next = getNextEnabledStage(afterStage)
        if (next != null) {
            startStageByNumber(subtopicId, next)
        } else {
            navigateBack()
        }
    }

    // === Spaced Repetition Daily Limit & Anti-Overload Quota ===
    private fun todayDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun loadDailyReviewLimit(): Int {
        val saved = prefs.getInt("daily_review_limit", DEFAULT_DAILY_REVIEW_LIMIT)
        return saved.coerceIn(20, 100)
    }

    private fun loadReviewsDoneToday(): Int {
        val savedDate = prefs.getString("daily_review_date", null)
        return if (savedDate == todayDateKey()) {
            prefs.getInt("daily_review_count", 0).coerceAtLeast(0)
        } else {
            0
        }
    }

    private fun incrementReviewsDoneToday() {
        val today = todayDateKey()
        val current = loadReviewsDoneToday() + 1
        prefs.edit()
            .putString("daily_review_date", today)
            .putInt("daily_review_count", current)
            .apply()
        _reviewsDoneToday.value = current
        _reviewDashboard.value = _reviewDashboard.value.copy(
            reviewsDoneToday = current,
            dailyLimit = _dailyReviewLimit.value
        )
    }

    private val _dailyReviewLimit = MutableStateFlow(loadDailyReviewLimit())
    val dailyReviewLimit: StateFlow<Int> = _dailyReviewLimit.asStateFlow()

    private val _reviewsDoneToday = MutableStateFlow(loadReviewsDoneToday())
    val reviewsDoneToday: StateFlow<Int> = _reviewsDoneToday.asStateFlow()

    private val _reviewDashboard = MutableStateFlow(
        ReviewDashboardState(
            reviewsDoneToday = _reviewsDoneToday.value,
            dailyLimit = _dailyReviewLimit.value
        )
    )
    val reviewDashboard: StateFlow<ReviewDashboardState> = _reviewDashboard.asStateFlow()

    fun setDailyReviewLimit(limit: Int) {
        val safe = limit.coerceIn(20, 100)
        _dailyReviewLimit.value = safe
        prefs.edit().putInt("daily_review_limit", safe).apply()
        _reviewDashboard.value = _reviewDashboard.value.copy(dailyLimit = safe)
    }

    fun refreshReviewDashboard() {
        viewModelScope.launch {
            val doneToday = loadReviewsDoneToday()
            _reviewsDoneToday.value = doneToday
            val snap = repository.getReviewDashboardSnapshot()
            _reviewDashboard.value = ReviewDashboardState(
                dueCount = snap.dueCount,
                enrolledCount = snap.enrolledCount,
                mastered16Count = snap.mastered16Count,
                nextReviewTimestamp = snap.nextReviewTimestamp,
                reviewsDoneToday = doneToday,
                dailyLimit = _dailyReviewLimit.value,
                isLoading = false
            )
        }
    }

    // === Mixed-Stage Spaced Repetition Session State ===
    private val _mixedReviewItems = MutableStateFlow<List<MixedReviewItem>>(emptyList())
    val mixedReviewItems: StateFlow<List<MixedReviewItem>> = _mixedReviewItems.asStateFlow()
    private val _mixedReviewIndex = MutableStateFlow(0)
    val mixedReviewIndex: StateFlow<Int> = _mixedReviewIndex.asStateFlow()
    private val _mixedReviewScore = MutableStateFlow(0)
    val mixedReviewScore: StateFlow<Int> = _mixedReviewScore.asStateFlow()
    private val _mixedReviewFinished = MutableStateFlow(false)
    val mixedReviewFinished: StateFlow<Boolean> = _mixedReviewFinished.asStateFlow()
    private val _mixedReviewIsEarlyPractice = MutableStateFlow(false)
    val mixedReviewIsEarlyPractice: StateFlow<Boolean> = _mixedReviewIsEarlyPractice.asStateFlow()
    private val _mixedCardFlipped = MutableStateFlow(false)
    val mixedCardFlipped: StateFlow<Boolean> = _mixedCardFlipped.asStateFlow()
    private val _mixedSelectedOptionIndex = MutableStateFlow<Int?>(null)
    val mixedSelectedOptionIndex: StateFlow<Int?> = _mixedSelectedOptionIndex.asStateFlow()
    private val _mixedSelectedString = MutableStateFlow<String?>(null)
    val mixedSelectedString: StateFlow<String?> = _mixedSelectedString.asStateFlow()
    private val _mixedSpellingInput = MutableStateFlow("")
    val mixedSpellingInput: StateFlow<String> = _mixedSpellingInput.asStateFlow()
    private val _mixedSpellingStatus = MutableStateFlow(Stage6WordStatus.TYPING)
    val mixedSpellingStatus: StateFlow<Stage6WordStatus> = _mixedSpellingStatus.asStateFlow()
    private val _mixedSpellingAttemptsLeft = MutableStateFlow(MAX_STAGE6_ATTEMPTS)
    val mixedSpellingAttemptsLeft: StateFlow<Int> = _mixedSpellingAttemptsLeft.asStateFlow()
    private val _mixedLastFeedback = MutableStateFlow<String?>(null)
    val mixedLastFeedback: StateFlow<String?> = _mixedLastFeedback.asStateFlow()

    // === Export / Import Status ===
    private val _ioStatusMessage = MutableStateFlow<IoStatusMessage?>(null)
    val ioStatusMessage: StateFlow<IoStatusMessage?> = _ioStatusMessage.asStateFlow()

    fun clearIoStatus() {
        _ioStatusMessage.value = null
    }

    fun exportVocabularyJson(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val json = repository.exportWordsAsJson()
                outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                val total = totalWordCount.value
                _ioStatusMessage.value = IoStatusMessage("Експортовано $total слів у формат JSON")
            } catch (e: Exception) {
                _ioStatusMessage.value = IoStatusMessage("Помилка експорту: ${e.message}", isError = true)
            }
        }
    }

    fun exportVocabularyTsv(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val tsv = repository.exportWordsAsTsv()
                outputStream.use { it.write(tsv.toByteArray(Charsets.UTF_8)) }
                val total = totalWordCount.value
                _ioStatusMessage.value = IoStatusMessage("Експортовано $total слів у таблицю TSV")
            } catch (e: Exception) {
                _ioStatusMessage.value = IoStatusMessage("Помилка експорту: ${e.message}", isError = true)
            }
        }
    }

    fun importVocabularyStream(inputStream: InputStream) {
        viewModelScope.launch {
            try {
                val text = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
                val summary = repository.importWordsFromString(text)
                if (summary.importedCount == 0 && summary.updatedCount == 0) {
                    _ioStatusMessage.value = IoStatusMessage(
                        "Не знайдено коректних слів у файлі. Перевірте формат JSON або TSV.",
                        isError = true
                    )
                } else {
                    _ioStatusMessage.value = IoStatusMessage(
                        "Імпортовано: +${summary.importedCount} нових, оновлено: ${summary.updatedCount}"
                    )
                }
            } catch (e: Exception) {
                _ioStatusMessage.value = IoStatusMessage("Помилка імпорту: ${e.message}", isError = true)
            }
        }
    }

    // === Global SQL Instant Search (max 40 results via indexed query) ===
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<WordEntity>>

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // === Subtopic Hub Active Words (10-15 words loaded cleanly per subtopic + level) ===
    private val _activeCardIndexInHub = MutableStateFlow(0)
    val activeCardIndexInHub: StateFlow<Int> = _activeCardIndexInHub.asStateFlow()

    private val _hubWords = MutableStateFlow<List<WordEntity>>(emptyList())
    val hubWords: StateFlow<List<WordEntity>> = _hubWords.asStateFlow()

    fun loadHubWords(subtopicId: String, level: String = _selectedLevel.value) {
        viewModelScope.launch {
            _hubWords.value = repository.getWordsBySubtopicAndLevelOnce(subtopicId, level, 15)
        }
    }

    // === Stage 1: Flashcards Session State ===
    private val _stage1Words = MutableStateFlow<List<WordEntity>>(emptyList())
    val stage1Words: StateFlow<List<WordEntity>> = _stage1Words.asStateFlow()
    private val _stage1Index = MutableStateFlow(0)
    val stage1Index: StateFlow<Int> = _stage1Index.asStateFlow()
    private val _stage1Flipped = MutableStateFlow(false)
    val stage1Flipped: StateFlow<Boolean> = _stage1Flipped.asStateFlow()
    private val _stage1Finished = MutableStateFlow(false)
    val stage1Finished: StateFlow<Boolean> = _stage1Finished.asStateFlow()
    private val _stage1AllowPhotoChange = MutableStateFlow(true)
    val stage1AllowPhotoChange: StateFlow<Boolean> = _stage1AllowPhotoChange.asStateFlow()

    // === Stage 2: Listening Quiz State ===
    private val _stage2Questions = MutableStateFlow<List<ListeningQ>>(emptyList())
    val stage2Questions: StateFlow<List<ListeningQ>> = _stage2Questions.asStateFlow()
    private val _stage2Index = MutableStateFlow(0)
    val stage2Index: StateFlow<Int> = _stage2Index.asStateFlow()
    private val _stage2SelectedAnswer = MutableStateFlow<Int?>(null)
    val stage2SelectedAnswer: StateFlow<Int?> = _stage2SelectedAnswer.asStateFlow()
    private val _stage2Score = MutableStateFlow(0)
    val stage2Score: StateFlow<Int> = _stage2Score.asStateFlow()
    private val _stage2Finished = MutableStateFlow(false)
    val stage2Finished: StateFlow<Boolean> = _stage2Finished.asStateFlow()

    // === Stage 3: Multiple Choice Quiz State ===
    private val _stage3Questions = MutableStateFlow<List<MultipleChoiceQ>>(emptyList())
    val stage3Questions: StateFlow<List<MultipleChoiceQ>> = _stage3Questions.asStateFlow()
    private val _stage3Index = MutableStateFlow(0)
    val stage3Index: StateFlow<Int> = _stage3Index.asStateFlow()
    private val _stage3SelectedAnswer = MutableStateFlow<Int?>(null)
    val stage3SelectedAnswer: StateFlow<Int?> = _stage3SelectedAnswer.asStateFlow()
    private val _stage3Score = MutableStateFlow(0)
    val stage3Score: StateFlow<Int> = _stage3Score.asStateFlow()
    private val _stage3Finished = MutableStateFlow(false)
    val stage3Finished: StateFlow<Boolean> = _stage3Finished.asStateFlow()

    // === Stage 4: Article Trainer (der / die / das) State ===
    private val _stage4Questions = MutableStateFlow<List<ArticleQ>>(emptyList())
    val stage4Questions: StateFlow<List<ArticleQ>> = _stage4Questions.asStateFlow()
    private val _stage4Index = MutableStateFlow(0)
    val stage4Index: StateFlow<Int> = _stage4Index.asStateFlow()
    private val _stage4SelectedArticle = MutableStateFlow<String?>(null)
    val stage4SelectedArticle: StateFlow<String?> = _stage4SelectedArticle.asStateFlow()
    private val _stage4Score = MutableStateFlow(0)
    val stage4Score: StateFlow<Int> = _stage4Score.asStateFlow()
    private val _stage4Finished = MutableStateFlow(false)
    val stage4Finished: StateFlow<Boolean> = _stage4Finished.asStateFlow()

    // === Stage 5: Plural Trainer (der/die/das -> die ...) State ===
    private val _stage5PluralQuestions = MutableStateFlow<List<PluralQ>>(emptyList())
    val stage5PluralQuestions: StateFlow<List<PluralQ>> = _stage5PluralQuestions.asStateFlow()
    private val _stage5PluralIndex = MutableStateFlow(0)
    val stage5PluralIndex: StateFlow<Int> = _stage5PluralIndex.asStateFlow()
    private val _stage5PluralSelected = MutableStateFlow<String?>(null)
    val stage5PluralSelected: StateFlow<String?> = _stage5PluralSelected.asStateFlow()
    private val _stage5PluralScore = MutableStateFlow(0)
    val stage5PluralScore: StateFlow<Int> = _stage5PluralScore.asStateFlow()
    private val _stage5PluralFinished = MutableStateFlow(false)
    val stage5PluralFinished: StateFlow<Boolean> = _stage5PluralFinished.asStateFlow()
    private val _stage5PluralAutoSkipped = MutableStateFlow(false)
    val stage5PluralAutoSkipped: StateFlow<Boolean> = _stage5PluralAutoSkipped.asStateFlow()

    // === Stage 6: Fill in the Blank State ===
    private val _stage5Questions = MutableStateFlow<List<FillBlankQ>>(emptyList())
    val stage5Questions: StateFlow<List<FillBlankQ>> = _stage5Questions.asStateFlow()
    private val _stage5Index = MutableStateFlow(0)
    val stage5Index: StateFlow<Int> = _stage5Index.asStateFlow()
    private val _stage5SelectedOption = MutableStateFlow<String?>(null)
    val stage5SelectedOption: StateFlow<String?> = _stage5SelectedOption.asStateFlow()
    private val _stage5Score = MutableStateFlow(0)
    val stage5Score: StateFlow<Int> = _stage5Score.asStateFlow()
    private val _stage5Finished = MutableStateFlow(false)
    val stage5Finished: StateFlow<Boolean> = _stage5Finished.asStateFlow()

    // === Stage 7: Spelling & Writing State ===
    private val _stage6Words = MutableStateFlow<List<WordEntity>>(emptyList())
    val stage6Words: StateFlow<List<WordEntity>> = _stage6Words.asStateFlow()
    private val _stage6Index = MutableStateFlow(0)
    val stage6Index: StateFlow<Int> = _stage6Index.asStateFlow()
    private val _stage6LetterPool = MutableStateFlow<List<Char>>(emptyList())
    val stage6LetterPool: StateFlow<List<Char>> = _stage6LetterPool.asStateFlow()
    private val _stage6Input = MutableStateFlow("")
    val stage6Input: StateFlow<String> = _stage6Input.asStateFlow()
    private val _stage6Status = MutableStateFlow(Stage6WordStatus.TYPING)
    val stage6Status: StateFlow<Stage6WordStatus> = _stage6Status.asStateFlow()
    private val _stage6AttemptsLeft = MutableStateFlow(MAX_STAGE6_ATTEMPTS)
    val stage6AttemptsLeft: StateFlow<Int> = _stage6AttemptsLeft.asStateFlow()
    private val _stage6HintsUsed = MutableStateFlow(0)
    val stage6HintsUsed: StateFlow<Int> = _stage6HintsUsed.asStateFlow()
    private val _stage6Score = MutableStateFlow(0)
    val stage6Score: StateFlow<Int> = _stage6Score.asStateFlow()
    private val _stage6Finished = MutableStateFlow(false)
    val stage6Finished: StateFlow<Boolean> = _stage6Finished.asStateFlow()
    private val _stage6RetryIndices = MutableStateFlow<Set<Int>>(emptySet())
    val stage6RetryIndices: StateFlow<Set<Int>> = _stage6RetryIndices.asStateFlow()

    // === Stage 8: Missing Letters in Word (Lückenwort) State ===
    private val _stage8Questions = MutableStateFlow<List<MissingLettersQ>>(emptyList())
    val stage8Questions: StateFlow<List<MissingLettersQ>> = _stage8Questions.asStateFlow()
    private val _stage8Index = MutableStateFlow(0)
    val stage8Index: StateFlow<Int> = _stage8Index.asStateFlow()
    private val _stage8Input = MutableStateFlow("")
    val stage8Input: StateFlow<String> = _stage8Input.asStateFlow()
    private val _stage8Status = MutableStateFlow(Stage6WordStatus.TYPING)
    val stage8Status: StateFlow<Stage6WordStatus> = _stage8Status.asStateFlow()
    private val _stage8AttemptsLeft = MutableStateFlow(MAX_STAGE6_ATTEMPTS)
    val stage8AttemptsLeft: StateFlow<Int> = _stage8AttemptsLeft.asStateFlow()
    private val _stage8Score = MutableStateFlow(0)
    val stage8Score: StateFlow<Int> = _stage8Score.asStateFlow()
    private val _stage8Finished = MutableStateFlow(false)
    val stage8Finished: StateFlow<Boolean> = _stage8Finished.asStateFlow()

    // True when the most recently completed stage brought the subtopic to 8/8 stages at 100% (unlocking SRS)
    private val _lastCompletedUnlockedSrs = MutableStateFlow(false)
    val lastCompletedUnlockedSrs: StateFlow<Boolean> = _lastCompletedUnlockedSrs.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VocabularyRepository(db.wordDao(), application.applicationContext)

        viewModelScope.launch {
            repository.initializePrepopulatedDataIfNeeded()
            refreshReviewDashboard()
        }

        viewModelScope.launch {
            repository.observeEnrolledWordCount().collect {
                refreshReviewDashboard()
            }
        }

        totalWordCount = repository.observeWordCount().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            6000
        )

        favoriteWords = repository.getFavoriteWords().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        boxCounts = repository.observeBoxCounts().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        levelStats = repository.observeLevelStats().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        subtopicLevelCounts = repository.observeSubtopicLevelCounts().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allProgress = repository.getAllSubtopicProgress().stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        searchResults = _searchQuery
            .debounce(120)
            .flatMapLatest { raw ->
                val trimmed = raw.trim()
                if (trimmed.length < 2) flowOf(emptyList())
                else repository.searchWordsLimited(trimmed)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
    }

    // Navigation
    fun navigateTo(screen: Screen) {
        _screenStack.value = _screenStack.value + screen
    }

    private fun Screen.isStageScreen(): Boolean = when (this) {
        is Screen.Stage1Flashcards,
        is Screen.Stage2Listening,
        is Screen.Stage3MultipleChoice,
        is Screen.Stage4ArticleTrainer,
        is Screen.Stage5PluralTrainer,
        is Screen.Stage6FillBlank,
        is Screen.Stage7Spelling,
        is Screen.Stage8MissingLetters,
        is Screen.MixedReview -> true
        else -> false
    }

    /**
     * Replaces any currently open stage screen so advancing Stage 1 -> 2 -> ... -> 6
     * never leaves finished stages stacked behind the active screen.
     */
    private fun navigateToStage(screen: Screen) {
        val baseStack = _screenStack.value.dropLastWhile { it.isStageScreen() }
        _screenStack.value = baseStack + screen
    }

    fun navigateBack(): Boolean {
        if (_screenStack.value.size > 1) {
            _screenStack.value = _screenStack.value.dropLast(1)
            if (_screenStack.value.lastOrNull() == Screen.MainTopics) {
                refreshReviewDashboard()
            }
            return true
        }
        return false
    }

    fun speak(text: String, slow: Boolean = false) {
        ttsManager.speak(text, slow)
    }

    fun toggleFavorite(word: WordEntity) {
        val newFav = !word.isFavorite
        _stage1Words.value = _stage1Words.value.map {
            if (it.id == word.id && it.german == word.german) it.copy(isFavorite = newFav) else it
        }
        _hubWords.value = _hubWords.value.map {
            if (it.id == word.id && it.german == word.german) it.copy(isFavorite = newFav) else it
        }
        viewModelScope.launch {
            repository.toggleFavorite(word.id, newFav)
        }
    }

    fun getProgressForSubtopic(subtopicId: String): SubtopicProgressEntity {
        return allProgress.value.firstOrNull { it.subtopicId == subtopicId }
            ?: SubtopicProgressEntity(subtopicId = subtopicId, unlockedStage = 1)
    }

    fun getCompletedStageSetForSubtopic(subtopicId: String): Set<Int> {
        return repository.getCompletedStageSet(
            subtopicId = subtopicId,
            level = _selectedLevel.value,
            fallbackProgress = getProgressForSubtopic(subtopicId)
        )
    }

    fun setHubCardIndex(index: Int) {
        _activeCardIndexInHub.value = index
    }

    // ==========================================
    // === STAGE 1: FLASHCARDS ===
    // ==========================================
    fun startStage1(subtopicId: String) {
        viewModelScope.launch {
            val isFirstPass = 1 !in getCompletedStageSetForSubtopic(subtopicId)
            val rawWords = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 15)
            // Keep curated order on the initial Stage 1 pass; randomize on all subsequent repetitions
            val orderedWords = if (isFirstPass) rawWords else rawWords.shuffled()
            _stage1AllowPhotoChange.value = isFirstPass
            _stage1Words.value = orderedWords
            _stage1Index.value = 0
            _stage1Flipped.value = false
            _stage1Finished.value = false
            _lastCompletedUnlockedSrs.value = false
            stockPhotoRepository.prefetchWordsAsync(orderedWords.take(4))
            navigateToStage(Screen.Stage1Flashcards(subtopicId))
        }
    }

    fun flipStage1Card() {
        _stage1Flipped.value = !_stage1Flipped.value
    }

    fun answerStage1Remembered(subtopicId: String) {
        val words = _stage1Words.value
        val idx = _stage1Index.value
        if (idx in words.indices) {
            val currentWord = words[idx]
            viewModelScope.launch {
                repository.recordReview(currentWord.id, ReviewRating.GOOD)
            }
            if (idx + 1 < words.size) {
                _stage1Index.value = idx + 1
                _stage1Flipped.value = false
            } else {
                _stage1Finished.value = true
                _stage1AllowPhotoChange.value = false
                viewModelScope.launch {
                    val unlocked = repository.completeStage(
                        subtopicId = subtopicId,
                        stage = 1,
                        score = words.size,
                        enabledStages = _enabledStages.value,
                        level = _selectedLevel.value,
                        achieved100Percent = true
                    )
                    _lastCompletedUnlockedSrs.value = unlocked
                    refreshReviewDashboard()
                }
            }
        }
    }

    fun answerStage1ShowAgain(subtopicId: String) {
        val words = _stage1Words.value
        val idx = _stage1Index.value
        if (idx in words.indices) {
            val currentWord = words[idx]
            viewModelScope.launch {
                repository.recordReview(currentWord.id, ReviewRating.AGAIN)
            }
            _stage1Words.value = words + currentWord
            _stage1Index.value = idx + 1
            _stage1Flipped.value = false
        }
    }

    // ==========================================
    // === STAGE 2: LISTENING QUIZ ===
    // ==========================================
    fun startStage2(subtopicId: String) {
        viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12).shuffled()
            val questions = words.map { target ->
                val distractors = repository.getDistractorsForWord(target, count = 3)
                val targetLabel = "${target.displayWithArticle} (${target.translationUk})"
                val distractorLabels = distractors.map { "${it.displayWithArticle} (${it.translationUk})" }
                val options = (distractorLabels + targetLabel).distinct().shuffled()
                val correctIdx = options.indexOf(targetLabel)
                ListeningQ(word = target, options = options, correctIndex = correctIdx)
            }
            _stage2Questions.value = questions
            _stage2Index.value = 0
            _stage2SelectedAnswer.value = null
            _stage2Score.value = 0
            _stage2Finished.value = false
            _lastCompletedUnlockedSrs.value = false
            navigateToStage(Screen.Stage2Listening(subtopicId))
            if (questions.isNotEmpty()) {
                speak(questions.first().word.displayWithArticle)
            }
        }
    }

    fun answerStage2(subtopicId: String, optionIndex: Int) {
        if (_stage2SelectedAnswer.value != null) return
        _stage2SelectedAnswer.value = optionIndex
        val currentQ = _stage2Questions.value.getOrNull(_stage2Index.value) ?: return
        if (optionIndex == currentQ.correctIndex) {
            _stage2Score.value += 1
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
        } else {
            // Cyclic mistake repeat: re-queue the missed word at the end with freshly shuffled options
            val targetLabel = currentQ.options.getOrNull(currentQ.correctIndex)
            val reshuffled = currentQ.options.shuffled()
            val newCorrectIdx = targetLabel?.let { reshuffled.indexOf(it) }?.takeIf { it >= 0 } ?: 0
            _stage2Questions.value = _stage2Questions.value + currentQ.copy(
                options = reshuffled,
                correctIndex = newCorrectIdx,
                isRetry = true
            )
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
        }
    }

    fun nextStage2(subtopicId: String) {
        val nextIdx = _stage2Index.value + 1
        if (nextIdx < _stage2Questions.value.size) {
            _stage2Index.value = nextIdx
            _stage2SelectedAnswer.value = null
            speak(_stage2Questions.value[nextIdx].word.displayWithArticle)
        } else {
            _stage2Finished.value = true
            viewModelScope.launch {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 2,
                    score = _stage2Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 3: MULTIPLE CHOICE QUIZ ===
    // ==========================================
    fun startStage3(subtopicId: String) {
        viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12).shuffled()
            val questions = words.map { target ->
                val distractors = repository.getDistractorsForWord(target, count = 3)
                val targetLabel = "${target.translationUk} · ${target.translationEn}"
                val distractorLabels = distractors.map { "${it.translationUk} · ${it.translationEn}" }
                val options = (distractorLabels + targetLabel).distinct().shuffled()
                val correctIdx = options.indexOf(targetLabel)
                MultipleChoiceQ(
                    word = target,
                    prompt = target.displayWithArticle,
                    options = options,
                    correctIndex = correctIdx
                )
            }
            _stage3Questions.value = questions
            _stage3Index.value = 0
            _stage3SelectedAnswer.value = null
            _stage3Score.value = 0
            _stage3Finished.value = false
            _lastCompletedUnlockedSrs.value = false
            stockPhotoRepository.prefetchWordsAsync(questions.take(4).map { it.word })
            navigateToStage(Screen.Stage3MultipleChoice(subtopicId))
            if (questions.isNotEmpty()) {
                speak(questions.first().word.displayWithArticle)
            }
        }
    }

    fun answerStage3(subtopicId: String, optionIndex: Int) {
        if (_stage3SelectedAnswer.value != null) return
        _stage3SelectedAnswer.value = optionIndex
        val currentQ = _stage3Questions.value.getOrNull(_stage3Index.value) ?: return
        if (optionIndex == currentQ.correctIndex) {
            _stage3Score.value += 1
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
        } else {
            val targetLabel = currentQ.options.getOrNull(currentQ.correctIndex)
            val reshuffled = currentQ.options.shuffled()
            val newCorrectIdx = targetLabel?.let { reshuffled.indexOf(it) }?.takeIf { it >= 0 } ?: 0
            _stage3Questions.value = _stage3Questions.value + currentQ.copy(
                options = reshuffled,
                correctIndex = newCorrectIdx,
                isRetry = true
            )
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
        }
    }

    fun nextStage3(subtopicId: String) {
        val nextIdx = _stage3Index.value + 1
        if (nextIdx < _stage3Questions.value.size) {
            _stage3Index.value = nextIdx
            _stage3SelectedAnswer.value = null
            speak(_stage3Questions.value[nextIdx].word.displayWithArticle)
            stockPhotoRepository.prefetchWordsAsync(
                _stage3Questions.value.drop(nextIdx + 1).take(3).map { it.word }
            )
        } else {
            _stage3Finished.value = true
            viewModelScope.launch {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 3,
                    score = _stage3Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 4: ARTIKEL TRAINER ===
    // ==========================================
    fun startStage4(subtopicId: String) {
        viewModelScope.launch {
            val allSub = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 15)
            val nouns = allSub.filter { it.article.lowercase().trim() in listOf("der", "die", "das") }
            val pool = nouns.ifEmpty {
                allSub.filter { it.article.isNotBlank() }
            }.shuffled()
            val questions = pool.map { ArticleQ(word = it, correctArticle = it.article.lowercase().trim()) }
            _stage4Questions.value = questions
            _stage4Index.value = 0
            _stage4SelectedArticle.value = null
            _stage4Score.value = 0
            _stage4Finished.value = questions.isEmpty()
            _lastCompletedUnlockedSrs.value = false
            stockPhotoRepository.prefetchWordsAsync(questions.take(4).map { it.word })
            navigateToStage(Screen.Stage4ArticleTrainer(subtopicId))
            if (questions.isNotEmpty()) {
                speak(questions.first().word.german)
            } else {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 4,
                    score = 0,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    fun answerStage4(subtopicId: String, article: String) {
        if (_stage4SelectedArticle.value != null) return
        _stage4SelectedArticle.value = article
        val currentQ = _stage4Questions.value.getOrNull(_stage4Index.value) ?: return
        if (article.equals(currentQ.correctArticle, ignoreCase = true)) {
            _stage4Score.value += 1
            speak("${currentQ.correctArticle} ${currentQ.word.german}")
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
        } else {
            _stage4Questions.value = _stage4Questions.value + currentQ.copy(isRetry = true)
            speak("${currentQ.correctArticle} ${currentQ.word.german}")
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
        }
    }

    fun nextStage4(subtopicId: String) {
        val nextIdx = _stage4Index.value + 1
        if (nextIdx < _stage4Questions.value.size) {
            _stage4Index.value = nextIdx
            _stage4SelectedArticle.value = null
            speak(_stage4Questions.value[nextIdx].word.german)
            stockPhotoRepository.prefetchWordsAsync(
                _stage4Questions.value.drop(nextIdx + 1).take(3).map { it.word }
            )
        } else {
            _stage4Finished.value = true
            viewModelScope.launch {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 4,
                    score = _stage4Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 5: PLURAL TRAINER (МНОЖИНА) ===
    // ==========================================
    fun startStage5Plural(subtopicId: String) {
        viewModelScope.launch {
            val allSub = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 15)
            val pluralNouns = allSub.filter { isPluralizableNoun(it) }.shuffled()
            val questions = pluralNouns.map { word ->
                val correct = word.plural.trim()
                val options = generatePluralOptions(word)
                PluralQ(
                    word = word,
                    options = options,
                    correctPlural = correct
                )
            }
            _stage5PluralQuestions.value = questions
            _stage5PluralIndex.value = 0
            _stage5PluralSelected.value = null
            _stage5PluralScore.value = 0
            _stage5PluralAutoSkipped.value = questions.isEmpty()
            _stage5PluralFinished.value = questions.isEmpty()
            _lastCompletedUnlockedSrs.value = false
            stockPhotoRepository.prefetchWordsAsync(questions.take(4).map { it.word })
            navigateToStage(Screen.Stage5PluralTrainer(subtopicId))
            if (questions.isNotEmpty()) {
                speak(questions.first().word.displayWithArticle)
            } else {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 5,
                    score = 0,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    fun answerStage5Plural(subtopicId: String, selectedPlural: String) {
        if (_stage5PluralSelected.value != null) return
        _stage5PluralSelected.value = selectedPlural
        val currentQ = _stage5PluralQuestions.value.getOrNull(_stage5PluralIndex.value) ?: return
        val isCorrect = selectedPlural.equals(currentQ.correctPlural, ignoreCase = true)
        speak(currentQ.correctPlural)
        if (isCorrect) {
            _stage5PluralScore.value += 1
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
        } else {
            _stage5PluralQuestions.value = _stage5PluralQuestions.value + currentQ.copy(
                options = currentQ.options.shuffled(),
                isRetry = true
            )
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
        }
    }

    fun nextStage5Plural(subtopicId: String) {
        val nextIdx = _stage5PluralIndex.value + 1
        if (nextIdx < _stage5PluralQuestions.value.size) {
            _stage5PluralIndex.value = nextIdx
            _stage5PluralSelected.value = null
            speak(_stage5PluralQuestions.value[nextIdx].word.displayWithArticle)
            stockPhotoRepository.prefetchWordsAsync(
                _stage5PluralQuestions.value.drop(nextIdx + 1).take(3).map { it.word }
            )
        } else {
            _stage5PluralFinished.value = true
            viewModelScope.launch {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 5,
                    score = _stage5PluralScore.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 6: FILL IN THE BLANK ===
    // ==========================================
    fun startStage5(subtopicId: String) {
        viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12)
                .filter { it.exampleDe.isNotBlank() }
                .shuffled()
            val questions = words.map { target ->
                val blankSentence = target.exampleDe.replace(
                    Regex(Regex.escape(target.german), RegexOption.IGNORE_CASE),
                    "_________"
                )
                val distractors = repository.getDistractorsForWord(target, count = 3).map { it.german }
                val options = (distractors + target.german).distinct().shuffled()
                FillBlankQ(
                    word = target,
                    sentenceWithBlank = blankSentence,
                    translation = target.exampleUk,
                    options = options,
                    correctOption = target.german
                )
            }
            _stage5Questions.value = questions
            _stage5Index.value = 0
            _stage5SelectedOption.value = null
            _stage5Score.value = 0
            _stage5Finished.value = questions.isEmpty()
            _lastCompletedUnlockedSrs.value = false
            navigateToStage(Screen.Stage6FillBlank(subtopicId))
            if (questions.isEmpty()) {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 6,
                    score = 0,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    fun answerStage5(subtopicId: String, option: String) {
        if (_stage5SelectedOption.value != null) return
        _stage5SelectedOption.value = option
        val currentQ = _stage5Questions.value.getOrNull(_stage5Index.value) ?: return
        if (option.equals(currentQ.correctOption, ignoreCase = true)) {
            _stage5Score.value += 1
            speak(currentQ.word.exampleDe)
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
        } else {
            _stage5Questions.value = _stage5Questions.value + currentQ.copy(
                options = currentQ.options.shuffled(),
                isRetry = true
            )
            speak(currentQ.word.exampleDe)
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
        }
    }

    fun nextStage5(subtopicId: String) {
        val nextIdx = _stage5Index.value + 1
        if (nextIdx < _stage5Questions.value.size) {
            _stage5Index.value = nextIdx
            _stage5SelectedOption.value = null
        } else {
            _stage5Finished.value = true
            viewModelScope.launch {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 6,
                    score = _stage5Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 7: SPELLING & WRITING ===
    // ==========================================
    fun startStage6(subtopicId: String) {
        viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12).shuffled()
            _stage6Words.value = words
            _stage6Index.value = 0
            _stage6Score.value = 0
            _stage6Finished.value = false
            _stage6RetryIndices.value = emptySet()
            _lastCompletedUnlockedSrs.value = false
            prepareStage6Word(0)
            navigateToStage(Screen.Stage7Spelling(subtopicId))
        }
    }

    private fun prepareStage6Word(index: Int) {
        val target = _stage6Words.value.getOrNull(index) ?: return
        _stage6Input.value = ""
        _stage6Status.value = Stage6WordStatus.TYPING
        _stage6AttemptsLeft.value = MAX_STAGE6_ATTEMPTS
        _stage6HintsUsed.value = 0

        val targetLetters = target.german.trim().filter { it != ' ' && it != '-' }.toList()
        val extraLetters = listOf('e', 'a', 'i', 'r', 's', 't', 'n', 'l', 'ä', 'ö', 'ü').shuffled().take(3)
        _stage6LetterPool.value = (targetLetters + extraLetters).shuffled()
        speak(target.displayWithArticle)
    }

    private fun handleStage6WrongAttempt(
        word: WordEntity,
        submitted: String,
        target: String,
        allowLenientUmlauts: Boolean = false
    ) {
        val remaining = (_stage6AttemptsLeft.value - 1).coerceAtLeast(0)
        _stage6AttemptsLeft.value = remaining
        if (remaining > 0) {
            // Keep the matching prefix from the start; erase only the wrong tail (or everything if char 0 is wrong)
            _stage6Input.value = keepCorrectPrefix(submitted, target, allowLenientUmlauts)
            _stage6Status.value = Stage6WordStatus.WRONG_TRY
        } else {
            _stage6Status.value = Stage6WordStatus.OUT_OF_ATTEMPTS
            val newIdx = _stage6Words.value.size
            _stage6Words.value = _stage6Words.value + word
            _stage6RetryIndices.value = _stage6RetryIndices.value + newIdx
            speak(word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(word.id, ReviewRating.AGAIN) }
        }
    }

    fun addStage6Letter(char: Char) {
        if (_stage6Status.value.isResolved) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        val target = word.german.trim()

        val baseInput = _stage6Input.value
        if (baseInput.length >= target.length) return

        val withLeadingSep = autoAdvanceSeparators(baseInput, target)
        val raw = if (withLeadingSep.length < target.length) withLeadingSep + char else withLeadingSep
        val newText = autoAdvanceSeparators(raw, target)
        _stage6Input.value = newText

        if (newText.equals(target, ignoreCase = true)) {
            _stage6Status.value = Stage6WordStatus.CORRECT
            _stage6Score.value += 1
            speak(word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(word.id, ReviewRating.GOOD) }
        } else if (newText.length >= target.length) {
            handleStage6WrongAttempt(word, newText, target)
        } else {
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    fun insertStage6Umlaut(char: Char) {
        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            if (_stage6Status.value.isResolved) return
            val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
            val target = word.german.trim()
            val updated = _stage6Input.value + char
            _stage6Input.value = updated
            if (updated.trim().equals(target, ignoreCase = true)) {
                _stage6Input.value = target
                _stage6Status.value = Stage6WordStatus.CORRECT
                _stage6Score.value += 1
                speak(word.displayWithArticle)
                viewModelScope.launch { repository.recordReview(word.id, ReviewRating.GOOD) }
            } else {
                _stage6Status.value = Stage6WordStatus.TYPING
            }
        } else {
            addStage6Letter(char)
        }
    }

    fun updateStage6TypedText(raw: String) {
        if (_stage6Status.value.isResolved) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        val cleaned = stripOptionalArticlePrefix(raw, word.article)
        _stage6Input.value = cleaned
        _stage6Status.value = Stage6WordStatus.TYPING
    }

    fun submitStage6TypedAnswer() {
        if (_stage6Status.value.isResolved) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        val target = word.german.trim()
        val cleaned = stripOptionalArticlePrefix(_stage6Input.value, word.article).trim()
        if (cleaned.isEmpty()) return

        if (cleaned.equals(target, ignoreCase = true)) {
            _stage6Input.value = target
            _stage6Status.value = Stage6WordStatus.CORRECT
            _stage6Score.value += 1
            speak(word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(word.id, ReviewRating.GOOD) }
        } else if (matchesWithLenientUmlauts(cleaned, target)) {
            _stage6Input.value = cleaned
            _stage6Status.value = Stage6WordStatus.CORRECT_UMLAUT_WARNING
            _stage6Score.value += 1
            speak(word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(word.id, ReviewRating.GOOD) }
        } else {
            handleStage6WrongAttempt(word, cleaned, target, allowLenientUmlauts = true)
        }
    }

    /**
     * Reveals exactly one next correct letter in the input field (fixing any mismatched suffix
     * and automatically inserting spaces/hyphens where needed).
     */
    fun useStage6Hint() {
        if (_stage6Status.value.isResolved) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        val target = word.german.trim()
        val current = _stage6Input.value

        val nextInput = computeNextHintInput(current, target)
        _stage6HintsUsed.value += 1
        _stage6Input.value = nextInput

        if (nextInput.equals(target, ignoreCase = true)) {
            _stage6Status.value = Stage6WordStatus.CORRECT
            _stage6Score.value += 1
            speak(word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(word.id, ReviewRating.GOOD) }
        } else {
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    fun removeStage6Letter() {
        if (_stage6Status.value.isResolved) return
        if (_stage6Input.value.isNotEmpty()) {
            _stage6Input.value = backspaceOverSeparators(_stage6Input.value)
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    fun clearStage6() {
        if (_stage6Status.value.isResolved) return
        _stage6Input.value = ""
        _stage6Status.value = Stage6WordStatus.TYPING
    }

    fun nextStage6(subtopicId: String) {
        val nextIdx = _stage6Index.value + 1
        if (nextIdx < _stage6Words.value.size) {
            _stage6Index.value = nextIdx
            prepareStage6Word(nextIdx)
        } else {
            _stage6Finished.value = true
            viewModelScope.launch {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 7,
                    score = _stage6Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 8: MISSING LETTERS (LÜCKENWORT) ===
    // ==========================================
    fun startStage8MissingLetters(subtopicId: String) {
        viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12)
                .filter { it.german.trim().count { ch -> ch.isLetter() } >= 2 }
                .shuffled()
            val questions = words.map { word ->
                val target = word.german.trim()
                val blanks = buildMissingLettersIndices(target)
                val pool = buildMissingLettersPool(target, blanks)
                MissingLettersQ(
                    word = word,
                    blankIndices = blanks,
                    letterPool = pool
                )
            }
            _stage8Questions.value = questions
            _stage8Index.value = 0
            _stage8Score.value = 0
            _stage8Finished.value = questions.isEmpty()
            _lastCompletedUnlockedSrs.value = false
            prepareStage8Question(0)
            navigateToStage(Screen.Stage8MissingLetters(subtopicId))
            if (questions.isEmpty()) {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 8,
                    score = 0,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    private fun prepareStage8Question(index: Int) {
        val q = _stage8Questions.value.getOrNull(index) ?: return
        _stage8Input.value = ""
        _stage8Status.value = Stage6WordStatus.TYPING
        _stage8AttemptsLeft.value = MAX_STAGE6_ATTEMPTS
        speak(q.word.displayWithArticle)
    }

    private fun expectedMissingString(q: MissingLettersQ): String {
        val target = q.word.german.trim()
        return buildString {
            q.blankIndices.forEach { idx ->
                if (idx in target.indices) append(target[idx])
            }
        }
    }

    private fun handleStage8WrongAttempt(
        q: MissingLettersQ,
        submitted: String,
        expectedMissing: String,
        allowLenientUmlauts: Boolean = false
    ) {
        val remaining = (_stage8AttemptsLeft.value - 1).coerceAtLeast(0)
        _stage8AttemptsLeft.value = remaining
        if (remaining > 0) {
            _stage8Input.value = keepCorrectMissingPrefix(submitted, expectedMissing, allowLenientUmlauts)
            _stage8Status.value = Stage6WordStatus.WRONG_TRY
        } else {
            _stage8Status.value = Stage6WordStatus.OUT_OF_ATTEMPTS
            _stage8Questions.value = _stage8Questions.value + q.copy(
                letterPool = q.letterPool.shuffled(),
                isRetry = true
            )
            speak(q.word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(q.word.id, ReviewRating.AGAIN) }
        }
    }

    fun addStage8Letter(char: Char) {
        if (_stage8Status.value.isResolved) return
        val q = _stage8Questions.value.getOrNull(_stage8Index.value) ?: return
        val expected = expectedMissingString(q)
        if (_stage8Input.value.length >= expected.length) return

        val updated = _stage8Input.value + char
        _stage8Input.value = updated

        if (updated.equals(expected, ignoreCase = true)) {
            _stage8Input.value = expected
            _stage8Status.value = Stage6WordStatus.CORRECT
            _stage8Score.value += 1
            speak(q.word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(q.word.id, ReviewRating.GOOD) }
        } else if (updated.length >= expected.length) {
            handleStage8WrongAttempt(q, updated, expected)
        } else {
            _stage8Status.value = Stage6WordStatus.TYPING
        }
    }

    fun insertStage8Umlaut(char: Char) {
        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            if (_stage8Status.value.isResolved) return
            val q = _stage8Questions.value.getOrNull(_stage8Index.value) ?: return
            val expected = expectedMissingString(q)
            val updated = _stage8Input.value + char
            _stage8Input.value = updated
            if (updated.trim().equals(expected, ignoreCase = true) ||
                updated.trim().equals(q.word.german.trim(), ignoreCase = true)
            ) {
                _stage8Input.value = expected
                _stage8Status.value = Stage6WordStatus.CORRECT
                _stage8Score.value += 1
                speak(q.word.displayWithArticle)
                viewModelScope.launch { repository.recordReview(q.word.id, ReviewRating.GOOD) }
            } else {
                _stage8Status.value = Stage6WordStatus.TYPING
            }
        } else {
            addStage8Letter(char)
        }
    }

    fun updateStage8TypedText(raw: String) {
        if (_stage8Status.value.isResolved) return
        val q = _stage8Questions.value.getOrNull(_stage8Index.value) ?: return
        _stage8Input.value = stripOptionalArticlePrefix(raw, q.word.article)
        _stage8Status.value = Stage6WordStatus.TYPING
    }

    fun submitStage8TypedAnswer() {
        if (_stage8Status.value.isResolved) return
        val q = _stage8Questions.value.getOrNull(_stage8Index.value) ?: return
        val target = q.word.german.trim()
        val expected = expectedMissingString(q)
        val cleaned = stripOptionalArticlePrefix(_stage8Input.value, q.word.article).trim()
        if (cleaned.isEmpty()) return

        when {
            cleaned.equals(expected, ignoreCase = true) || cleaned.equals(target, ignoreCase = true) -> {
                _stage8Input.value = expected
                _stage8Status.value = Stage6WordStatus.CORRECT
                _stage8Score.value += 1
                speak(q.word.displayWithArticle)
                viewModelScope.launch { repository.recordReview(q.word.id, ReviewRating.GOOD) }
            }
            matchesMissingLettersLenient(cleaned, expected, target) -> {
                _stage8Input.value = expected
                _stage8Status.value = Stage6WordStatus.CORRECT_UMLAUT_WARNING
                _stage8Score.value += 1
                speak(q.word.displayWithArticle)
                viewModelScope.launch { repository.recordReview(q.word.id, ReviewRating.GOOD) }
            }
            else -> {
                handleStage8WrongAttempt(q, cleaned, expected, allowLenientUmlauts = true)
            }
        }
    }

    fun useStage8Hint() {
        if (_stage8Status.value.isResolved) return
        val q = _stage8Questions.value.getOrNull(_stage8Index.value) ?: return
        val expected = expectedMissingString(q)
        val nextInput = computeNextMissingHint(_stage8Input.value, expected)
        _stage8Input.value = nextInput
        if (nextInput.equals(expected, ignoreCase = true)) {
            _stage8Status.value = Stage6WordStatus.CORRECT
            _stage8Score.value += 1
            speak(q.word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(q.word.id, ReviewRating.GOOD) }
        } else {
            _stage8Status.value = Stage6WordStatus.TYPING
        }
    }

    fun removeStage8Letter() {
        if (_stage8Status.value.isResolved) return
        if (_stage8Input.value.isNotEmpty()) {
            _stage8Input.value = _stage8Input.value.dropLast(1)
            _stage8Status.value = Stage6WordStatus.TYPING
        }
    }

    fun clearStage8() {
        if (_stage8Status.value.isResolved) return
        _stage8Input.value = ""
        _stage8Status.value = Stage6WordStatus.TYPING
    }

    fun nextStage8MissingLetters(subtopicId: String) {
        val nextIdx = _stage8Index.value + 1
        if (nextIdx < _stage8Questions.value.size) {
            _stage8Index.value = nextIdx
            prepareStage8Question(nextIdx)
        } else {
            _stage8Finished.value = true
            viewModelScope.launch {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 8,
                    score = _stage8Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value,
                    achieved100Percent = true
                )
                _lastCompletedUnlockedSrs.value = unlocked
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === MIXED-STAGE SPACED REPETITION REVIEW ===
    // ==========================================
    fun startMixedReviewSession(earlyPractice: Boolean = false) {
        viewModelScope.launch {
            val doneToday = loadReviewsDoneToday()
            _reviewsDoneToday.value = doneToday
            val remainingToday = (_dailyReviewLimit.value - doneToday).coerceAtLeast(0)
            if (remainingToday <= 0) {
                refreshReviewDashboard()
                return@launch
            }

            val roundCap = if (earlyPractice) minOf(10, remainingToday) else minOf(MIXED_ROUND_SIZE, remainingToday)
            val rawWords = if (earlyPractice) {
                repository.getEarlyWordsForMixedReview(roundCap)
            } else {
                repository.getDueWordsForMixedReview(limit = roundCap)
            }
            if (rawWords.isEmpty()) {
                refreshReviewDashboard()
                return@launch
            }

            val enabled = _enabledStages.value.ifEmpty { ALL_STAGES }
            val shuffledWords = rawWords.shuffled()
            val builtItems = mutableListOf<MixedReviewItem>()
            var lastStageType: Int? = null

            for (word in shuffledWords) {
                val chosenStage = pickMixedStageForWord(word, enabled, lastStageType)
                lastStageType = chosenStage
                val step = repository.getWordSrsStep(word)
                val item = buildMixedReviewItem(word, step, chosenStage)
                builtItems.add(item)
            }

            _mixedReviewItems.value = builtItems
            _mixedReviewIndex.value = 0
            _mixedReviewScore.value = 0
            _mixedReviewFinished.value = false
            _mixedReviewIsEarlyPractice.value = earlyPractice
            resetMixedItemState()
            stockPhotoRepository.prefetchWordsAsync(
                builtItems.filter {
                    it is MixedReviewItem.FlashcardReview ||
                        it is MixedReviewItem.MultipleChoiceReview ||
                        it is MixedReviewItem.ArticleReview ||
                        it is MixedReviewItem.PluralReview
                }.take(4).map { it.word }
            )
            navigateToStage(Screen.MixedReview)
            builtItems.firstOrNull()?.let { speakForMixedItem(it) }
        }
    }

    private suspend fun buildMixedReviewItem(
        word: WordEntity,
        srsStep: Int,
        stageType: Int
    ): MixedReviewItem {
        return when (stageType) {
            1 -> MixedReviewItem.FlashcardReview(word = word, srsStep = srsStep)
            2 -> {
                val distractors = repository.getDistractorsForWord(word, count = 3)
                val targetLabel = "${word.displayWithArticle} (${word.translationUk})"
                val options = (distractors.map { "${it.displayWithArticle} (${it.translationUk})" } + targetLabel)
                    .distinct()
                    .shuffled()
                MixedReviewItem.ListeningReview(
                    word = word,
                    srsStep = srsStep,
                    options = options,
                    correctIndex = options.indexOf(targetLabel).coerceAtLeast(0)
                )
            }
            3 -> {
                val distractors = repository.getDistractorsForWord(word, count = 3)
                val targetLabel = "${word.translationUk} · ${word.translationEn}"
                val options = (distractors.map { "${it.translationUk} · ${it.translationEn}" } + targetLabel)
                    .distinct()
                    .shuffled()
                MixedReviewItem.MultipleChoiceReview(
                    word = word,
                    srsStep = srsStep,
                    options = options,
                    correctIndex = options.indexOf(targetLabel).coerceAtLeast(0)
                )
            }
            4 -> MixedReviewItem.ArticleReview(
                word = word,
                srsStep = srsStep,
                correctArticle = word.article.lowercase().trim()
            )
            5 -> MixedReviewItem.PluralReview(
                word = word,
                srsStep = srsStep,
                options = generatePluralOptions(word),
                correctPlural = word.plural.trim()
            )
            6 -> {
                val blankSentence = word.exampleDe.replace(
                    Regex(Regex.escape(word.german), RegexOption.IGNORE_CASE),
                    "_________"
                )
                val distractors = repository.getDistractorsForWord(word, count = 3).map { it.german }
                val options = (distractors + word.german).distinct().shuffled()
                MixedReviewItem.FillBlankReview(
                    word = word,
                    srsStep = srsStep,
                    sentenceWithBlank = blankSentence,
                    translation = word.exampleUk,
                    options = options,
                    correctOption = word.german
                )
            }
            7 -> {
                val targetLetters = word.german.trim().filter { it != ' ' && it != '-' }.toList()
                val extraLetters = listOf('e', 'a', 'i', 'r', 's', 't', 'n', 'l', 'ä', 'ö', 'ü').shuffled().take(3)
                MixedReviewItem.SpellingReview(
                    word = word,
                    srsStep = srsStep,
                    letterPool = (targetLetters + extraLetters).shuffled()
                )
            }
            else -> {
                val target = word.german.trim()
                val blanks = buildMissingLettersIndices(target)
                val pool = buildMissingLettersPool(target, blanks)
                MixedReviewItem.MissingLettersReview(
                    word = word,
                    srsStep = srsStep,
                    blankIndices = blanks,
                    letterPool = pool
                )
            }
        }
    }

    private fun resetMixedItemState() {
        _mixedCardFlipped.value = false
        _mixedSelectedOptionIndex.value = null
        _mixedSelectedString.value = null
        _mixedSpellingInput.value = ""
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        _mixedSpellingAttemptsLeft.value = MAX_STAGE6_ATTEMPTS
        _mixedLastFeedback.value = null
    }

    private fun speakForMixedItem(item: MixedReviewItem) {
        when (item) {
            is MixedReviewItem.FlashcardReview -> speakIfAuto(item.word.displayWithArticle)
            is MixedReviewItem.ListeningReview -> speak(item.word.displayWithArticle)
            is MixedReviewItem.MultipleChoiceReview -> speak(item.word.displayWithArticle)
            is MixedReviewItem.ArticleReview -> speak(item.word.german)
            is MixedReviewItem.PluralReview -> speak(item.word.displayWithArticle)
            is MixedReviewItem.FillBlankReview -> Unit
            is MixedReviewItem.SpellingReview -> speak(item.word.displayWithArticle)
            is MixedReviewItem.MissingLettersReview -> speak(item.word.displayWithArticle)
        }
    }

    private fun appendMixedRetry(item: MixedReviewItem) {
        val retryItem: MixedReviewItem = when (item) {
            is MixedReviewItem.FlashcardReview -> item.copy(isRetry = true)
            is MixedReviewItem.ListeningReview -> {
                val targetLabel = item.options.getOrNull(item.correctIndex)
                val reshuffled = item.options.shuffled()
                val newIdx = targetLabel?.let { reshuffled.indexOf(it) }?.takeIf { it >= 0 } ?: 0
                item.copy(options = reshuffled, correctIndex = newIdx, isRetry = true)
            }
            is MixedReviewItem.MultipleChoiceReview -> {
                val targetLabel = item.options.getOrNull(item.correctIndex)
                val reshuffled = item.options.shuffled()
                val newIdx = targetLabel?.let { reshuffled.indexOf(it) }?.takeIf { it >= 0 } ?: 0
                item.copy(options = reshuffled, correctIndex = newIdx, isRetry = true)
            }
            is MixedReviewItem.ArticleReview -> item.copy(isRetry = true)
            is MixedReviewItem.PluralReview -> item.copy(options = item.options.shuffled(), isRetry = true)
            is MixedReviewItem.FillBlankReview -> item.copy(options = item.options.shuffled(), isRetry = true)
            is MixedReviewItem.SpellingReview -> item.copy(letterPool = item.letterPool.shuffled(), isRetry = true)
            is MixedReviewItem.MissingLettersReview -> item.copy(letterPool = item.letterPool.shuffled(), isRetry = true)
        }
        _mixedReviewItems.value = _mixedReviewItems.value + retryItem
    }

    private fun recordMixedOutcome(item: MixedReviewItem, isCorrect: Boolean) {
        if (item.isRetry) {
            if (isCorrect) {
                _mixedReviewScore.value += 1
                _mixedLastFeedback.value = "Виправлено! Слово закріплено в цій сесії"
            } else {
                appendMixedRetry(item)
                _mixedLastFeedback.value = "Ще одна помилка — слово додано в кінець для закріплення"
            }
            return
        }

        if (isCorrect) {
            _mixedReviewScore.value += 1
        } else {
            appendMixedRetry(item)
        }
        incrementReviewsDoneToday()
        viewModelScope.launch {
            val res = repository.recordSpacedReview(item.word.id, isCorrect)
            if (res != null) {
                val intervalText = SpacedRepetition.formatIntervalUk(res.intervalHours)
                _mixedLastFeedback.value = when {
                    res.isMastered -> "Слово закріплено (${res.newStep}/${SpacedRepetition.TARGET_REPETITIONS}) · перевірка $intervalText"
                    isCorrect -> "Повтор ${res.newStep} з ${SpacedRepetition.TARGET_REPETITIONS} · наступний раз $intervalText"
                    else -> "Слово додано в кінець цієї сесії · наступний раз $intervalText"
                }
            }
        }
    }

    fun flipMixedFlashcard() {
        _mixedCardFlipped.value = !_mixedCardFlipped.value
    }

    fun answerMixedFlashcard(remembered: Boolean) {
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.FlashcardReview
            ?: return
        recordMixedOutcome(item, remembered)
        nextMixedReviewItem()
    }

    fun answerMixedOptionIndex(optionIndex: Int) {
        if (_mixedSelectedOptionIndex.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) ?: return
        val isCorrect = when (item) {
            is MixedReviewItem.ListeningReview -> optionIndex == item.correctIndex
            is MixedReviewItem.MultipleChoiceReview -> optionIndex == item.correctIndex
            else -> return
        }
        _mixedSelectedOptionIndex.value = optionIndex
        recordMixedOutcome(item, isCorrect)
    }

    fun answerMixedArticle(article: String) {
        if (_mixedSelectedString.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.ArticleReview
            ?: return
        _mixedSelectedString.value = article
        val isCorrect = article.equals(item.correctArticle, ignoreCase = true)
        speak("${item.correctArticle} ${item.word.german}")
        recordMixedOutcome(item, isCorrect)
    }

    fun answerMixedPlural(option: String) {
        if (_mixedSelectedString.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.PluralReview
            ?: return
        _mixedSelectedString.value = option
        val isCorrect = option.equals(item.correctPlural, ignoreCase = true)
        speak(item.correctPlural)
        recordMixedOutcome(item, isCorrect)
    }

    fun answerMixedFillBlank(option: String) {
        if (_mixedSelectedString.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.FillBlankReview
            ?: return
        _mixedSelectedString.value = option
        val isCorrect = option.equals(item.correctOption, ignoreCase = true)
        speak(item.word.exampleDe)
        recordMixedOutcome(item, isCorrect)
    }

    private fun handleMixedSpellingWrongAttempt(
        item: MixedReviewItem.SpellingReview,
        submitted: String,
        target: String,
        allowLenientUmlauts: Boolean = false
    ) {
        val remaining = (_mixedSpellingAttemptsLeft.value - 1).coerceAtLeast(0)
        _mixedSpellingAttemptsLeft.value = remaining
        if (remaining > 0) {
            _mixedSpellingInput.value = keepCorrectPrefix(submitted, target, allowLenientUmlauts)
            _mixedSpellingStatus.value = Stage6WordStatus.WRONG_TRY
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.OUT_OF_ATTEMPTS
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = false)
        }
    }

    fun addMixedSpellingLetter(char: Char) {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        val target = item.word.german.trim()

        val baseInput = _mixedSpellingInput.value
        if (baseInput.length >= target.length) return

        val withLeadingSep = autoAdvanceSeparators(baseInput, target)
        val raw = if (withLeadingSep.length < target.length) withLeadingSep + char else withLeadingSep
        val newText = autoAdvanceSeparators(raw, target)
        _mixedSpellingInput.value = newText

        if (newText.equals(target, ignoreCase = true)) {
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = true)
        } else if (newText.length >= target.length) {
            handleMixedSpellingWrongAttempt(item, newText, target)
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun insertMixedSpellingUmlaut(char: Char) {
        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            if (_mixedSpellingStatus.value.isResolved) return
            val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
                ?: return
            val target = item.word.german.trim()
            val updated = _mixedSpellingInput.value + char
            _mixedSpellingInput.value = updated
            if (updated.trim().equals(target, ignoreCase = true)) {
                _mixedSpellingInput.value = target
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            } else {
                _mixedSpellingStatus.value = Stage6WordStatus.TYPING
            }
        } else {
            addMixedSpellingLetter(char)
        }
    }

    fun updateMixedSpellingTypedText(raw: String) {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        _mixedSpellingInput.value = stripOptionalArticlePrefix(raw, item.word.article)
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
    }

    fun submitMixedSpellingTypedAnswer() {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        val target = item.word.german.trim()
        val cleaned = stripOptionalArticlePrefix(_mixedSpellingInput.value, item.word.article).trim()
        if (cleaned.isEmpty()) return

        if (cleaned.equals(target, ignoreCase = true)) {
            _mixedSpellingInput.value = target
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = true)
        } else if (matchesWithLenientUmlauts(cleaned, target)) {
            _mixedSpellingInput.value = cleaned
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT_UMLAUT_WARNING
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = true)
        } else {
            handleMixedSpellingWrongAttempt(item, cleaned, target, allowLenientUmlauts = true)
        }
    }

    fun useMixedSpellingHint() {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        val target = item.word.german.trim()
        val current = _mixedSpellingInput.value

        val nextInput = computeNextHintInput(current, target)
        _mixedSpellingInput.value = nextInput
        if (nextInput.equals(target, ignoreCase = true)) {
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = true)
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun removeMixedSpellingLetter() {
        if (_mixedSpellingStatus.value.isResolved) return
        if (_mixedSpellingInput.value.isNotEmpty()) {
            _mixedSpellingInput.value = backspaceOverSeparators(_mixedSpellingInput.value)
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun clearMixedSpelling() {
        if (_mixedSpellingStatus.value.isResolved) return
        _mixedSpellingInput.value = ""
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
    }

    // --- Mixed Review: Stage 8 (Missing Letters) ---
    private fun expectedMixedMissingString(item: MixedReviewItem.MissingLettersReview): String {
        val target = item.word.german.trim()
        return buildString {
            item.blankIndices.forEach { idx ->
                if (idx in target.indices) append(target[idx])
            }
        }
    }

    private fun handleMixedMissingWrongAttempt(
        item: MixedReviewItem.MissingLettersReview,
        submitted: String,
        expectedMissing: String,
        allowLenientUmlauts: Boolean = false
    ) {
        val remaining = (_mixedSpellingAttemptsLeft.value - 1).coerceAtLeast(0)
        _mixedSpellingAttemptsLeft.value = remaining
        if (remaining > 0) {
            _mixedSpellingInput.value = keepCorrectMissingPrefix(submitted, expectedMissing, allowLenientUmlauts)
            _mixedSpellingStatus.value = Stage6WordStatus.WRONG_TRY
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.OUT_OF_ATTEMPTS
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = false)
        }
    }

    fun addMixedMissingLetter(char: Char) {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.MissingLettersReview
            ?: return
        val expected = expectedMixedMissingString(item)
        if (_mixedSpellingInput.value.length >= expected.length) return

        val updated = _mixedSpellingInput.value + char
        _mixedSpellingInput.value = updated

        if (updated.equals(expected, ignoreCase = true)) {
            _mixedSpellingInput.value = expected
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = true)
        } else if (updated.length >= expected.length) {
            handleMixedMissingWrongAttempt(item, updated, expected)
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun insertMixedMissingUmlaut(char: Char) {
        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            if (_mixedSpellingStatus.value.isResolved) return
            val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.MissingLettersReview
                ?: return
            val expected = expectedMixedMissingString(item)
            val updated = _mixedSpellingInput.value + char
            _mixedSpellingInput.value = updated
            if (updated.trim().equals(expected, ignoreCase = true) ||
                updated.trim().equals(item.word.german.trim(), ignoreCase = true)
            ) {
                _mixedSpellingInput.value = expected
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            } else {
                _mixedSpellingStatus.value = Stage6WordStatus.TYPING
            }
        } else {
            addMixedMissingLetter(char)
        }
    }

    fun updateMixedMissingTypedText(raw: String) {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.MissingLettersReview
            ?: return
        _mixedSpellingInput.value = stripOptionalArticlePrefix(raw, item.word.article)
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
    }

    fun submitMixedMissingTypedAnswer() {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.MissingLettersReview
            ?: return
        val target = item.word.german.trim()
        val expected = expectedMixedMissingString(item)
        val cleaned = stripOptionalArticlePrefix(_mixedSpellingInput.value, item.word.article).trim()
        if (cleaned.isEmpty()) return

        when {
            cleaned.equals(expected, ignoreCase = true) || cleaned.equals(target, ignoreCase = true) -> {
                _mixedSpellingInput.value = expected
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            }
            matchesMissingLettersLenient(cleaned, expected, target) -> {
                _mixedSpellingInput.value = expected
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT_UMLAUT_WARNING
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            }
            else -> {
                handleMixedMissingWrongAttempt(item, cleaned, expected, allowLenientUmlauts = true)
            }
        }
    }

    fun useMixedMissingHint() {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.MissingLettersReview
            ?: return
        val expected = expectedMixedMissingString(item)
        val nextInput = computeNextMissingHint(_mixedSpellingInput.value, expected)
        _mixedSpellingInput.value = nextInput
        if (nextInput.equals(expected, ignoreCase = true)) {
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = true)
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun removeMixedMissingLetter() {
        if (_mixedSpellingStatus.value.isResolved) return
        if (_mixedSpellingInput.value.isNotEmpty()) {
            _mixedSpellingInput.value = _mixedSpellingInput.value.dropLast(1)
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun clearMixedMissing() {
        if (_mixedSpellingStatus.value.isResolved) return
        _mixedSpellingInput.value = ""
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
    }

    fun nextMixedReviewItem() {
        val nextIdx = _mixedReviewIndex.value + 1
        if (nextIdx < _mixedReviewItems.value.size) {
            _mixedReviewIndex.value = nextIdx
            resetMixedItemState()
            _mixedReviewItems.value.getOrNull(nextIdx)?.let { speakForMixedItem(it) }
            stockPhotoRepository.prefetchWordsAsync(
                _mixedReviewItems.value.drop(nextIdx + 1)
                    .filter {
                        it is MixedReviewItem.FlashcardReview ||
                            it is MixedReviewItem.MultipleChoiceReview ||
                            it is MixedReviewItem.ArticleReview ||
                            it is MixedReviewItem.PluralReview
                    }
                    .take(3)
                    .map { it.word }
            )
        } else {
            _mixedReviewFinished.value = true
            refreshReviewDashboard()
        }
    }

    override fun onCleared() {
        super.onCleared()
        offlineDownloadJob?.cancel()
        ttsManager.shutdown()
    }

    companion object {
        const val MAX_STAGE6_ATTEMPTS = 3
        const val DEFAULT_DAILY_REVIEW_LIMIT = 60
        const val MIXED_ROUND_SIZE = 15
        val ALL_STAGES: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7, 8)
        val DAILY_LIMIT_PRESETS: List<Int> = listOf(40, 60, 80)

        internal fun nextEnabledStage(enabledStages: Set<Int>, afterStage: Int): Int? {
            return enabledStages.filter { it in 1..8 && it > afterStage }.minOrNull()
        }

        internal fun isPluralizableNoun(word: WordEntity): Boolean {
            val art = word.article.lowercase().trim()
            if (art !in listOf("der", "die", "das")) return false
            val pl = word.plural.trim()
            return pl.startsWith("die ", ignoreCase = true) && pl.length > 4
        }

        /**
         * Builds 4 distinct, linguistically plausible German plural choices ("die ...")
         * always containing the exact correct plural form.
         */
        internal fun generatePluralOptions(word: WordEntity): List<String> {
            val correct = word.plural.trim()
            val singular = word.german.trim()
            if (correct.isEmpty()) return listOf("die $singular")

            val baseStem = if (singular.endsWith("e") && singular.length > 2) {
                singular.dropLast(1)
            } else {
                singular
            }
            val umlautedSingular = applyFirstVowelUmlaut(singular)
            val umlautedBase = applyFirstVowelUmlaut(baseStem)

            val rawCandidates = linkedSetOf<String>()
            fun addCandidate(form: String) {
                val cleaned = "die ${form.trim()}"
                if (!cleaned.equals(correct, ignoreCase = true) && form.trim().length >= 2) {
                    rawCandidates.add(cleaned)
                }
            }

            // Plausible German plural endings (-e, -en, -n, -er, -s, unchanged, with/without umlaut)
            if (singular.endsWith("in") && singular.length > 3) {
                addCandidate("${singular}nen")
                addCandidate("${singular}en")
                addCandidate("${singular}e")
                addCandidate("${singular}s")
            }
            addCandidate("${singular}e")
            addCandidate("${singular}en")
            addCandidate("${singular}n")
            addCandidate("${singular}er")
            addCandidate("${singular}s")
            addCandidate(singular)
            if (baseStem != singular) {
                addCandidate("${baseStem}en")
                addCandidate("${baseStem}er")
                addCandidate("${baseStem}e")
            }
            if (umlautedSingular != singular) {
                addCandidate("${umlautedSingular}e")
                addCandidate("${umlautedSingular}er")
                addCandidate(umlautedSingular)
                addCandidate("${umlautedSingular}en")
            }
            if (umlautedBase != baseStem) {
                addCandidate("${umlautedBase}er")
                addCandidate("${umlautedBase}e")
            }

            val distractors = rawCandidates.shuffled().take(3)
            return (distractors + correct).distinct().shuffled()
        }

        private fun applyFirstVowelUmlaut(word: String): String {
            // Find the last stressed stem vowel (a, o, u) before final syllable, or first occurrence
            val idx = word.indexOfLast { it in listOf('a', 'o', 'u', 'A', 'O', 'U') }
            if (idx < 0) return word
            val replacement = when (word[idx]) {
                'a' -> 'ä'
                'o' -> 'ö'
                'u' -> 'ü'
                'A' -> 'Ä'
                'O' -> 'Ö'
                'U' -> 'Ü'
                else -> word[idx]
            }
            return word.substring(0, idx) + replacement + word.substring(idx + 1)
        }

        /**
         * Selects 1..5 non-adjacent-friendly letter indices to hide inside a German word for Stage 8.
         * Keeps index 0 visible when the word has >= 4 letters so the word shape stays recognizable,
         * and prioritizes umlauts/vowels so learners actively recall tricky spelling spots.
         */
        internal fun buildMissingLettersIndices(word: String): List<Int> {
            val trimmed = word.trim()
            val letterIndices = trimmed.indices.filter { trimmed[it].isLetter() }
            if (letterIndices.isEmpty()) return emptyList()
            if (letterIndices.size == 1) return letterIndices

            val targetCount = when (letterIndices.size) {
                2, 3 -> 1
                4, 5 -> 2
                6, 7, 8 -> 3
                9, 10, 11 -> 4
                else -> 5
            }

            val candidateIndices = if (letterIndices.size >= 4) {
                letterIndices.drop(1) // Keep first letter visible for anchor context
            } else {
                letterIndices
            }

            val prioritySet = setOf('ä', 'ö', 'ü', 'ß', 'a', 'e', 'i', 'o', 'u', 'Ä', 'Ö', 'Ü', 'A', 'E', 'I', 'O', 'U')
            val prioritized = candidateIndices.filter { trimmed[it] in prioritySet }.shuffled()
            val others = candidateIndices.filter { trimmed[it] !in prioritySet }.shuffled()
            val ordered = prioritized + others

            val chosen = mutableListOf<Int>()
            // First pass: prefer non-adjacent blanks for visual readability
            for (idx in ordered) {
                if (chosen.size >= targetCount) break
                if (chosen.none { kotlin.math.abs(it - idx) == 1 }) {
                    chosen.add(idx)
                }
            }
            // Second pass: fill remaining slots if needed
            for (idx in ordered) {
                if (chosen.size >= targetCount) break
                if (idx !in chosen) {
                    chosen.add(idx)
                }
            }
            return chosen.sorted()
        }

        internal fun buildMissingLettersPool(word: String, blankIndices: List<Int>): List<Char> {
            val trimmed = word.trim()
            val required = blankIndices.mapNotNull { idx -> trimmed.getOrNull(idx)?.lowercaseChar() }
            val commonExtras = listOf('e', 'a', 'i', 'o', 'u', 'r', 's', 't', 'n', 'l', 'h', 'ä', 'ö', 'ü')
                .filter { it !in required }
                .shuffled()
                .take(4)
            return (required + commonExtras).shuffled()
        }

        internal fun matchesMissingLettersLenient(
            input: String,
            expectedMissing: String,
            fullTarget: String
        ): Boolean {
            return matchesWithLenientUmlauts(input, expectedMissing) ||
                matchesWithLenientUmlauts(input, fullTarget)
        }

        internal fun keepCorrectMissingPrefix(
            input: String,
            expectedMissing: String,
            allowLenientUmlauts: Boolean = false
        ): String {
            return keepCorrectPrefix(input, expectedMissing, allowLenientUmlauts)
        }

        internal fun computeNextMissingHint(current: String, expectedMissing: String): String {
            return computeNextHintInput(current, expectedMissing)
        }

        internal fun pickMixedStageForWord(
            word: WordEntity,
            enabledStages: Set<Int>,
            previousStageType: Int?
        ): Int {
            val eligible = mutableListOf<Int>()
            if (1 in enabledStages) eligible.add(1)
            if (2 in enabledStages) eligible.add(2)
            if (3 in enabledStages) eligible.add(3)
            if (4 in enabledStages && word.article.lowercase().trim() in listOf("der", "die", "das")) {
                eligible.add(4)
            }
            if (5 in enabledStages && isPluralizableNoun(word)) {
                eligible.add(5)
            }
            if (6 in enabledStages &&
                word.exampleDe.isNotBlank() &&
                word.exampleDe.contains(word.german, ignoreCase = true)
            ) {
                eligible.add(6)
            }
            if (7 in enabledStages && word.german.trim().length in 2..24) {
                eligible.add(7)
            }
            if (8 in enabledStages && word.german.trim().count { it.isLetter() } >= 3) {
                eligible.add(8)
            }
            if (eligible.isEmpty()) return enabledStages.minOrNull() ?: 3

            val activeStages = eligible.filter { it != 1 }
            val preferredPool = if (activeStages.isNotEmpty() && eligible.size > 1) {
                activeStages
            } else {
                eligible
            }
            val varied = preferredPool.filter { it != previousStageType }
            return (if (varied.isNotEmpty()) varied else preferredPool).random()
        }

        internal fun autoAdvanceSeparators(input: String, target: String): String {
            var result = input
            while (result.length < target.length && (target[result.length] == ' ' || target[result.length] == '-')) {
                result += target[result.length]
            }
            return result
        }

        internal fun backspaceOverSeparators(input: String): String {
            var trimmed = input
            while (trimmed.isNotEmpty() && (trimmed.last() == ' ' || trimmed.last() == '-')) {
                trimmed = trimmed.dropLast(1)
            }
            if (trimmed.isNotEmpty()) {
                trimmed = trimmed.dropLast(1)
            }
            while (trimmed.isNotEmpty() && (trimmed.last() == ' ' || trimmed.last() == '-')) {
                trimmed = trimmed.dropLast(1)
            }
            return trimmed
        }

        internal fun computeNextHintInput(current: String, target: String): String {
            if (target.isEmpty()) return ""
            var prefixLen = 0
            while (prefixLen < current.length &&
                prefixLen < target.length &&
                current[prefixLen].equals(target[prefixLen], ignoreCase = true)
            ) {
                prefixLen++
            }
            var nextPos = prefixLen
            while (nextPos < target.length && (target[nextPos] == ' ' || target[nextPos] == '-')) {
                nextPos++
            }
            val revealed = if (nextPos < target.length) {
                target.substring(0, nextPos + 1)
            } else {
                target
            }
            return autoAdvanceSeparators(revealed, target)
        }

        /**
         * Returns true if [input] matches [target] when allowing plain base letters or standard
         * German ASCII digraphs in place of umlauts / Eszett (ä -> a/ae, ö -> o/oe, ü -> u/ue, ß -> ss/s).
         * Does NOT forgive spurious umlauts where [target] has plain letters.
         */
        internal fun matchesWithLenientUmlauts(input: String, target: String): Boolean {
            val inLow = input.trim().lowercase()
            val tgtLow = target.trim().lowercase()
            if (inLow.isEmpty() || tgtLow.isEmpty()) return false

            val memo = Array(inLow.length + 1) { IntArray(tgtLow.length + 1) { -1 } }
            fun match(i: Int, j: Int): Boolean {
                if (i == inLow.length && j == tgtLow.length) return true
                if (i == inLow.length || j == tgtLow.length) return false
                val cached = memo[i][j]
                if (cached != -1) return cached == 1

                val t = tgtLow[j]
                val ok = when (t) {
                    'ä' -> (inLow[i] == 'ä' && match(i + 1, j + 1)) ||
                        (inLow[i] == 'a' && match(i + 1, j + 1)) ||
                        (i + 1 < inLow.length && inLow[i] == 'a' && inLow[i + 1] == 'e' && match(i + 2, j + 1))
                    'ö' -> (inLow[i] == 'ö' && match(i + 1, j + 1)) ||
                        (inLow[i] == 'o' && match(i + 1, j + 1)) ||
                        (i + 1 < inLow.length && inLow[i] == 'o' && inLow[i + 1] == 'e' && match(i + 2, j + 1))
                    'ü' -> (inLow[i] == 'ü' && match(i + 1, j + 1)) ||
                        (inLow[i] == 'u' && match(i + 1, j + 1)) ||
                        (i + 1 < inLow.length && inLow[i] == 'u' && inLow[i + 1] == 'e' && match(i + 2, j + 1))
                    'ß' -> (inLow[i] == 'ß' && match(i + 1, j + 1)) ||
                        (i + 1 < inLow.length && inLow[i] == 's' && inLow[i + 1] == 's' && match(i + 2, j + 1)) ||
                        (inLow[i] == 's' && match(i + 1, j + 1))
                    else -> inLow[i] == t && match(i + 1, j + 1)
                }
                memo[i][j] = if (ok) 1 else 0
                return ok
            }
            return match(0, 0)
        }

        /**
         * Retains the longest case-insensitive matching prefix from the start of [input] against
         * [target], returning [target]'s proper casing and advancing past spaces/hyphens.
         * When [allowLenientUmlauts] is true (manual keyboard mode), plain a/o/u/s/ss/ae/oe/ue
         * also advance past matching ä/ö/ü/ß characters.
         * Returns empty string if even the very first character is incorrect.
         */
        internal fun keepCorrectPrefix(
            input: String,
            target: String,
            allowLenientUmlauts: Boolean = false
        ): String {
            if (input.isEmpty() || target.isEmpty()) return ""
            if (!allowLenientUmlauts) {
                var prefixLen = 0
                while (prefixLen < input.length &&
                    prefixLen < target.length &&
                    input[prefixLen].equals(target[prefixLen], ignoreCase = true)
                ) {
                    prefixLen++
                }
                if (prefixLen == 0) return ""
                val matched = target.substring(0, prefixLen)
                return autoAdvanceSeparators(matched, target)
            }

            val inLow = input.lowercase()
            val tgtLow = target.lowercase()
            var i = 0
            var j = 0
            while (i < inLow.length && j < tgtLow.length) {
                val ic = inLow[i]
                val tc = tgtLow[j]
                when {
                    ic == tc -> {
                        i++
                        j++
                    }
                    tc == 'ä' && ic == 'a' -> {
                        i += if (i + 1 < inLow.length && inLow[i + 1] == 'e' && (j + 1 >= tgtLow.length || tgtLow[j + 1] != 'e')) 2 else 1
                        j++
                    }
                    tc == 'ö' && ic == 'o' -> {
                        i += if (i + 1 < inLow.length && inLow[i + 1] == 'e' && (j + 1 >= tgtLow.length || tgtLow[j + 1] != 'e')) 2 else 1
                        j++
                    }
                    tc == 'ü' && ic == 'u' -> {
                        i += if (i + 1 < inLow.length && inLow[i + 1] == 'e' && (j + 1 >= tgtLow.length || tgtLow[j + 1] != 'e')) 2 else 1
                        j++
                    }
                    tc == 'ß' && ic == 's' -> {
                        i += if (i + 1 < inLow.length && inLow[i + 1] == 's' && (j + 1 >= tgtLow.length || tgtLow[j + 1] != 's')) 2 else 1
                        j++
                    }
                    else -> break
                }
            }
            if (j == 0) return ""
            val matched = target.substring(0, j)
            return autoAdvanceSeparators(matched, target)
        }

        /**
         * Strips an optional leading article ("der ", "die ", "das ") when typing in keyboard mode,
         * since the article badge is already displayed automatically before the input field.
         */
        internal fun stripOptionalArticlePrefix(input: String, article: String): String {
            val trimmed = input.trimStart()
            val cleanArticle = article.trim()
            if (cleanArticle.isEmpty()) return trimmed
            val prefix = "$cleanArticle "
            return if (trimmed.startsWith(prefix, ignoreCase = true)) {
                trimmed.substring(prefix.length).trimStart()
            } else {
                trimmed
            }
        }
    }
}

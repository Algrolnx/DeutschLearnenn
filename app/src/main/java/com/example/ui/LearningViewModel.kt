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
import com.example.data.repository.VocabularyRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.ReviewRating
import com.example.util.SpacedRepetition
import com.example.util.TtsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

sealed class Screen {
    object MainTopics : Screen()
    data class SubtopicsList(val mainTopicId: String) : Screen()
    data class SubtopicHub(val subtopicId: String) : Screen()

    // 6-Stage Learning Progression
    data class Stage1Flashcards(val subtopicId: String) : Screen()
    data class Stage2Listening(val subtopicId: String) : Screen()
    data class Stage3MultipleChoice(val subtopicId: String) : Screen()
    data class Stage4ArticleTrainer(val subtopicId: String) : Screen()
    data class Stage5FillBlank(val subtopicId: String) : Screen()
    data class Stage6Spelling(val subtopicId: String) : Screen()

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

    data class FlashcardReview(
        override val word: WordEntity,
        override val srsStep: Int
    ) : MixedReviewItem() {
        override val stageType: Int = 1
    }

    data class ListeningReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val options: List<String>,
        val correctIndex: Int
    ) : MixedReviewItem() {
        override val stageType: Int = 2
    }

    data class MultipleChoiceReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val options: List<String>,
        val correctIndex: Int
    ) : MixedReviewItem() {
        override val stageType: Int = 3
    }

    data class ArticleReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val correctArticle: String
    ) : MixedReviewItem() {
        override val stageType: Int = 4
    }

    data class FillBlankReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val sentenceWithBlank: String,
        val translation: String,
        val options: List<String>,
        val correctOption: String
    ) : MixedReviewItem() {
        override val stageType: Int = 5
    }

    data class SpellingReview(
        override val word: WordEntity,
        override val srsStep: Int,
        val letterPool: List<Char>
    ) : MixedReviewItem() {
        override val stageType: Int = 6
    }
}

data class MultipleChoiceQ(
    val word: WordEntity,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

data class ListeningQ(
    val word: WordEntity,
    val options: List<String>,
    val correctIndex: Int
)

data class ArticleQ(
    val word: WordEntity,
    val correctArticle: String
)

data class FillBlankQ(
    val word: WordEntity,
    val sentenceWithBlank: String,
    val translation: String,
    val options: List<String>,
    val correctOption: String
)

data class IoStatusMessage(
    val text: String,
    val isError: Boolean = false
)

enum class Stage6WordStatus {
    TYPING,
    WRONG_TRY,
    CORRECT,
    OUT_OF_ATTEMPTS
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LearningViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
    private val repository: VocabularyRepository
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
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
        _ioStatusMessage.value = null
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

    // === Customizable Enabled Learning & Review Stages ===
    private fun loadEnabledStages(): Set<Int> {
        val raw = prefs.getString("enabled_stages_csv", null) ?: return ALL_STAGES
        val parsed = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..6 }
            .toSet()
        return parsed.ifEmpty { ALL_STAGES }
    }

    private val _enabledStages = MutableStateFlow(loadEnabledStages())
    val enabledStages: StateFlow<Set<Int>> = _enabledStages.asStateFlow()

    fun toggleStageEnabled(stage: Int) {
        if (stage !in 1..6) return
        val current = _enabledStages.value
        val updated = if (stage in current) {
            if (current.size <= 1) return // Always keep at least one stage active
            current - stage
        } else {
            current + stage
        }
        _enabledStages.value = updated
        prefs.edit().putString("enabled_stages_csv", updated.sorted().joinToString(",")).apply()
    }

    fun getFirstEnabledStage(): Int = _enabledStages.value.minOrNull() ?: 1

    fun getNextEnabledStage(afterStage: Int): Int? = nextEnabledStage(_enabledStages.value, afterStage)

    fun startStageByNumber(subtopicId: String, stage: Int) {
        when (stage) {
            1 -> startStage1(subtopicId)
            2 -> startStage2(subtopicId)
            3 -> startStage3(subtopicId)
            4 -> startStage4(subtopicId)
            5 -> startStage5(subtopicId)
            6 -> startStage6(subtopicId)
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

    // === Stage 5: Fill in the Blank State ===
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

    // === Stage 6: Spelling & Writing State ===
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
        is Screen.Stage5FillBlank,
        is Screen.Stage6Spelling,
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

    fun setHubCardIndex(index: Int) {
        _activeCardIndexInHub.value = index
    }

    // ==========================================
    // === STAGE 1: FLASHCARDS ===
    // ==========================================
    fun startStage1(subtopicId: String) {
        viewModelScope.launch {
            val isFirstPass = getProgressForSubtopic(subtopicId).completedStages < 1
            val rawWords = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 15)
            // Keep curated order on the initial Stage 1 pass; randomize on all subsequent repetitions
            val orderedWords = if (isFirstPass) rawWords else rawWords.shuffled()
            _stage1AllowPhotoChange.value = isFirstPass
            _stage1Words.value = orderedWords
            _stage1Index.value = 0
            _stage1Flipped.value = false
            _stage1Finished.value = false
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
                    repository.completeStage(
                        subtopicId = subtopicId,
                        stage = 1,
                        score = words.size,
                        enabledStages = _enabledStages.value,
                        level = _selectedLevel.value
                    )
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
                repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 2,
                    score = _stage2Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value
                )
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
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
        }
    }

    fun nextStage3(subtopicId: String) {
        val nextIdx = _stage3Index.value + 1
        if (nextIdx < _stage3Questions.value.size) {
            _stage3Index.value = nextIdx
            _stage3SelectedAnswer.value = null
            speak(_stage3Questions.value[nextIdx].word.displayWithArticle)
        } else {
            _stage3Finished.value = true
            viewModelScope.launch {
                repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 3,
                    score = _stage3Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value
                )
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
            _stage4Finished.value = false
            navigateToStage(Screen.Stage4ArticleTrainer(subtopicId))
            if (questions.isNotEmpty()) {
                speak(questions.first().word.german)
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
        } else {
            _stage4Finished.value = true
            viewModelScope.launch {
                repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 4,
                    score = _stage4Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value
                )
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 5: FILL IN THE BLANK ===
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
            _stage5Finished.value = false
            navigateToStage(Screen.Stage5FillBlank(subtopicId))
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
                repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 5,
                    score = _stage5Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value
                )
                refreshReviewDashboard()
            }
        }
    }

    // ==========================================
    // === STAGE 6: SPELLING & WRITING ===
    // ==========================================
    fun startStage6(subtopicId: String) {
        viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12).shuffled()
            _stage6Words.value = words
            _stage6Index.value = 0
            _stage6Score.value = 0
            _stage6Finished.value = false
            prepareStage6Word(0)
            navigateToStage(Screen.Stage6Spelling(subtopicId))
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

    fun addStage6Letter(char: Char) {
        val currentStatus = _stage6Status.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        val target = word.german.trim()

        val baseInput = if (currentStatus == Stage6WordStatus.WRONG_TRY) "" else _stage6Input.value
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
            val remaining = (_stage6AttemptsLeft.value - 1).coerceAtLeast(0)
            _stage6AttemptsLeft.value = remaining
            if (remaining > 0) {
                _stage6Status.value = Stage6WordStatus.WRONG_TRY
            } else {
                _stage6Status.value = Stage6WordStatus.OUT_OF_ATTEMPTS
                speak(word.displayWithArticle)
                viewModelScope.launch { repository.recordReview(word.id, ReviewRating.AGAIN) }
            }
        } else {
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    /**
     * Reveals exactly one next correct letter in the input field (fixing any mismatched suffix
     * and automatically inserting spaces/hyphens where needed).
     */
    fun useStage6Hint() {
        val currentStatus = _stage6Status.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        val target = word.german.trim()
        val current = if (currentStatus == Stage6WordStatus.WRONG_TRY) "" else _stage6Input.value

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
        val currentStatus = _stage6Status.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
        if (_stage6Input.value.isNotEmpty()) {
            _stage6Input.value = backspaceOverSeparators(_stage6Input.value)
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    fun clearStage6() {
        val currentStatus = _stage6Status.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
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
                repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 6,
                    score = _stage6Score.value,
                    enabledStages = _enabledStages.value,
                    level = _selectedLevel.value
                )
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
            5 -> {
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
            else -> {
                val targetLetters = word.german.trim().filter { it != ' ' && it != '-' }.toList()
                val extraLetters = listOf('e', 'a', 'i', 'r', 's', 't', 'n', 'l', 'ä', 'ö', 'ü').shuffled().take(3)
                MixedReviewItem.SpellingReview(
                    word = word,
                    srsStep = srsStep,
                    letterPool = (targetLetters + extraLetters).shuffled()
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
            is MixedReviewItem.FillBlankReview -> Unit
            is MixedReviewItem.SpellingReview -> speak(item.word.displayWithArticle)
        }
    }

    private fun recordMixedOutcome(word: WordEntity, isCorrect: Boolean) {
        if (isCorrect) {
            _mixedReviewScore.value += 1
        }
        incrementReviewsDoneToday()
        viewModelScope.launch {
            val res = repository.recordSpacedReview(word.id, isCorrect)
            if (res != null) {
                val intervalText = SpacedRepetition.formatIntervalUk(res.intervalHours)
                _mixedLastFeedback.value = when {
                    res.isMastered -> "Слово закріплено (${res.newStep}/${SpacedRepetition.TARGET_REPETITIONS}) · перевірка $intervalText"
                    isCorrect -> "Повтор ${res.newStep} з ${SpacedRepetition.TARGET_REPETITIONS} · наступний раз $intervalText"
                    else -> "Повернемося до слова $intervalText (крок ${res.newStep}/${SpacedRepetition.TARGET_REPETITIONS})"
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
        recordMixedOutcome(item.word, remembered)
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
        recordMixedOutcome(item.word, isCorrect)
    }

    fun answerMixedArticle(article: String) {
        if (_mixedSelectedString.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.ArticleReview
            ?: return
        _mixedSelectedString.value = article
        val isCorrect = article.equals(item.correctArticle, ignoreCase = true)
        speak("${item.correctArticle} ${item.word.german}")
        recordMixedOutcome(item.word, isCorrect)
    }

    fun answerMixedFillBlank(option: String) {
        if (_mixedSelectedString.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.FillBlankReview
            ?: return
        _mixedSelectedString.value = option
        val isCorrect = option.equals(item.correctOption, ignoreCase = true)
        speak(item.word.exampleDe)
        recordMixedOutcome(item.word, isCorrect)
    }

    fun addMixedSpellingLetter(char: Char) {
        val currentStatus = _mixedSpellingStatus.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        val target = item.word.german.trim()

        val baseInput = if (currentStatus == Stage6WordStatus.WRONG_TRY) "" else _mixedSpellingInput.value
        if (baseInput.length >= target.length) return

        val withLeadingSep = autoAdvanceSeparators(baseInput, target)
        val raw = if (withLeadingSep.length < target.length) withLeadingSep + char else withLeadingSep
        val newText = autoAdvanceSeparators(raw, target)
        _mixedSpellingInput.value = newText

        if (newText.equals(target, ignoreCase = true)) {
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item.word, isCorrect = true)
        } else if (newText.length >= target.length) {
            val remaining = (_mixedSpellingAttemptsLeft.value - 1).coerceAtLeast(0)
            _mixedSpellingAttemptsLeft.value = remaining
            if (remaining > 0) {
                _mixedSpellingStatus.value = Stage6WordStatus.WRONG_TRY
            } else {
                _mixedSpellingStatus.value = Stage6WordStatus.OUT_OF_ATTEMPTS
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item.word, isCorrect = false)
            }
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun useMixedSpellingHint() {
        val currentStatus = _mixedSpellingStatus.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        val target = item.word.german.trim()
        val current = if (currentStatus == Stage6WordStatus.WRONG_TRY) "" else _mixedSpellingInput.value

        val nextInput = computeNextHintInput(current, target)
        _mixedSpellingInput.value = nextInput
        if (nextInput.equals(target, ignoreCase = true)) {
            _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item.word, isCorrect = true)
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun removeMixedSpellingLetter() {
        val currentStatus = _mixedSpellingStatus.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
        if (_mixedSpellingInput.value.isNotEmpty()) {
            _mixedSpellingInput.value = backspaceOverSeparators(_mixedSpellingInput.value)
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun clearMixedSpelling() {
        val currentStatus = _mixedSpellingStatus.value
        if (currentStatus == Stage6WordStatus.CORRECT || currentStatus == Stage6WordStatus.OUT_OF_ATTEMPTS) return
        _mixedSpellingInput.value = ""
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
    }

    fun nextMixedReviewItem() {
        val nextIdx = _mixedReviewIndex.value + 1
        if (nextIdx < _mixedReviewItems.value.size) {
            _mixedReviewIndex.value = nextIdx
            resetMixedItemState()
            _mixedReviewItems.value.getOrNull(nextIdx)?.let { speakForMixedItem(it) }
        } else {
            _mixedReviewFinished.value = true
            refreshReviewDashboard()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    companion object {
        const val MAX_STAGE6_ATTEMPTS = 3
        const val DEFAULT_DAILY_REVIEW_LIMIT = 60
        const val MIXED_ROUND_SIZE = 15
        val ALL_STAGES: Set<Int> = setOf(1, 2, 3, 4, 5, 6)
        val DAILY_LIMIT_PRESETS: List<Int> = listOf(40, 60, 80)

        internal fun nextEnabledStage(enabledStages: Set<Int>, afterStage: Int): Int? {
            return enabledStages.filter { it in 1..6 && it > afterStage }.minOrNull()
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
            if (5 in enabledStages &&
                word.exampleDe.isNotBlank() &&
                word.exampleDe.contains(word.german, ignoreCase = true)
            ) {
                eligible.add(5)
            }
            if (6 in enabledStages && word.german.trim().length in 2..24) {
                eligible.add(6)
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
    }
}

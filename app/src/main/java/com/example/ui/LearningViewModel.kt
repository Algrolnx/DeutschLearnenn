package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.BoxCount
import com.example.data.db.LevelStats
import com.example.data.db.SubtopicLevelCount
import com.example.data.model.ImportantSentence
import com.example.data.model.SentenceSituationGroup
import com.example.data.model.SubtopicProgressEntity
import com.example.data.model.TopicCatalog
import com.example.data.model.WordEntity
import com.example.data.repository.ImportantSentencesRepository
import com.example.data.repository.StockPhotoRepository
import com.example.data.repository.VocabularyRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.ReviewRating
import com.example.util.SpacedRepetition
import com.example.util.TtsManager
import com.example.util.WordExampleGenerator
import com.example.util.WordExampleSentence
import java.util.concurrent.ConcurrentHashMap
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

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
    data class Stage8FinalMix(val subtopicId: String) : Screen()

    object MixedReview : Screen()
    object Progress : Screen()
    data class GrammarGuide(val initialTopicId: String? = null) : Screen()
    data class SentenceSituation(val level: String, val situationId: String) : Screen()
}

enum class MainTab {
    TOPICS,
    REVIEW,
    GRAMMAR,
    FAVORITES
}

enum class TopicsContentMode {
    WORDS,
    SENTENCES
}

enum class FillBlankInputMode {
    OPTIONS,
    KEYBOARD
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
        val matchedToken: String,
        val translation: String,
        val options: List<String>,
        val correctOption: String,
        val fullSentence: String = word.exampleDe,
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
    val matchedToken: String,
    val translation: String,
    val options: List<String>,
    val correctOption: String,
    val fullSentence: String = word.exampleDe,
    val isRetry: Boolean = false
)

data class FillBlankMatch(
    val sentenceWithBlank: String,
    val matchedToken: String
)

sealed class FillBlankKeyboardEvaluation {
    data class ExactMatch(val canonical: String) : FillBlankKeyboardEvaluation()
    data class LenientUmlautMatch(val canonical: String) : FillBlankKeyboardEvaluation()
    data class WrongInput(val keptPrefix: String) : FillBlankKeyboardEvaluation()
}

sealed class KeyboardSpellingEvaluation {
    data class ExactMatch(val canonical: String) : KeyboardSpellingEvaluation()
    data class LenientUmlautMatch(val cleanedInput: String) : KeyboardSpellingEvaluation()
    data class MissingArticle(val preservedText: String) : KeyboardSpellingEvaluation()
    data class WrongArticle(val typedArticle: String, val remainder: String) : KeyboardSpellingEvaluation()
    data class WrongSpelling(val keptPrefix: String, val articleWasCorrect: Boolean) : KeyboardSpellingEvaluation()
}

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
    val progressFraction: Float = 0f,
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
    private val sentencesRepository: ImportantSentencesRepository = ImportantSentencesRepository(application)
    private val wordExampleRotation = ConcurrentHashMap<String, Int>()
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

    private val _streakDays = MutableStateFlow(
        computeCurrentStreak(
            savedStreak = prefs.getInt("streak_days", 0),
            lastActiveDate = prefs.getString("streak_last_active_date", null),
            today = todayDateKey(),
            yesterday = yesterdayDateKey()
        )
    )
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private fun recordDailyStudyActivity() {
        val today = todayDateKey()
        val yesterday = yesterdayDateKey()
        val savedStreak = prefs.getInt("streak_days", 0)
        val lastDate = prefs.getString("streak_last_active_date", null)
        val updated = computeUpdatedStreak(savedStreak, lastDate, today, yesterday)
        if (updated != savedStreak || lastDate != today) {
            prefs.edit()
                .putInt("streak_days", updated)
                .putString("streak_last_active_date", today)
                .apply()
        }
        _streakDays.value = updated
    }

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

    // === Stage 6 (Fill Blank) Input Mode (Options vs Keyboard) ===
    private val initialStage5Mode = FillBlankInputMode.entries.firstOrNull {
        it.name == prefs.getString("stage5_fillblank_input_mode", FillBlankInputMode.OPTIONS.name)
    } ?: FillBlankInputMode.OPTIONS

    private val _stage5InputMode = MutableStateFlow(initialStage5Mode)
    val stage5InputMode: StateFlow<FillBlankInputMode> = _stage5InputMode.asStateFlow()

    fun setStage5InputMode(mode: FillBlankInputMode) {
        _stage5InputMode.value = mode
        prefs.edit().putString("stage5_fillblank_input_mode", mode.name).apply()
    }

    // === Stage 7 (Spelling) Input Mode (Letter Tiles vs Keyboard) ===
    private val initialStage6Mode = Stage6InputMode.entries.firstOrNull {
        it.name == prefs.getString("stage6_input_mode", Stage6InputMode.LETTER_TILES.name)
    } ?: Stage6InputMode.LETTER_TILES

    private val _stage6InputMode = MutableStateFlow(initialStage6Mode)
    val stage6InputMode: StateFlow<Stage6InputMode> = _stage6InputMode.asStateFlow()

    fun setStage6InputMode(mode: Stage6InputMode) {
        _stage6InputMode.value = mode
        prefs.edit().putString("stage6_input_mode", mode.name).apply()
    }

    // === Topics Tab Content Mode (Слова vs Речення) & Important Sentences ===
    private val _topicsContentMode = MutableStateFlow(TopicsContentMode.WORDS)
    val topicsContentMode: StateFlow<TopicsContentMode> = _topicsContentMode.asStateFlow()

    fun setTopicsContentMode(mode: TopicsContentMode) {
        _topicsContentMode.value = mode
    }

    private val _learnedSentenceIds = MutableStateFlow(sentencesRepository.getLearnedIds())
    val learnedSentenceIds: StateFlow<Set<Int>> = _learnedSentenceIds.asStateFlow()

    private val _favoriteSentenceIds = MutableStateFlow(sentencesRepository.getFavoriteIds())
    val favoriteSentenceIds: StateFlow<Set<Int>> = _favoriteSentenceIds.asStateFlow()

    fun getSentencesForLevel(level: String): List<ImportantSentence> =
        sentencesRepository.getSentencesByLevel(level)

    fun getSituationGroupsForLevel(level: String): List<SentenceSituationGroup> =
        sentencesRepository.getSituationGroupsByLevel(level)

    fun getSituationGroup(level: String, situationId: String): SentenceSituationGroup? =
        sentencesRepository.getSituationGroupsByLevel(level).firstOrNull { it.situationId == situationId }

    fun searchSentences(query: String, preferredLevel: String): List<ImportantSentence> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val normLevel = preferredLevel.uppercase()
        val matches = sentencesRepository.getAllSentences().filter { s ->
            s.german.lowercase().contains(q) ||
                s.ukrainian.lowercase().contains(q) ||
                s.situationUk.lowercase().contains(q) ||
                s.situationDe.lowercase().contains(q) ||
                s.noteUk.lowercase().contains(q)
        }
        return matches.sortedBy { if (it.level == normLevel) 0 else 1 }.take(40)
    }

    fun toggleSentenceLearned(sentenceId: Int) {
        val current = _learnedSentenceIds.value
        val updated = if (sentenceId in current) {
            current - sentenceId
        } else {
            recordDailyStudyActivity()
            current + sentenceId
        }
        _learnedSentenceIds.value = updated
        sentencesRepository.saveLearnedIds(updated)
    }

    fun toggleSentenceFavorite(sentenceId: Int) {
        val current = _favoriteSentenceIds.value
        val updated = if (sentenceId in current) current - sentenceId else current + sentenceId
        _favoriteSentenceIds.value = updated
        sentencesRepository.saveFavoriteIds(updated)
    }

    private fun nextRotatedFillBlankSentence(word: WordEntity): Pair<WordExampleSentence, FillBlankMatch>? {
        val examples = WordExampleGenerator.buildExamplesForWord(word)
        if (examples.isEmpty()) return null
        val key = "${word.id}_${word.german.lowercase()}"
        val startIdx = wordExampleRotation.compute(key) { _, prev ->
            if (prev == null) 0 else (prev + 1) % examples.size
        } ?: 0
        for (offset in examples.indices) {
            val candidate = examples[(startIdx + offset) % examples.size]
            val match = buildFillBlankMatch(word, candidate.de)
            if (match != null) return candidate to match
        }
        return null
    }

    // === Offline Level Preload (Photos + HD Pronunciation) ===
    private var offlineDownloadJob: Job? = null
    private val offlineDownloadSessionId = AtomicLong(0L)
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
        val sessionId = offlineDownloadSessionId.incrementAndGet()
        offlineDownloadJob?.cancel()
        offlineDownloadJob = viewModelScope.launch {
            val allLevelWords = repository.getWordsByLevelOnce(normLevel)
            if (allLevelWords.isEmpty()) {
                if (offlineDownloadSessionId.get() == sessionId) {
                    _offlineDownloadState.value = _offlineDownloadState.value.copy(
                        isRunning = false,
                        isPausedForRateLimit = false,
                        activeLevel = null,
                        progressFraction = 0f,
                        statusMessage = "Немає слів для рівня $normLevel"
                    )
                }
                return@launch
            }

            // Strictly follow the menu order (Topic 1 & all its subtopics first, then Topic 2, etc.)
            val orderedWords = orderWordsByTopicCatalog(allLevelWords)

            val totalWords = orderedWords.size
            val hasPhotoApi = stockPhotoRepository.hasConfiguredApiKey()
            val latchTarget = if (hasPhotoApi) 2 else 1
            val totalSteps = (totalWords * latchTarget).coerceAtLeast(1)

            val audioDoneCount = AtomicInteger(0)
            val audioReadyCount = AtomicInteger(0)
            val photoDoneCount = AtomicInteger(0)
            val photosReadyCount = AtomicInteger(0)
            val wordsCompletedCount = AtomicInteger(0)
            val wordLatches = Array(totalWords) { AtomicInteger(0) }
            val rateLimitWaitSec = AtomicInteger(0)
            val currentTopicLabel = AtomicReference("")
            val lastUiPublishMs = AtomicLong(0L)

            fun publishProgress(force: Boolean = false) {
                if (offlineDownloadSessionId.get() != sessionId) return
                val now = System.currentTimeMillis()
                if (!force && now - lastUiPublishMs.get() < 120L) return
                lastUiPublishMs.set(now)

                val doneAudio = audioDoneCount.get()
                val donePhotos = if (hasPhotoApi) photoDoneCount.get() else 0
                val completedWords = wordsCompletedCount.get()
                val fraction = ((doneAudio + donePhotos).toFloat() / totalSteps).coerceIn(0f, 1f)
                val waitSec = rateLimitWaitSec.get()
                val topicSuffix = currentTopicLabel.get().takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()

                val statusText = if (waitSec > 0) {
                    if (doneAudio < totalWords) {
                        "Рівень $normLevel: аудіо триває · пауза ліміту Pixabay (~$waitSec с)"
                    } else {
                        "Рівень $normLevel: пауза ліміту Pixabay (~$waitSec с)$topicSuffix"
                    }
                } else {
                    "Рівень $normLevel: $completedWords / $totalWords слів$topicSuffix"
                }

                _offlineDownloadState.value = _offlineDownloadState.value.copy(
                    processedWords = completedWords,
                    totalWords = totalWords,
                    photosReady = photosReadyCount.get(),
                    audioReady = audioReadyCount.get(),
                    progressFraction = fraction,
                    isPausedForRateLimit = waitSec > 0,
                    statusMessage = statusText
                )
            }

            _offlineDownloadState.value = _offlineDownloadState.value.copy(
                activeLevel = normLevel,
                processedWords = 0,
                totalWords = totalWords,
                photosReady = 0,
                audioReady = 0,
                progressFraction = 0f,
                isPausedForRateLimit = false,
                isRunning = true,
                statusMessage = "Завантаження рівня $normLevel (починаючи з Теми 1)..."
            )

            try {
                // Run HD audio pool (10 parallel workers) and photo pool (3 parallel workers) concurrently
                // so audio never blocks photos and Pixabay's 100 req/min limit is utilized cleanly without 429 storms.
                coroutineScope {
                    val audioCursor = AtomicInteger(0)
                    val audioWorkers = List(10) {
                        launch(Dispatchers.IO) {
                            while (isActive) {
                                val idx = audioCursor.getAndIncrement()
                                if (idx >= totalWords) break
                                ensureActive()
                                val word = orderedWords[idx]
                                val audioWithArt = ttsManager.prefetchHdAudio(word.displayWithArticle)
                                if (word.article.isNotBlank() &&
                                    !word.german.equals(word.displayWithArticle, ignoreCase = true)
                                ) {
                                    ensureActive()
                                    ttsManager.prefetchHdAudio(word.german)
                                }
                                if (isPluralizableNoun(word)) {
                                    ensureActive()
                                    ttsManager.prefetchHdAudio(word.plural.trim())
                                }
                                if (audioWithArt) audioReadyCount.incrementAndGet()
                                audioDoneCount.incrementAndGet()
                                if (wordLatches[idx].incrementAndGet() >= latchTarget) {
                                    wordsCompletedCount.incrementAndGet()
                                }
                                publishProgress()
                            }
                        }
                    }

                    val photoWorkers = if (hasPhotoApi) {
                        val photoCursor = AtomicInteger(0)
                        List(3) {
                            launch(Dispatchers.IO) {
                                while (isActive) {
                                    val idx = photoCursor.getAndIncrement()
                                    if (idx >= totalWords) break
                                    ensureActive()
                                    val word = orderedWords[idx]
                                    TopicCatalog.findSubtopicById(word.subtopicId)?.let { sub ->
                                        currentTopicLabel.set(sub.titleUk)
                                    }

                                    val photoOk = if (stockPhotoRepository.getLocalPhotoFile(word) != null) {
                                        true
                                    } else {
                                        var loaded = false
                                        for (attempt in 0 until 6) {
                                            ensureActive()
                                            // If no cached URL yet, respect active Pixabay cooldown before hitting API
                                            if (stockPhotoRepository.getCachedSync(word) == null) {
                                                var waitMs = stockPhotoRepository.remainingCooldownMs()
                                                while (waitMs > 0L && isActive) {
                                                    val sec = ((waitMs + 999L) / 1000L).toInt().coerceAtLeast(1)
                                                    rateLimitWaitSec.set(sec)
                                                    publishProgress(force = true)
                                                    delay(minOf(1000L, waitMs))
                                                    waitMs = stockPhotoRepository.remainingCooldownMs()
                                                }
                                            }
                                            ensureActive()
                                            rateLimitWaitSec.set(0)
                                            loaded = stockPhotoRepository.preloadWordForOffline(word, fastBulk = true)
                                            if (loaded) break
                                            // Only retry if we hit a Pixabay rate-limit cooldown
                                            val cooldown = stockPhotoRepository.remainingCooldownMs()
                                            if (cooldown <= 0L) break
                                            var waitMs = cooldown
                                            while (waitMs > 0L && isActive) {
                                                val sec = ((waitMs + 999L) / 1000L).toInt().coerceAtLeast(1)
                                                rateLimitWaitSec.set(sec)
                                                publishProgress(force = true)
                                                delay(minOf(1000L, waitMs))
                                                waitMs = stockPhotoRepository.remainingCooldownMs()
                                            }
                                            rateLimitWaitSec.set(0)
                                        }
                                        loaded
                                    }

                                    if (photoOk) photosReadyCount.incrementAndGet()
                                    photoDoneCount.incrementAndGet()
                                    if (wordLatches[idx].incrementAndGet() >= latchTarget) {
                                        wordsCompletedCount.incrementAndGet()
                                    }
                                    publishProgress()
                                }
                            }
                        }
                    } else {
                        emptyList()
                    }

                    audioWorkers.forEach { it.join() }
                    photoWorkers.forEach { it.join() }
                }

                if (offlineDownloadSessionId.get() == sessionId) {
                    val refreshed = computeOfflineLevelSummaries()
                    val finalPhotos = photosReadyCount.get()
                    val finalAudio = audioReadyCount.get()
                    _offlineDownloadState.value = _offlineDownloadState.value.copy(
                        isRunning = false,
                        isPausedForRateLimit = false,
                        activeLevel = null,
                        processedWords = totalWords,
                        photosReady = finalPhotos,
                        audioReady = finalAudio,
                        progressFraction = 1f,
                        levelSummaries = refreshed,
                        statusMessage = "Рівень $normLevel збережено для офлайн-занять (фото: $finalPhotos, аудіо: $finalAudio)"
                    )
                }
            } catch (_: CancellationException) {
                withContext(NonCancellable) {
                    if (offlineDownloadSessionId.get() == sessionId) {
                        val refreshed = computeOfflineLevelSummaries()
                        _offlineDownloadState.value = _offlineDownloadState.value.copy(
                            isRunning = false,
                            isPausedForRateLimit = false,
                            activeLevel = null,
                            levelSummaries = refreshed,
                            statusMessage = "Завантаження $normLevel зупинено · вже збережене лишилось у кеші"
                        )
                    }
                }
            } catch (e: Exception) {
                if (offlineDownloadSessionId.get() == sessionId) {
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
    }

    fun cancelOfflineLevelDownload() {
        val activeLvl = _offlineDownloadState.value.activeLevel
        offlineDownloadSessionId.incrementAndGet()
        offlineDownloadJob?.cancel()
        offlineDownloadJob = null
        _offlineDownloadState.value = _offlineDownloadState.value.copy(
            isRunning = false,
            isPausedForRateLimit = false,
            activeLevel = null,
            statusMessage = if (activeLvl != null) {
                "Завантаження $activeLvl зупинено · вже збережене лишилось у кеші"
            } else {
                "Завантаження зупинено · вже збережене лишилось у кеші"
            }
        )
        refreshOfflineLevelSummaries()
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
            8 -> startStage8FinalMix(subtopicId)
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

    private fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

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

    // === Mixed-Stage Session State (Spaced Repetition Review OR Stage 8 Final Mix) ===
    private val _mixedSessionSubtopicId = MutableStateFlow<String?>(null)
    val mixedSessionSubtopicId: StateFlow<String?> = _mixedSessionSubtopicId.asStateFlow()
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
    private val _mixedSpellingSelectedArticle = MutableStateFlow<String?>(null)
    val mixedSpellingSelectedArticle: StateFlow<String?> = _mixedSpellingSelectedArticle.asStateFlow()
    private val _mixedSpellingErrorHint = MutableStateFlow<String?>(null)
    val mixedSpellingErrorHint: StateFlow<String?> = _mixedSpellingErrorHint.asStateFlow()
    private val _mixedSpellingStatus = MutableStateFlow(Stage6WordStatus.TYPING)
    val mixedSpellingStatus: StateFlow<Stage6WordStatus> = _mixedSpellingStatus.asStateFlow()
    private val _mixedSpellingAttemptsLeft = MutableStateFlow(MAX_STAGE6_ATTEMPTS)
    val mixedSpellingAttemptsLeft: StateFlow<Int> = _mixedSpellingAttemptsLeft.asStateFlow()
    private val _mixedFillBlankInput = MutableStateFlow("")
    val mixedFillBlankInput: StateFlow<String> = _mixedFillBlankInput.asStateFlow()
    private val _mixedFillBlankStatus = MutableStateFlow(Stage6WordStatus.TYPING)
    val mixedFillBlankStatus: StateFlow<Stage6WordStatus> = _mixedFillBlankStatus.asStateFlow()
    private val _mixedFillBlankAttemptsLeft = MutableStateFlow(MAX_STAGE6_ATTEMPTS)
    val mixedFillBlankAttemptsLeft: StateFlow<Int> = _mixedFillBlankAttemptsLeft.asStateFlow()
    private val _mixedFillBlankErrorHint = MutableStateFlow<String?>(null)
    val mixedFillBlankErrorHint: StateFlow<String?> = _mixedFillBlankErrorHint.asStateFlow()
    private val _mixedLastFeedback = MutableStateFlow<String?>(null)
    val mixedLastFeedback: StateFlow<String?> = _mixedLastFeedback.asStateFlow()

    // === Export / Import Status ===
    private val _ioStatusMessage = MutableStateFlow<IoStatusMessage?>(null)
    val ioStatusMessage: StateFlow<IoStatusMessage?> = _ioStatusMessage.asStateFlow()

    fun clearIoStatus() {
        _ioStatusMessage.value = null
    }

    fun reportIoError(message: String) {
        _ioStatusMessage.value = IoStatusMessage(message, isError = true)
    }

    fun exportVocabularyJson(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val json = repository.exportWordsAsJson()
                withContext(Dispatchers.IO) {
                    outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
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
                withContext(Dispatchers.IO) {
                    outputStream.use { it.write(tsv.toByteArray(Charsets.UTF_8)) }
                }
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
                val text = withContext(Dispatchers.IO) {
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
                }
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
    private val _stage5KeyboardInput = MutableStateFlow("")
    val stage5KeyboardInput: StateFlow<String> = _stage5KeyboardInput.asStateFlow()
    private val _stage5KeyboardStatus = MutableStateFlow(Stage6WordStatus.TYPING)
    val stage5KeyboardStatus: StateFlow<Stage6WordStatus> = _stage5KeyboardStatus.asStateFlow()
    private val _stage5KeyboardAttemptsLeft = MutableStateFlow(MAX_STAGE6_ATTEMPTS)
    val stage5KeyboardAttemptsLeft: StateFlow<Int> = _stage5KeyboardAttemptsLeft.asStateFlow()
    private val _stage5KeyboardErrorHint = MutableStateFlow<String?>(null)
    val stage5KeyboardErrorHint: StateFlow<String?> = _stage5KeyboardErrorHint.asStateFlow()
    private val _stage5Score = MutableStateFlow(0)
    val stage5Score: StateFlow<Int> = _stage5Score.asStateFlow()
    private val _stage5Finished = MutableStateFlow(false)
    val stage5Finished: StateFlow<Boolean> = _stage5Finished.asStateFlow()

    // === Stage 7: Spelling & Writing (with required article) State ===
    private val _stage6Words = MutableStateFlow<List<WordEntity>>(emptyList())
    val stage6Words: StateFlow<List<WordEntity>> = _stage6Words.asStateFlow()
    private val _stage6Index = MutableStateFlow(0)
    val stage6Index: StateFlow<Int> = _stage6Index.asStateFlow()
    private val _stage6LetterPool = MutableStateFlow<List<Char>>(emptyList())
    val stage6LetterPool: StateFlow<List<Char>> = _stage6LetterPool.asStateFlow()
    private val _stage6Input = MutableStateFlow("")
    val stage6Input: StateFlow<String> = _stage6Input.asStateFlow()
    private val _stage6SelectedArticle = MutableStateFlow<String?>(null)
    val stage6SelectedArticle: StateFlow<String?> = _stage6SelectedArticle.asStateFlow()
    private val _stage6ErrorHint = MutableStateFlow<String?>(null)
    val stage6ErrorHint: StateFlow<String?> = _stage6ErrorHint.asStateFlow()
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
        is Screen.Stage8FinalMix,
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

    private var stageLaunchJob: Job? = null

    fun navigateBack(): Boolean {
        ttsManager.stop()
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
        _mixedReviewItems.value = _mixedReviewItems.value.map { item ->
            if (item.word.id == word.id && item.word.german == word.german) {
                val updatedWord = item.word.copy(isFavorite = newFav)
                when (item) {
                    is MixedReviewItem.FlashcardReview -> item.copy(word = updatedWord)
                    is MixedReviewItem.ListeningReview -> item.copy(word = updatedWord)
                    is MixedReviewItem.MultipleChoiceReview -> item.copy(word = updatedWord)
                    is MixedReviewItem.ArticleReview -> item.copy(word = updatedWord)
                    is MixedReviewItem.PluralReview -> item.copy(word = updatedWord)
                    is MixedReviewItem.FillBlankReview -> item.copy(word = updatedWord)
                    is MixedReviewItem.SpellingReview -> item.copy(word = updatedWord)
                }
            } else {
                item
            }
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
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
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
                recordDailyStudyActivity()
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
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12).shuffled()
            val questions = words.map { target ->
                val distractors = repository.getDistractorsForWord(target, count = 3)
                val targetLabel = target.translationUk.trim()
                val distractorLabels = distractors.map { it.translationUk.trim() }.filter { it.isNotBlank() }
                val options = (distractorLabels + targetLabel).distinct().shuffled()
                val correctIdx = options.indexOf(targetLabel).coerceAtLeast(0)
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
            recordDailyStudyActivity()
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
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12).shuffled()
            val questions = words.map { target ->
                val distractors = repository.getDistractorsForWord(target, count = 3)
                val targetLabel = "${target.translationUk} · ${target.translationEn}"
                val distractorLabels = distractors.map { "${it.translationUk} · ${it.translationEn}" }
                val options = (distractorLabels + targetLabel).distinct().shuffled()
                val correctIdx = options.indexOf(targetLabel).coerceAtLeast(0)
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
            recordDailyStudyActivity()
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
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
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
            val firstQuestion = questions.firstOrNull()
            if (firstQuestion != null) {
                speak(firstQuestion.word.german)
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
            recordDailyStudyActivity()
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
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
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
            recordDailyStudyActivity()
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
    private fun resetStage5QuestionInput() {
        _stage5SelectedOption.value = null
        _stage5KeyboardInput.value = ""
        _stage5KeyboardStatus.value = Stage6WordStatus.TYPING
        _stage5KeyboardAttemptsLeft.value = MAX_STAGE6_ATTEMPTS
        _stage5KeyboardErrorHint.value = null
    }

    private fun buildRetryFillBlankQ(currentQ: FillBlankQ): FillBlankQ {
        val nextPair = nextRotatedFillBlankSentence(currentQ.word)
        return if (nextPair != null) {
            currentQ.copy(
                sentenceWithBlank = nextPair.second.sentenceWithBlank,
                matchedToken = nextPair.second.matchedToken,
                translation = nextPair.first.uk,
                fullSentence = nextPair.first.de,
                options = currentQ.options.shuffled(),
                isRetry = true
            )
        } else {
            currentQ.copy(
                options = currentQ.options.shuffled(),
                isRetry = true
            )
        }
    }

    fun startStage5(subtopicId: String) {
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12)
                .shuffled()
            val questions = words.mapNotNull { target ->
                val rotated = nextRotatedFillBlankSentence(target) ?: return@mapNotNull null
                val (example, blankMatch) = rotated
                val distractors = repository.getDistractorsForWord(target, count = 3).map { it.german }
                val options = (distractors + target.german).distinct().shuffled()
                FillBlankQ(
                    word = target,
                    sentenceWithBlank = blankMatch.sentenceWithBlank,
                    matchedToken = blankMatch.matchedToken,
                    translation = example.uk,
                    options = options,
                    correctOption = target.german,
                    fullSentence = example.de
                )
            }
            _stage5Questions.value = questions
            _stage5Index.value = 0
            resetStage5QuestionInput()
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
        if (_stage5SelectedOption.value != null || _stage5KeyboardStatus.value.isResolved) return
        _stage5SelectedOption.value = option
        val currentQ = _stage5Questions.value.getOrNull(_stage5Index.value) ?: return
        if (option.equals(currentQ.correctOption, ignoreCase = true)) {
            _stage5Score.value += 1
            speak(currentQ.fullSentence)
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
        } else {
            _stage5Questions.value = _stage5Questions.value + buildRetryFillBlankQ(currentQ)
            speak(currentQ.fullSentence)
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
        }
    }

    fun updateStage5KeyboardInput(raw: String) {
        if (_stage5KeyboardStatus.value.isResolved || _stage5SelectedOption.value != null) return
        _stage5KeyboardInput.value = raw
        _stage5KeyboardErrorHint.value = null
        _stage5KeyboardStatus.value = Stage6WordStatus.TYPING
    }

    fun insertStage5KeyboardChar(char: Char) {
        if (_stage5KeyboardStatus.value.isResolved || _stage5SelectedOption.value != null) return
        _stage5KeyboardInput.value += char
        _stage5KeyboardErrorHint.value = null
        _stage5KeyboardStatus.value = Stage6WordStatus.TYPING
    }

    fun clearStage5KeyboardInput() {
        if (_stage5KeyboardStatus.value.isResolved || _stage5SelectedOption.value != null) return
        _stage5KeyboardInput.value = ""
        _stage5KeyboardErrorHint.value = null
        _stage5KeyboardStatus.value = Stage6WordStatus.TYPING
    }

    fun useStage5KeyboardHint() {
        if (_stage5KeyboardStatus.value.isResolved || _stage5SelectedOption.value != null) return
        val currentQ = _stage5Questions.value.getOrNull(_stage5Index.value) ?: return
        val target = currentQ.matchedToken.ifBlank { currentQ.word.german }.trim()
        val nextInput = computeNextHintInput(_stage5KeyboardInput.value, target)
        _stage5KeyboardInput.value = nextInput
        _stage5KeyboardErrorHint.value = null
        if (nextInput.equals(target, ignoreCase = true)) {
            _stage5KeyboardInput.value = target
            _stage5KeyboardStatus.value = Stage6WordStatus.CORRECT
            _stage5SelectedOption.value = currentQ.correctOption
            _stage5Score.value += 1
            speak(currentQ.fullSentence)
            viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
        } else {
            _stage5KeyboardStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun submitStage5KeyboardAnswer() {
        if (_stage5KeyboardStatus.value.isResolved || _stage5SelectedOption.value != null) return
        val currentQ = _stage5Questions.value.getOrNull(_stage5Index.value) ?: return
        val raw = _stage5KeyboardInput.value.trim()
        if (raw.isEmpty()) return

        when (val eval = evaluateFillBlankKeyboardAnswer(raw, currentQ.matchedToken, currentQ.word.german)) {
            is FillBlankKeyboardEvaluation.ExactMatch -> {
                _stage5KeyboardInput.value = eval.canonical
                _stage5KeyboardErrorHint.value = null
                _stage5KeyboardStatus.value = Stage6WordStatus.CORRECT
                _stage5SelectedOption.value = currentQ.correctOption
                _stage5Score.value += 1
                speak(currentQ.fullSentence)
                viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
            }
            is FillBlankKeyboardEvaluation.LenientUmlautMatch -> {
                _stage5KeyboardInput.value = eval.canonical
                _stage5KeyboardErrorHint.value = null
                _stage5KeyboardStatus.value = Stage6WordStatus.CORRECT_UMLAUT_WARNING
                _stage5SelectedOption.value = currentQ.correctOption
                _stage5Score.value += 1
                speak(currentQ.fullSentence)
                viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.GOOD) }
            }
            is FillBlankKeyboardEvaluation.WrongInput -> {
                val remaining = (_stage5KeyboardAttemptsLeft.value - 1).coerceAtLeast(0)
                _stage5KeyboardAttemptsLeft.value = remaining
                if (remaining > 0) {
                    _stage5KeyboardInput.value = eval.keptPrefix
                    _stage5KeyboardStatus.value = Stage6WordStatus.WRONG_TRY
                    _stage5KeyboardErrorHint.value = "Спробуйте ще раз · залишилось спроб: $remaining"
                } else {
                    _stage5KeyboardStatus.value = Stage6WordStatus.OUT_OF_ATTEMPTS
                    _stage5SelectedOption.value = "__wrong_keyboard__"
                    _stage5KeyboardErrorHint.value = null
                    _stage5Questions.value = _stage5Questions.value + buildRetryFillBlankQ(currentQ)
                    speak(currentQ.fullSentence)
                    viewModelScope.launch { repository.recordReview(currentQ.word.id, ReviewRating.AGAIN) }
                }
            }
        }
    }

    fun nextStage5(subtopicId: String) {
        val nextIdx = _stage5Index.value + 1
        if (nextIdx < _stage5Questions.value.size) {
            _stage5Index.value = nextIdx
            resetStage5QuestionInput()
        } else {
            _stage5Finished.value = true
            recordDailyStudyActivity()
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
    // === STAGE 7: SPELLING & WRITING (WITH ARTICLE) ===
    // ==========================================
    fun startStage6(subtopicId: String) {
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 12).shuffled()
            _stage6Words.value = words
            _stage6Index.value = 0
            _stage6Score.value = 0
            _stage6Finished.value = words.isEmpty()
            _stage6RetryIndices.value = emptySet()
            _lastCompletedUnlockedSrs.value = false
            prepareStage6Word(0)
            navigateToStage(Screen.Stage7Spelling(subtopicId))
            if (words.isEmpty()) {
                val unlocked = repository.completeStage(
                    subtopicId = subtopicId,
                    stage = 7,
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

    private fun prepareStage6Word(index: Int) {
        val target = _stage6Words.value.getOrNull(index) ?: return
        _stage6Input.value = ""
        _stage6SelectedArticle.value = null
        _stage6ErrorHint.value = null
        _stage6Status.value = Stage6WordStatus.TYPING
        _stage6AttemptsLeft.value = MAX_STAGE6_ATTEMPTS
        _stage6HintsUsed.value = 0

        val targetLetters = target.german.trim().filter { it != ' ' && it != '-' }.toList()
        val extraLetters = listOf('e', 'a', 'i', 'r', 's', 't', 'n', 'l', 'ä', 'ö', 'ü').shuffled().take(3)
        _stage6LetterPool.value = (targetLetters + extraLetters).shuffled()
        // Speak only the bare word before solving so audio doesn't spoil der/die/das
        speak(if (requiresArticleInSpelling(target)) target.german else target.displayWithArticle)
    }

    private fun handleStage6WrongAttempt(
        word: WordEntity,
        keptInput: String,
        errorMessage: String? = null,
        resetArticle: Boolean = false
    ) {
        val remaining = (_stage6AttemptsLeft.value - 1).coerceAtLeast(0)
        _stage6AttemptsLeft.value = remaining
        if (remaining > 0) {
            _stage6Input.value = keptInput
            if (resetArticle) _stage6SelectedArticle.value = null
            _stage6ErrorHint.value = errorMessage
            _stage6Status.value = Stage6WordStatus.WRONG_TRY
        } else {
            _stage6ErrorHint.value = null
            _stage6Status.value = Stage6WordStatus.OUT_OF_ATTEMPTS
            val newIdx = _stage6Words.value.size
            _stage6Words.value = _stage6Words.value + word
            _stage6RetryIndices.value = _stage6RetryIndices.value + newIdx
            speak(word.displayWithArticle)
            viewModelScope.launch { repository.recordReview(word.id, ReviewRating.AGAIN) }
        }
    }

    private fun completeStage6WordCorrect(
        word: WordEntity,
        status: Stage6WordStatus = Stage6WordStatus.CORRECT
    ) {
        _stage6ErrorHint.value = null
        _stage6Status.value = status
        _stage6Score.value += 1
        speak(word.displayWithArticle)
        viewModelScope.launch { repository.recordReview(word.id, ReviewRating.GOOD) }
    }

    fun selectStage6Article(article: String) {
        if (_stage6Status.value.isResolved) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        if (!requiresArticleInSpelling(word)) return

        val normalized = article.lowercase().trim()
        _stage6SelectedArticle.value = normalized
        _stage6ErrorHint.value = null

        val targetNoun = word.german.trim()
        if (_stage6Input.value.equals(targetNoun, ignoreCase = true)) {
            if (normalized.equals(word.article.trim(), ignoreCase = true)) {
                _stage6Input.value = targetNoun
                completeStage6WordCorrect(word)
            } else {
                handleStage6WrongAttempt(
                    word = word,
                    keptInput = targetNoun,
                    errorMessage = "Слово вірне, але артикль інший! Оберіть правильний (der / die / das)",
                    resetArticle = true
                )
            }
        } else {
            _stage6Status.value = Stage6WordStatus.TYPING
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
        _stage6ErrorHint.value = null

        if (newText.equals(target, ignoreCase = true)) {
            _stage6Input.value = target
            if (!requiresArticleInSpelling(word)) {
                completeStage6WordCorrect(word)
            } else {
                val chosenArt = _stage6SelectedArticle.value
                when {
                    chosenArt == null -> {
                        _stage6Status.value = Stage6WordStatus.TYPING
                        _stage6ErrorHint.value = "Слово зібрано! Тепер оберіть артикль: der, die або das"
                    }
                    chosenArt.equals(word.article.trim(), ignoreCase = true) -> {
                        completeStage6WordCorrect(word)
                    }
                    else -> {
                        handleStage6WrongAttempt(
                            word = word,
                            keptInput = target,
                            errorMessage = "Слово вірне, але артикль інший! Оберіть правильний (der / die / das)",
                            resetArticle = true
                        )
                    }
                }
            }
        } else if (newText.length >= target.length) {
            handleStage6WrongAttempt(
                word = word,
                keptInput = keepCorrectPrefix(newText, target, allowLenientUmlauts = false)
            )
        } else {
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    fun insertStage6Umlaut(char: Char) {
        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            if (_stage6Status.value.isResolved) return
            val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
            val fullTarget = spellingTargetFor(word)
            val updated = _stage6Input.value + char
            _stage6Input.value = updated
            _stage6ErrorHint.value = null
            if (normalizeSpellingSpaces(updated).equals(fullTarget, ignoreCase = true)) {
                _stage6Input.value = fullTarget
                completeStage6WordCorrect(word)
            } else {
                _stage6Status.value = Stage6WordStatus.TYPING
            }
        } else {
            addStage6Letter(char)
        }
    }

    fun updateStage6TypedText(raw: String) {
        if (_stage6Status.value.isResolved) return
        _stage6Input.value = raw
        _stage6ErrorHint.value = null
        _stage6Status.value = Stage6WordStatus.TYPING
    }

    fun submitStage6TypedAnswer() {
        if (_stage6Status.value.isResolved) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        val raw = _stage6Input.value.trim()
        if (raw.isEmpty()) return

        when (val eval = evaluateKeyboardSpelling(raw, word)) {
            is KeyboardSpellingEvaluation.ExactMatch -> {
                _stage6Input.value = eval.canonical
                completeStage6WordCorrect(word, Stage6WordStatus.CORRECT)
            }
            is KeyboardSpellingEvaluation.LenientUmlautMatch -> {
                _stage6Input.value = eval.cleanedInput
                completeStage6WordCorrect(word, Stage6WordStatus.CORRECT_UMLAUT_WARNING)
            }
            is KeyboardSpellingEvaluation.MissingArticle -> {
                handleStage6WrongAttempt(
                    word = word,
                    keptInput = eval.preservedText,
                    errorMessage = "Не забудьте написати артикль (der / die / das) перед словом!"
                )
            }
            is KeyboardSpellingEvaluation.WrongArticle -> {
                handleStage6WrongAttempt(
                    word = word,
                    keptInput = eval.remainder,
                    errorMessage = "Невірний артикль «${eval.typedArticle}» · спробуйте інший (der / die / das)"
                )
            }
            is KeyboardSpellingEvaluation.WrongSpelling -> {
                val msg = if (eval.articleWasCorrect && requiresArticleInSpelling(word)) {
                    "Артикль вірний! Перевірте написання слова"
                } else {
                    null
                }
                handleStage6WrongAttempt(
                    word = word,
                    keptInput = eval.keptPrefix,
                    errorMessage = msg
                )
            }
        }
    }

    /**
     * Reveals the article first (when missing/wrong for nouns), then reveals one next correct letter.
     */
    fun useStage6Hint() {
        if (_stage6Status.value.isResolved) return
        val word = _stage6Words.value.getOrNull(_stage6Index.value) ?: return
        _stage6HintsUsed.value += 1
        _stage6ErrorHint.value = null

        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            val fullTarget = spellingTargetFor(word)
            val nextInput = computeKeyboardHintWithArticle(_stage6Input.value, word)
            _stage6Input.value = nextInput
            if (normalizeSpellingSpaces(nextInput).equals(fullTarget, ignoreCase = true)) {
                _stage6Input.value = fullTarget
                completeStage6WordCorrect(word)
            } else {
                _stage6Status.value = Stage6WordStatus.TYPING
            }
            return
        }

        // LETTER_TILES mode: reveal article first if noun needs it and it's not yet correct
        val targetNoun = word.german.trim()
        if (requiresArticleInSpelling(word) &&
            !_stage6SelectedArticle.value.equals(word.article.trim(), ignoreCase = true)
        ) {
            _stage6SelectedArticle.value = word.article.lowercase().trim()
            if (_stage6Input.value.equals(targetNoun, ignoreCase = true)) {
                _stage6Input.value = targetNoun
                completeStage6WordCorrect(word)
            } else {
                _stage6Status.value = Stage6WordStatus.TYPING
            }
            return
        }

        val nextInput = computeNextHintInput(_stage6Input.value, targetNoun)
        _stage6Input.value = nextInput
        if (nextInput.equals(targetNoun, ignoreCase = true) &&
            (!requiresArticleInSpelling(word) ||
                _stage6SelectedArticle.value.equals(word.article.trim(), ignoreCase = true))
        ) {
            completeStage6WordCorrect(word)
        } else {
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    fun removeStage6Letter() {
        if (_stage6Status.value.isResolved) return
        if (_stage6Input.value.isNotEmpty()) {
            _stage6Input.value = backspaceOverSeparators(_stage6Input.value)
            _stage6ErrorHint.value = null
            _stage6Status.value = Stage6WordStatus.TYPING
        }
    }

    fun clearStage6() {
        if (_stage6Status.value.isResolved) return
        _stage6Input.value = ""
        _stage6SelectedArticle.value = null
        _stage6ErrorHint.value = null
        _stage6Status.value = Stage6WordStatus.TYPING
    }

    fun nextStage6(subtopicId: String) {
        val nextIdx = _stage6Index.value + 1
        if (nextIdx < _stage6Words.value.size) {
            _stage6Index.value = nextIdx
            prepareStage6Word(nextIdx)
        } else {
            _stage6Finished.value = true
            recordDailyStudyActivity()
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
    // === STAGE 8: FINAL MIX (GESAMTTEST — ALL STAGES COMBINED) ===
    // ==========================================
    fun startStage8FinalMix(subtopicId: String) {
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
            val words = repository.getWordsBySubtopicAndLevelOnce(subtopicId, _selectedLevel.value, 15).shuffled()
            val activeStages = (_enabledStages.value intersect setOf(2, 3, 4, 5, 6, 7))
                .ifEmpty { setOf(2, 3, 4, 5, 6, 7) }

            val builtItems = mutableListOf<MixedReviewItem>()
            val stageUsageCounts = mutableMapOf<Int, Int>()
            var lastStageType: Int? = null

            for (word in words) {
                val chosenStage = pickFinalMixStageForWord(
                    word = word,
                    enabledPracticeStages = activeStages,
                    stageUsageCounts = stageUsageCounts,
                    previousStageType = lastStageType
                )
                lastStageType = chosenStage
                stageUsageCounts[chosenStage] = (stageUsageCounts[chosenStage] ?: 0) + 1
                val item = buildMixedReviewItem(word = word, srsStep = 0, stageType = chosenStage)
                builtItems.add(item)
            }

            _mixedSessionSubtopicId.value = subtopicId
            _mixedReviewItems.value = builtItems
            _mixedReviewIndex.value = 0
            _mixedReviewScore.value = 0
            _mixedReviewFinished.value = builtItems.isEmpty()
            _mixedReviewIsEarlyPractice.value = false
            _lastCompletedUnlockedSrs.value = false
            resetMixedItemState()

            stockPhotoRepository.prefetchWordsAsync(
                builtItems.filter {
                    it is MixedReviewItem.FlashcardReview ||
                        it is MixedReviewItem.MultipleChoiceReview ||
                        it is MixedReviewItem.ArticleReview ||
                        it is MixedReviewItem.PluralReview
                }.take(4).map { it.word }
            )
            navigateToStage(Screen.Stage8FinalMix(subtopicId))

            val firstItem = builtItems.firstOrNull()
            if (firstItem != null) {
                speakForMixedItem(firstItem)
            } else {
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

    // ==========================================
    // === MIXED-STAGE SPACED REPETITION REVIEW ===
    // ==========================================
    fun startMixedReviewSession(earlyPractice: Boolean = false) {
        stageLaunchJob?.cancel()
        stageLaunchJob = viewModelScope.launch {
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

            _mixedSessionSubtopicId.value = null
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
                val targetLabel = word.translationUk.trim()
                val options = (distractors.map { it.translationUk.trim() }.filter { it.isNotBlank() } + targetLabel)
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
                val rotated = nextRotatedFillBlankSentence(word)
                if (rotated != null) {
                    val (example, blankMatch) = rotated
                    val distractors = repository.getDistractorsForWord(word, count = 3).map { it.german }
                    val options = (distractors + word.german).distinct().shuffled()
                    MixedReviewItem.FillBlankReview(
                        word = word,
                        srsStep = srsStep,
                        sentenceWithBlank = blankMatch.sentenceWithBlank,
                        matchedToken = blankMatch.matchedToken,
                        translation = example.uk,
                        options = options,
                        correctOption = word.german,
                        fullSentence = example.de
                    )
                } else {
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
        _mixedSpellingSelectedArticle.value = null
        _mixedSpellingErrorHint.value = null
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        _mixedSpellingAttemptsLeft.value = MAX_STAGE6_ATTEMPTS
        _mixedFillBlankInput.value = ""
        _mixedFillBlankStatus.value = Stage6WordStatus.TYPING
        _mixedFillBlankAttemptsLeft.value = MAX_STAGE6_ATTEMPTS
        _mixedFillBlankErrorHint.value = null
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
            is MixedReviewItem.SpellingReview ->
                speak(if (requiresArticleInSpelling(item.word)) item.word.german else item.word.displayWithArticle)
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
            is MixedReviewItem.FillBlankReview -> {
                val nextPair = nextRotatedFillBlankSentence(item.word)
                if (nextPair != null) {
                    item.copy(
                        sentenceWithBlank = nextPair.second.sentenceWithBlank,
                        matchedToken = nextPair.second.matchedToken,
                        translation = nextPair.first.uk,
                        fullSentence = nextPair.first.de,
                        options = item.options.shuffled(),
                        isRetry = true
                    )
                } else {
                    item.copy(options = item.options.shuffled(), isRetry = true)
                }
            }
            is MixedReviewItem.SpellingReview -> item.copy(letterPool = item.letterPool.shuffled(), isRetry = true)
        }
        _mixedReviewItems.value = _mixedReviewItems.value + retryItem
    }

    private fun recordMixedOutcome(item: MixedReviewItem, isCorrect: Boolean) {
        recordDailyStudyActivity()
        val isFinalMixStage = _mixedSessionSubtopicId.value != null
        if (isFinalMixStage) {
            if (isCorrect) {
                _mixedReviewScore.value += 1
                _mixedLastFeedback.value = if (item.isRetry) {
                    "Виправлено! Слово закріплено у фінальному міксі"
                } else {
                    "Вірно! Рухаємося далі"
                }
                viewModelScope.launch { repository.recordReview(item.word.id, ReviewRating.GOOD) }
            } else {
                appendMixedRetry(item)
                _mixedLastFeedback.value = "Помилка · слово повернеться в кінці фінального міксу для закріплення"
                viewModelScope.launch { repository.recordReview(item.word.id, ReviewRating.AGAIN) }
            }
            return
        }

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
        if (_mixedSelectedString.value != null || _mixedFillBlankStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.FillBlankReview
            ?: return
        _mixedSelectedString.value = option
        val isCorrect = option.equals(item.correctOption, ignoreCase = true)
        speak(item.fullSentence)
        recordMixedOutcome(item, isCorrect)
    }

    fun updateMixedFillBlankInput(raw: String) {
        if (_mixedFillBlankStatus.value.isResolved || _mixedSelectedString.value != null) return
        _mixedFillBlankInput.value = raw
        _mixedFillBlankErrorHint.value = null
        _mixedFillBlankStatus.value = Stage6WordStatus.TYPING
    }

    fun insertMixedFillBlankChar(char: Char) {
        if (_mixedFillBlankStatus.value.isResolved || _mixedSelectedString.value != null) return
        _mixedFillBlankInput.value += char
        _mixedFillBlankErrorHint.value = null
        _mixedFillBlankStatus.value = Stage6WordStatus.TYPING
    }

    fun clearMixedFillBlankInput() {
        if (_mixedFillBlankStatus.value.isResolved || _mixedSelectedString.value != null) return
        _mixedFillBlankInput.value = ""
        _mixedFillBlankErrorHint.value = null
        _mixedFillBlankStatus.value = Stage6WordStatus.TYPING
    }

    fun useMixedFillBlankHint() {
        if (_mixedFillBlankStatus.value.isResolved || _mixedSelectedString.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.FillBlankReview
            ?: return
        val target = item.matchedToken.ifBlank { item.word.german }.trim()
        val nextInput = computeNextHintInput(_mixedFillBlankInput.value, target)
        _mixedFillBlankInput.value = nextInput
        _mixedFillBlankErrorHint.value = null
        if (nextInput.equals(target, ignoreCase = true)) {
            _mixedFillBlankInput.value = target
            _mixedFillBlankStatus.value = Stage6WordStatus.CORRECT
            _mixedSelectedString.value = item.correctOption
            speak(item.fullSentence)
            recordMixedOutcome(item, isCorrect = true)
        } else {
            _mixedFillBlankStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun submitMixedFillBlankAnswer() {
        if (_mixedFillBlankStatus.value.isResolved || _mixedSelectedString.value != null) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.FillBlankReview
            ?: return
        val raw = _mixedFillBlankInput.value.trim()
        if (raw.isEmpty()) return

        when (val eval = evaluateFillBlankKeyboardAnswer(raw, item.matchedToken, item.word.german)) {
            is FillBlankKeyboardEvaluation.ExactMatch -> {
                _mixedFillBlankInput.value = eval.canonical
                _mixedFillBlankErrorHint.value = null
                _mixedFillBlankStatus.value = Stage6WordStatus.CORRECT
                _mixedSelectedString.value = item.correctOption
                speak(item.fullSentence)
                recordMixedOutcome(item, isCorrect = true)
            }
            is FillBlankKeyboardEvaluation.LenientUmlautMatch -> {
                _mixedFillBlankInput.value = eval.canonical
                _mixedFillBlankErrorHint.value = null
                _mixedFillBlankStatus.value = Stage6WordStatus.CORRECT_UMLAUT_WARNING
                _mixedSelectedString.value = item.correctOption
                speak(item.fullSentence)
                recordMixedOutcome(item, isCorrect = true)
            }
            is FillBlankKeyboardEvaluation.WrongInput -> {
                val remaining = (_mixedFillBlankAttemptsLeft.value - 1).coerceAtLeast(0)
                _mixedFillBlankAttemptsLeft.value = remaining
                if (remaining > 0) {
                    _mixedFillBlankInput.value = eval.keptPrefix
                    _mixedFillBlankStatus.value = Stage6WordStatus.WRONG_TRY
                    _mixedFillBlankErrorHint.value = "Спробуйте ще раз · залишилось спроб: $remaining"
                } else {
                    _mixedFillBlankStatus.value = Stage6WordStatus.OUT_OF_ATTEMPTS
                    _mixedSelectedString.value = "__wrong_keyboard__"
                    _mixedFillBlankErrorHint.value = null
                    speak(item.fullSentence)
                    recordMixedOutcome(item, isCorrect = false)
                }
            }
        }
    }

    private fun handleMixedSpellingWrongAttempt(
        item: MixedReviewItem.SpellingReview,
        keptInput: String,
        errorMessage: String? = null,
        resetArticle: Boolean = false
    ) {
        val remaining = (_mixedSpellingAttemptsLeft.value - 1).coerceAtLeast(0)
        _mixedSpellingAttemptsLeft.value = remaining
        if (remaining > 0) {
            _mixedSpellingInput.value = keptInput
            if (resetArticle) _mixedSpellingSelectedArticle.value = null
            _mixedSpellingErrorHint.value = errorMessage
            _mixedSpellingStatus.value = Stage6WordStatus.WRONG_TRY
        } else {
            _mixedSpellingErrorHint.value = null
            _mixedSpellingStatus.value = Stage6WordStatus.OUT_OF_ATTEMPTS
            speak(item.word.displayWithArticle)
            recordMixedOutcome(item, isCorrect = false)
        }
    }

    fun selectMixedSpellingArticle(article: String) {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        if (!requiresArticleInSpelling(item.word)) return

        val normalized = article.lowercase().trim()
        _mixedSpellingSelectedArticle.value = normalized
        _mixedSpellingErrorHint.value = null

        val targetNoun = item.word.german.trim()
        if (_mixedSpellingInput.value.equals(targetNoun, ignoreCase = true)) {
            if (normalized.equals(item.word.article.trim(), ignoreCase = true)) {
                _mixedSpellingInput.value = targetNoun
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            } else {
                handleMixedSpellingWrongAttempt(
                    item = item,
                    keptInput = targetNoun,
                    errorMessage = "Слово вірне, але артикль інший! Оберіть правильний (der / die / das)",
                    resetArticle = true
                )
            }
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
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
        _mixedSpellingErrorHint.value = null

        if (newText.equals(target, ignoreCase = true)) {
            _mixedSpellingInput.value = target
            if (!requiresArticleInSpelling(item.word)) {
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            } else {
                val chosenArt = _mixedSpellingSelectedArticle.value
                when {
                    chosenArt == null -> {
                        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
                        _mixedSpellingErrorHint.value = "Слово зібрано! Тепер оберіть артикль: der, die або das"
                    }
                    chosenArt.equals(item.word.article.trim(), ignoreCase = true) -> {
                        _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                        speak(item.word.displayWithArticle)
                        recordMixedOutcome(item, isCorrect = true)
                    }
                    else -> {
                        handleMixedSpellingWrongAttempt(
                            item = item,
                            keptInput = target,
                            errorMessage = "Слово вірне, але артикль інший! Оберіть правильний (der / die / das)",
                            resetArticle = true
                        )
                    }
                }
            }
        } else if (newText.length >= target.length) {
            handleMixedSpellingWrongAttempt(
                item = item,
                keptInput = keepCorrectPrefix(newText, target, allowLenientUmlauts = false)
            )
        } else {
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun insertMixedSpellingUmlaut(char: Char) {
        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            if (_mixedSpellingStatus.value.isResolved) return
            val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
                ?: return
            val fullTarget = spellingTargetFor(item.word)
            val updated = _mixedSpellingInput.value + char
            _mixedSpellingInput.value = updated
            _mixedSpellingErrorHint.value = null
            if (normalizeSpellingSpaces(updated).equals(fullTarget, ignoreCase = true)) {
                _mixedSpellingInput.value = fullTarget
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
        _mixedSpellingInput.value = raw
        _mixedSpellingErrorHint.value = null
        _mixedSpellingStatus.value = Stage6WordStatus.TYPING
    }

    fun submitMixedSpellingTypedAnswer() {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        val raw = _mixedSpellingInput.value.trim()
        if (raw.isEmpty()) return

        when (val eval = evaluateKeyboardSpelling(raw, item.word)) {
            is KeyboardSpellingEvaluation.ExactMatch -> {
                _mixedSpellingInput.value = eval.canonical
                _mixedSpellingErrorHint.value = null
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            }
            is KeyboardSpellingEvaluation.LenientUmlautMatch -> {
                _mixedSpellingInput.value = eval.cleanedInput
                _mixedSpellingErrorHint.value = null
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT_UMLAUT_WARNING
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            }
            is KeyboardSpellingEvaluation.MissingArticle -> {
                handleMixedSpellingWrongAttempt(
                    item = item,
                    keptInput = eval.preservedText,
                    errorMessage = "Не забудьте написати артикль (der / die / das) перед словом!"
                )
            }
            is KeyboardSpellingEvaluation.WrongArticle -> {
                handleMixedSpellingWrongAttempt(
                    item = item,
                    keptInput = eval.remainder,
                    errorMessage = "Невірний артикль «${eval.typedArticle}» · спробуйте інший (der / die / das)"
                )
            }
            is KeyboardSpellingEvaluation.WrongSpelling -> {
                val msg = if (eval.articleWasCorrect && requiresArticleInSpelling(item.word)) {
                    "Артикль вірний! Перевірте написання слова"
                } else {
                    null
                }
                handleMixedSpellingWrongAttempt(
                    item = item,
                    keptInput = eval.keptPrefix,
                    errorMessage = msg
                )
            }
        }
    }

    fun useMixedSpellingHint() {
        if (_mixedSpellingStatus.value.isResolved) return
        val item = _mixedReviewItems.value.getOrNull(_mixedReviewIndex.value) as? MixedReviewItem.SpellingReview
            ?: return
        _mixedSpellingErrorHint.value = null

        if (_stage6InputMode.value == Stage6InputMode.KEYBOARD) {
            val fullTarget = spellingTargetFor(item.word)
            val nextInput = computeKeyboardHintWithArticle(_mixedSpellingInput.value, item.word)
            _mixedSpellingInput.value = nextInput
            if (normalizeSpellingSpaces(nextInput).equals(fullTarget, ignoreCase = true)) {
                _mixedSpellingInput.value = fullTarget
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            } else {
                _mixedSpellingStatus.value = Stage6WordStatus.TYPING
            }
            return
        }

        val targetNoun = item.word.german.trim()
        if (requiresArticleInSpelling(item.word) &&
            !_mixedSpellingSelectedArticle.value.equals(item.word.article.trim(), ignoreCase = true)
        ) {
            _mixedSpellingSelectedArticle.value = item.word.article.lowercase().trim()
            if (_mixedSpellingInput.value.equals(targetNoun, ignoreCase = true)) {
                _mixedSpellingInput.value = targetNoun
                _mixedSpellingStatus.value = Stage6WordStatus.CORRECT
                speak(item.word.displayWithArticle)
                recordMixedOutcome(item, isCorrect = true)
            } else {
                _mixedSpellingStatus.value = Stage6WordStatus.TYPING
            }
            return
        }

        val nextInput = computeNextHintInput(_mixedSpellingInput.value, targetNoun)
        _mixedSpellingInput.value = nextInput
        if (nextInput.equals(targetNoun, ignoreCase = true) &&
            (!requiresArticleInSpelling(item.word) ||
                _mixedSpellingSelectedArticle.value.equals(item.word.article.trim(), ignoreCase = true))
        ) {
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
            _mixedSpellingErrorHint.value = null
            _mixedSpellingStatus.value = Stage6WordStatus.TYPING
        }
    }

    fun clearMixedSpelling() {
        if (_mixedSpellingStatus.value.isResolved) return
        _mixedSpellingInput.value = ""
        _mixedSpellingSelectedArticle.value = null
        _mixedSpellingErrorHint.value = null
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
            recordDailyStudyActivity()
            val finalMixSubId = _mixedSessionSubtopicId.value
            if (finalMixSubId != null) {
                viewModelScope.launch {
                    val unlocked = repository.completeStage(
                        subtopicId = finalMixSubId,
                        stage = 8,
                        score = _mixedReviewScore.value,
                        enabledStages = _enabledStages.value,
                        level = _selectedLevel.value,
                        achieved100Percent = true
                    )
                    _lastCompletedUnlockedSrs.value = unlocked
                    refreshReviewDashboard()
                }
            } else {
                refreshReviewDashboard()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stageLaunchJob?.cancel()
        offlineDownloadJob?.cancel()
        ttsManager.shutdown()
    }

    companion object {
        const val MAX_STAGE6_ATTEMPTS = 3
        const val DEFAULT_DAILY_REVIEW_LIMIT = 60
        const val MIXED_ROUND_SIZE = 15
        val ALL_STAGES: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7, 8)
        val DAILY_LIMIT_PRESETS: List<Int> = listOf(40, 60, 80)

        internal fun orderWordsByTopicCatalog(words: List<WordEntity>): List<WordEntity> {
            val catalogSubtopicOrder = TopicCatalog.mainTopics
                .flatMap { it.subtopics }
                .mapIndexed { idx, sub -> sub.id to idx }
                .toMap()
            return words.sortedWith(
                compareBy<WordEntity>(
                    { catalogSubtopicOrder[it.subtopicId] ?: Int.MAX_VALUE },
                    { it.id }
                )
            )
        }

        internal fun nextEnabledStage(enabledStages: Set<Int>, afterStage: Int): Int? {
            return enabledStages.filter { it in 1..8 && it > afterStage }.minOrNull()
        }

        internal fun computeEffectiveUnlockedStage(
            completedStages: Set<Int>,
            visibleStages: List<Int>
        ): Int {
            val firstVisible = visibleStages.firstOrNull() ?: 1
            if (completedStages.isEmpty()) return firstVisible
            val maxDone = completedStages.maxOrNull() ?: 0
            return visibleStages.firstOrNull { it > maxDone } ?: VocabularyRepository.TOTAL_STAGES
        }

        internal fun computeCurrentStreak(
            savedStreak: Int,
            lastActiveDate: String?,
            today: String,
            yesterday: String
        ): Int {
            if (savedStreak <= 0 || lastActiveDate.isNullOrBlank()) return 0
            return if (lastActiveDate == today || lastActiveDate == yesterday) savedStreak else 0
        }

        internal fun computeUpdatedStreak(
            savedStreak: Int,
            lastActiveDate: String?,
            today: String,
            yesterday: String
        ): Int {
            return when {
                lastActiveDate == today -> savedStreak.coerceAtLeast(1)
                lastActiveDate == yesterday -> savedStreak.coerceAtLeast(0) + 1
                else -> 1
            }
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

        internal fun requiresArticleInSpelling(word: WordEntity): Boolean {
            return word.article.lowercase().trim() in listOf("der", "die", "das")
        }

        internal fun spellingTargetFor(word: WordEntity): String {
            return if (requiresArticleInSpelling(word)) {
                "${word.article.lowercase().trim()} ${word.german.trim()}"
            } else {
                word.german.trim()
            }
        }

        internal fun normalizeSpellingSpaces(raw: String): String {
            return raw.trim().replace(Regex("\\s+"), " ")
        }

        internal fun evaluateKeyboardSpelling(
            rawInput: String,
            word: WordEntity
        ): KeyboardSpellingEvaluation {
            val cleaned = normalizeSpellingSpaces(rawInput)
            val fullTarget = spellingTargetFor(word)

            if (cleaned.equals(fullTarget, ignoreCase = true)) {
                return KeyboardSpellingEvaluation.ExactMatch(fullTarget)
            }
            if (matchesWithLenientUmlauts(cleaned, fullTarget)) {
                return KeyboardSpellingEvaluation.LenientUmlautMatch(cleaned)
            }

            if (requiresArticleInSpelling(word)) {
                val expectedArticle = word.article.lowercase().trim()
                val firstSpace = cleaned.indexOf(' ')
                if (firstSpace < 0) {
                    // User entered only one word without any article
                    return KeyboardSpellingEvaluation.MissingArticle(cleaned)
                }
                val firstToken = cleaned.substring(0, firstSpace).lowercase()
                val remainder = cleaned.substring(firstSpace + 1).trim()
                if (firstToken in listOf("der", "die", "das")) {
                    if (firstToken != expectedArticle) {
                        return KeyboardSpellingEvaluation.WrongArticle(
                            typedArticle = firstToken,
                            remainder = remainder
                        )
                    }
                    // Correct article, check noun prefix
                    val keptNoun = keepCorrectPrefix(remainder, word.german.trim(), allowLenientUmlauts = true)
                    return KeyboardSpellingEvaluation.WrongSpelling(
                        keptPrefix = "$expectedArticle $keptNoun",
                        articleWasCorrect = true
                    )
                }
                // First token wasn't even an article (e.g. multi-word noun typed without article)
                return KeyboardSpellingEvaluation.MissingArticle(cleaned)
            }

            val kept = keepCorrectPrefix(cleaned, fullTarget, allowLenientUmlauts = true)
            return KeyboardSpellingEvaluation.WrongSpelling(
                keptPrefix = kept,
                articleWasCorrect = true
            )
        }

        internal fun computeKeyboardHintWithArticle(current: String, word: WordEntity): String {
            val fullTarget = spellingTargetFor(word)
            if (requiresArticleInSpelling(word)) {
                val expectedArticle = word.article.lowercase().trim()
                val expectedPrefix = "$expectedArticle "
                val normalized = normalizeSpellingSpaces(current)

                if (normalized.isEmpty()) {
                    return expectedPrefix
                }

                // Already equals the right article (e.g. "die" or "die ") -> reveal first letter of the noun
                if (normalized.equals(expectedArticle, ignoreCase = true)) {
                    return computeNextHintInput(expectedPrefix, fullTarget)
                }

                // Already starts with the right article -> reveal next letter of the word
                if (normalized.startsWith(expectedPrefix, ignoreCase = true)) {
                    return computeNextHintInput(normalized, fullTarget)
                }

                // If user typed a wrong article followed by a word (e.g. "die Apfel"),
                // replace ONLY the article and preserve the typed word!
                val firstSpace = normalized.indexOf(' ')
                if (firstSpace > 0) {
                    val firstToken = normalized.substring(0, firstSpace).lowercase()
                    val remainder = normalized.substring(firstSpace + 1).trim()
                    if (firstToken in listOf("der", "die", "das")) {
                        return if (remainder.isNotEmpty()) {
                            "$expectedArticle $remainder"
                        } else {
                            expectedPrefix
                        }
                    }
                    // Multi-word input without any article -> prepend the correct article
                    return "$expectedArticle $normalized"
                }

                // Single token: if it's a wrong article or partial article ("d", "de", "da", "der", "die", "das"), replace with correct article
                val low = normalized.lowercase()
                if (low in listOf("der", "die", "das", "d", "de", "da")) {
                    return expectedPrefix
                }

                // User typed the noun without an article (e.g. "Apfel" or "Apf") -> prepend the correct article and keep their word
                return "$expectedArticle $normalized"
            }
            return computeNextHintInput(normalizeSpellingSpaces(current), fullTarget)
        }

        /**
         * Evaluates keyboard typing in Stage 6 (Lückentext / Fill-in-the-blank).
         * Accepts both the exact inflected token inside the sentence (`matchedToken`)
         * and the base dictionary form (`baseGerman`), with lenient umlaut handling.
         */
        internal fun evaluateFillBlankKeyboardAnswer(
            rawInput: String,
            matchedToken: String,
            baseGerman: String
        ): FillBlankKeyboardEvaluation {
            val cleaned = normalizeSpellingSpaces(rawInput)
            val tokenTarget = matchedToken.trim().ifEmpty { baseGerman.trim() }
            val baseTarget = baseGerman.trim()

            if (cleaned.equals(tokenTarget, ignoreCase = true)) {
                return FillBlankKeyboardEvaluation.ExactMatch(tokenTarget)
            }
            if (baseTarget.isNotEmpty() && cleaned.equals(baseTarget, ignoreCase = true)) {
                return FillBlankKeyboardEvaluation.ExactMatch(tokenTarget)
            }
            if (matchesWithLenientUmlauts(cleaned, tokenTarget)) {
                return FillBlankKeyboardEvaluation.LenientUmlautMatch(tokenTarget)
            }
            if (baseTarget.isNotEmpty() && matchesWithLenientUmlauts(cleaned, baseTarget)) {
                return FillBlankKeyboardEvaluation.LenientUmlautMatch(tokenTarget)
            }

            val prefixToken = keepCorrectPrefix(cleaned, tokenTarget, allowLenientUmlauts = true)
            val prefixBase = if (baseTarget.isNotEmpty()) {
                keepCorrectPrefix(cleaned, baseTarget, allowLenientUmlauts = true)
            } else {
                ""
            }
            val bestPrefix = if (prefixToken.length >= prefixBase.length) prefixToken else prefixBase
            return FillBlankKeyboardEvaluation.WrongInput(bestPrefix)
        }

        /**
         * Builds a guaranteed-valid fill-in-the-blank sentence for Stage 6 / Final Mix / Mixed Review.
         * Uses whole-word German letter boundaries so substrings like "Ei" inside "Ein" are never masked,
         * and returns null if the example sentence does not actually contain the target word (or its clear inflection).
         */
        internal fun buildFillBlankMatch(
            word: WordEntity,
            exampleSentence: String = word.exampleDe
        ): FillBlankMatch? {
            val example = exampleSentence.trim()
            val target = word.german.trim()
            if (example.isEmpty() || target.isEmpty()) return null

            val leftBoundary = "(?<![A-Za-zÄÖÜäöüß])"
            val rightBoundary = "(?![A-Za-zÄÖÜäöüß])"

            // 1. Exact whole-word match (case-insensitive)
            val exactRegex = Regex("$leftBoundary${Regex.escape(target)}$rightBoundary", RegexOption.IGNORE_CASE)
            exactRegex.find(example)?.let { match ->
                val blanked = example.replaceRange(match.range, "_________")
                if (blanked.contains("_________")) {
                    return FillBlankMatch(sentenceWithBlank = blanked, matchedToken = match.value)
                }
            }

            // 2. Exact plural noun match (e.g., Zahn -> Zähne, Hand -> Hände, Glocke -> Glocken)
            val rawPlural = word.plural.trim()
            if (rawPlural.startsWith("die ", ignoreCase = true) && rawPlural.length > 4) {
                val pluralForm = rawPlural.substring(4).trim()
                if (pluralForm.isNotEmpty() && !pluralForm.contains(" ")) {
                    val pluralRegex = Regex("$leftBoundary${Regex.escape(pluralForm)}$rightBoundary", RegexOption.IGNORE_CASE)
                    pluralRegex.find(example)?.let { match ->
                        val blanked = example.replaceRange(match.range, "_________")
                        if (blanked.contains("_________")) {
                            return FillBlankMatch(sentenceWithBlank = blanked, matchedToken = match.value)
                        }
                    }
                }
            }

            // 3. Simple German inflection (noun/adjective ending -e/-en/-er/-es/-em/-n/-s on stems >= 3 chars, or verb conjugation)
            if (!target.contains(" ") && target.length >= 3) {
                val inflectedRegex = Regex(
                    "$leftBoundary${Regex.escape(target)}(?:e|en|er|es|em|n|s)$rightBoundary",
                    RegexOption.IGNORE_CASE
                )
                inflectedRegex.find(example)?.let { match ->
                    val blanked = example.replaceRange(match.range, "_________")
                    if (blanked.contains("_________")) {
                        return FillBlankMatch(sentenceWithBlank = blanked, matchedToken = match.value)
                    }
                }

                // Regular verb conjugation (e.g. gehen -> gehe/geht/ging, sprechen -> spreche, kaufen -> kaufe)
                if (word.article.isBlank() && target.length >= 4 && target.endsWith("en")) {
                    val stem = target.dropLast(2)
                    if (stem.length >= 3) {
                        val verbRegex = Regex(
                            "$leftBoundary${Regex.escape(stem)}(?:e|st|t|en|te|ten|test|tet|est|et)$rightBoundary",
                            RegexOption.IGNORE_CASE
                        )
                        verbRegex.find(example)?.let { match ->
                            val blanked = example.replaceRange(match.range, "_________")
                            if (blanked.contains("_________")) {
                                return FillBlankMatch(sentenceWithBlank = blanked, matchedToken = match.value)
                            }
                        }
                    }
                }
            }

            return null
        }

        /**
         * Picks a balanced practice stage (2..7) for Stage 8 Final Mix so every word
         * gets an eligible stage and all enabled stages appear evenly across the round.
         */
        internal fun pickFinalMixStageForWord(
            word: WordEntity,
            enabledPracticeStages: Set<Int>,
            stageUsageCounts: Map<Int, Int>,
            previousStageType: Int?
        ): Int {
            val eligible = mutableListOf<Int>()
            if (2 in enabledPracticeStages) eligible.add(2)
            if (3 in enabledPracticeStages) eligible.add(3)
            if (4 in enabledPracticeStages && requiresArticleInSpelling(word)) eligible.add(4)
            if (5 in enabledPracticeStages && isPluralizableNoun(word)) eligible.add(5)
            if (6 in enabledPracticeStages && buildFillBlankMatch(word) != null) {
                eligible.add(6)
            }
            if (7 in enabledPracticeStages && word.german.trim().length in 2..24) eligible.add(7)

            if (eligible.isEmpty()) return 3
            val varied = eligible.filter { it != previousStageType }.ifEmpty { eligible }
            val minUsage = varied.minOf { stageUsageCounts[it] ?: 0 }
            val leastUsed = varied.filter { (stageUsageCounts[it] ?: 0) == minUsage }
            return leastUsed.random()
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
            if (4 in enabledStages && requiresArticleInSpelling(word)) {
                eligible.add(4)
            }
            if (5 in enabledStages && isPluralizableNoun(word)) {
                eligible.add(5)
            }
            if (6 in enabledStages && buildFillBlankMatch(word) != null) {
                eligible.add(6)
            }
            if (7 in enabledStages && word.german.trim().length in 2..24) {
                eligible.add(7)
            }
            if (eligible.isEmpty()) return (enabledStages intersect setOf(1, 2, 3, 4, 5, 6, 7)).minOrNull() ?: 3

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
    }
}

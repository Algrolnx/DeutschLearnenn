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
import com.example.util.TtsManager
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

    object Progress : Screen()
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
    private val _stage6Status = MutableStateFlow<Boolean?>(null)
    val stage6Status: StateFlow<Boolean?> = _stage6Status.asStateFlow()
    private val _stage6Score = MutableStateFlow(0)
    val stage6Score: StateFlow<Int> = _stage6Score.asStateFlow()
    private val _stage6Finished = MutableStateFlow(false)
    val stage6Finished: StateFlow<Boolean> = _stage6Finished.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VocabularyRepository(db.wordDao(), application.applicationContext)

        viewModelScope.launch {
            repository.initializePrepopulatedDataIfNeeded()
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

    fun navigateBack(): Boolean {
        if (_screenStack.value.size > 1) {
            _screenStack.value = _screenStack.value.dropLast(1)
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
            navigateTo(Screen.Stage1Flashcards(subtopicId))
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
                    repository.completeStage(subtopicId, stage = 1, score = words.size)
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
            navigateTo(Screen.Stage2Listening(subtopicId))
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
                repository.completeStage(subtopicId, stage = 2, score = _stage2Score.value)
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
            navigateTo(Screen.Stage3MultipleChoice(subtopicId))
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
                repository.completeStage(subtopicId, stage = 3, score = _stage3Score.value)
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
            navigateTo(Screen.Stage4ArticleTrainer(subtopicId))
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
                repository.completeStage(subtopicId, stage = 4, score = _stage4Score.value)
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
            navigateTo(Screen.Stage5FillBlank(subtopicId))
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
                repository.completeStage(subtopicId, stage = 5, score = _stage5Score.value)
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
            navigateTo(Screen.Stage6Spelling(subtopicId))
        }
    }

    private fun prepareStage6Word(index: Int) {
        val target = _stage6Words.value.getOrNull(index) ?: return
        _stage6Input.value = ""
        _stage6Status.value = null

        val targetLetters = target.german.trim().toList()
        val extraLetters = listOf('e', 'a', 'i', 'r', 's', 't', 'ä', 'ö', 'ü').shuffled().take(3)
        _stage6LetterPool.value = (targetLetters + extraLetters).shuffled()
        speak(target.displayWithArticle)
    }

    fun addStage6Letter(char: Char) {
        if (_stage6Status.value == true) return
        val newText = _stage6Input.value + char
        _stage6Input.value = newText
        val target = _stage6Words.value.getOrNull(_stage6Index.value)?.german?.trim() ?: return

        if (newText.equals(target, ignoreCase = true)) {
            _stage6Status.value = true
            _stage6Score.value += 1
            speak(target)
            _stage6Words.value.getOrNull(_stage6Index.value)?.let {
                viewModelScope.launch { repository.recordReview(it.id, ReviewRating.GOOD) }
            }
        } else if (newText.length >= target.length) {
            _stage6Status.value = false
        } else {
            _stage6Status.value = null
        }
    }

    fun removeStage6Letter() {
        if (_stage6Input.value.isNotEmpty()) {
            _stage6Input.value = _stage6Input.value.dropLast(1)
            _stage6Status.value = null
        }
    }

    fun clearStage6() {
        _stage6Input.value = ""
        _stage6Status.value = null
    }

    fun nextStage6(subtopicId: String) {
        val nextIdx = _stage6Index.value + 1
        if (nextIdx < _stage6Words.value.size) {
            _stage6Index.value = nextIdx
            prepareStage6Word(nextIdx)
        } else {
            _stage6Finished.value = true
            viewModelScope.launch {
                repository.completeStage(subtopicId, stage = 6, score = _stage6Score.value)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

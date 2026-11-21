package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LearningViewModel
import com.example.ui.Screen
import com.example.ui.screens.MainTopicsScreen
import com.example.ui.screens.MixedReviewSessionScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.StageArticleScreen
import com.example.ui.screens.StageFillBlankScreen
import com.example.ui.screens.StageFlashcardsScreen
import com.example.ui.screens.StageListeningScreen
import com.example.ui.screens.StageMultipleChoiceScreen
import com.example.ui.screens.StageSpellingScreen
import com.example.ui.screens.SubtopicHubScreen
import com.example.ui.screens.SubtopicsListScreen
import com.example.ui.theme.DeutschLernTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LearningViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

            DeutschLernTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screenTransition"
                    ) { screen ->
                        when (screen) {
                            is Screen.MainTopics -> MainTopicsScreen(viewModel = viewModel)
                            is Screen.SubtopicsList -> SubtopicsListScreen(
                                mainTopicId = screen.mainTopicId,
                                viewModel = viewModel
                            )
                            is Screen.SubtopicHub -> SubtopicHubScreen(
                                subtopicId = screen.subtopicId,
                                viewModel = viewModel
                            )
                            is Screen.Stage1Flashcards -> StageFlashcardsScreen(
                                subtopicId = screen.subtopicId,
                                viewModel = viewModel
                            )
                            is Screen.Stage2Listening -> StageListeningScreen(
                                subtopicId = screen.subtopicId,
                                viewModel = viewModel
                            )
                            is Screen.Stage3MultipleChoice -> StageMultipleChoiceScreen(
                                subtopicId = screen.subtopicId,
                                viewModel = viewModel
                            )
                            is Screen.Stage4ArticleTrainer -> StageArticleScreen(
                                subtopicId = screen.subtopicId,
                                viewModel = viewModel
                            )
                            is Screen.Stage5FillBlank -> StageFillBlankScreen(
                                subtopicId = screen.subtopicId,
                                viewModel = viewModel
                            )
                            is Screen.Stage6Spelling -> StageSpellingScreen(
                                subtopicId = screen.subtopicId,
                                viewModel = viewModel
                            )
                            is Screen.MixedReview -> MixedReviewSessionScreen(viewModel = viewModel)
                            is Screen.Progress -> ProgressScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

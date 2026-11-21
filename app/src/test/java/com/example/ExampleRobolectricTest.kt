package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.util.ReviewRating
import com.example.util.SpacedRepetition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DeutschLern", appName)
  }

  @Test
  fun `test spaced repetition calculation`() {
    val resultAgain = SpacedRepetition.calculateNextReview(currentBox = 3, rating = ReviewRating.AGAIN)
    assertEquals(1, resultAgain.newBox)

    val resultGood = SpacedRepetition.calculateNextReview(currentBox = 2, rating = ReviewRating.GOOD)
    assertEquals(3, resultGood.newBox)
    assertTrue(resultGood.isCorrect)

    val resultEasy = SpacedRepetition.calculateNextReview(currentBox = 2, rating = ReviewRating.EASY)
    assertEquals(4, resultEasy.newBox)
  }

  @Test
  fun `main activity launches cleanly and opens food topic`() {
    composeTestRule.waitUntil(timeoutMillis = 15_000) {
      composeTestRule.onAllNodesWithText("DeutschLern").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("DeutschLern").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_topic_food").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("Essen & Trinken").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Essen & Trinken").assertIsDisplayed()
  }
}

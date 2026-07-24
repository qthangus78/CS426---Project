package com.topic11.cs426

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FieldFlowNavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardInspectionAndPlaceholdersNavigateWithBack() {
        assertDashboardVisible()

        // Ensure "All" filter is selected to see everything - scroll to it first
        composeRule.onNodeWithTag("dashboard-content").performScrollToNode(hasText("All"))
        composeRule.onNodeWithText("All").performClick()

        // In-progress inspection (might be in hero card or list) - scroll to it first
        composeRule.onNodeWithTag("dashboard-content").performScrollToNode(hasText("Computer Lab I.44"))
        composeRule.onNodeWithText("Computer Lab I.44").performClick()

        // Assert top bar title and progress
        composeRule.onNodeWithText("Computer Lab I.44").assertIsDisplayed()
        composeRule.onNodeWithText("0 of 7 items answered").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertDashboardVisible()

        // Not started inspection - scroll to it if necessary
        composeRule.onNodeWithTag("dashboard-content").performScrollToNode(hasText("Projector P-204"))
        composeRule.onNodeWithText("Projector P-204").performClick()

        composeRule.onNodeWithText("Projector P-204").assertIsDisplayed()
        composeRule.onNodeWithText("0 of 7 items answered").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertDashboardVisible()

        // Quick actions navigation
        composeRule.onNodeWithTag("dashboard-content").performScrollToNode(hasText("Quick actions"))

        // Assets placeholder
        composeRule.onNodeWithText("Assets").performClick()
        composeRule.onNodeWithText("Assets").assertIsDisplayed()
        composeRule.onNodeWithText("Future responsibilities").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertDashboardVisible()

        // Templates placeholder
        composeRule.onNodeWithTag("dashboard-content").performScrollToNode(hasText("Templates"))
        composeRule.onNodeWithText("Templates").performClick()
        composeRule.onNodeWithText("Templates").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertDashboardVisible()
    }

    private fun assertDashboardVisible() {
        composeRule.onNodeWithText("FieldFlow").assertIsDisplayed()
        composeRule.onNodeWithText("Architecture Bootstrap").assertIsDisplayed()
    }
}

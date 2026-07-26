package com.topic11.cs426

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FieldFlowNavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardInspectionDraftFlowReturnsToDashboard() {
        assertDashboardVisible()

        composeRule
            .onNodeWithTag("dashboard-continue-inspection")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText("Section", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Pass").onFirst().performScrollTo().performClick()
        composeRule.onNodeWithText("Save Draft").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save Draft").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        assertDashboardVisible()
    }

    @Test
    fun dashboardStartInspectionCreatesDraftAndOpensInspection() {
        assertDashboardVisible()

        composeRule
            .onNodeWithTag("dashboard-start-inspection")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithTag("start-inspection-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("start-inspection-confirm").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("Section", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Section", substring = true).assertIsDisplayed()
    }

    private fun assertDashboardVisible() {
        composeRule.onNodeWithTag("dashboard-root").assertIsDisplayed()
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }
}

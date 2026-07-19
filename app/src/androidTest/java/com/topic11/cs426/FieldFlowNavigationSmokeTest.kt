package com.topic11.cs426

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        composeRule.onNodeWithText("Computer Lab I.44").performClick()
        composeRule.onNodeWithText("Inspection").assertIsDisplayed()
        composeRule.onNodeWithText("Computer Lab I.44").assertIsDisplayed()
        composeRule.onNodeWithText("6 of 10 items complete").assertIsDisplayed()
        composeRule.onNodeWithText("Full checklist workflow will be implemented later.").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertDashboardVisible()

        openPlaceholderAndReturn("Assets", "- locations")
        openPlaceholderAndReturn("Templates", "- checklist items")
        openPlaceholderAndReturn("Issues", "- issue severity")
        openPlaceholderAndReturn("Reports", "- PDF and JSON exporters behind Domain ports")
        openPlaceholderAndReturn("Assets", "- rooms")
    }

    private fun openPlaceholderAndReturn(
        buttonText: String,
        futureResponsibilityText: String,
    ) {
        composeRule.onNodeWithText(buttonText).performClick()
        composeRule.onNodeWithText("Not implemented yet.").assertIsDisplayed()
        composeRule.onNodeWithText(futureResponsibilityText).assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        assertDashboardVisible()
    }

    private fun assertDashboardVisible() {
        composeRule.onNodeWithText("FieldFlow").assertIsDisplayed()
        composeRule.onNodeWithText("Architecture Bootstrap").assertIsDisplayed()
        composeRule.onNodeWithText("Computer Lab I.44").assertIsDisplayed()
        composeRule.onNodeWithText("Projector P-204").assertIsDisplayed()
        composeRule.onNodeWithText("Laboratory A2 Safety Check").assertIsDisplayed()
    }
}

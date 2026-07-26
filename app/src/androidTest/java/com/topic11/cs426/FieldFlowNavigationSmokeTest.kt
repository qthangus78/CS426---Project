package com.topic11.cs426

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FieldFlowNavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardInspectionDraftFlowReturnsToDashboard() {
        awaitDashboardLoaded()
        assertDashboardVisible()

        scrollDashboardTo(hasTestTag("dashboard-continue-inspection"))
        composeRule
            .onNodeWithTag("dashboard-continue-inspection")
            .assertIsDisplayed()
            .performClick()

        awaitInspectionEditorLoaded()
        composeRule.onNodeWithText("Section", substring = true).assertIsDisplayed()

        scrollChecklistTo(hasText("Pass"))
        composeRule.onAllNodesWithText("Pass").onFirst().performClick()

        scrollChecklistTo(hasText("Save Draft"))
        composeRule.onNodeWithText("Save Draft").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save Draft").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        awaitDashboardAfterInspection()
        assertDashboardVisible()
    }

    /**
     * The system back — not the top bar's "Back" — is the gesture-navigation path out of the editor,
     * and it has to flush the debounced draft too. This walks the reported case: answer an item, then
     * leave immediately, inside the autosave debounce window where the answer only exists in memory.
     */
    @Test
    fun systemBackFromInspectionFlushesDraftAndReturnsToDashboard() {
        awaitDashboardLoaded()
        openContinuedInspection()

        scrollChecklistTo(hasTestTag(UNANSWERED_ITEM_PASS_TAG))
        composeRule.onNodeWithTag(UNANSWERED_ITEM_PASS_TAG).performClick()
        val answeredBeforeLeaving = answeredItemCount()

        // No waiting here on purpose: the draft is still only in memory at this point.
        Espresso.pressBack()

        awaitDashboardAfterInspection()
        assertDashboardVisible()

        // Reopening reads the inspection back out of Room, so the answer survives only if system back
        // ran the same flush the top bar's "Back" does.
        openContinuedInspection()
        assertEquals(answeredBeforeLeaving, answeredItemCount())
    }

    /**
     * Every screen other than the inspection editor leans on the navigator's own back handling, which
     * only exists if the root opts into it. Going two records deep and pressing back twice is what
     * separates "back pops one record" from "back finishes the Activity" — the latter shows up here as
     * Espresso's NoActivityResumedException rather than a failed assertion.
     */
    @Test
    fun systemBackUnwindsNestedScreensInsteadOfLeavingTheApp() {
        awaitDashboardLoaded()

        scrollDashboardTo(hasTestTag("dashboard-assets"))
        composeRule.onNodeWithTag("dashboard-assets").performClick()
        awaitNode(hasTestTag("assets-root"))
        // The dashboard stays composed through the push animation, and it shows the same asset names,
        // so the asset row is only unambiguous once the transition has settled.
        awaitNodeGone(hasTestTag("dashboard-root"))

        composeRule.onNodeWithTag("assets-content").performScrollToNode(hasText(SAMPLE_ASSET_NAME))
        composeRule.onNodeWithText(SAMPLE_ASSET_NAME).performClick()
        awaitNode(hasTestTag("asset-detail-root"))

        Espresso.pressBack()
        awaitNodeGone(hasTestTag("asset-detail-root"))
        composeRule.onNodeWithTag("assets-root").assertIsDisplayed()

        Espresso.pressBack()
        awaitNodeGone(hasTestTag("assets-root"))
        assertDashboardVisible()
    }

    @Test
    fun dashboardStartInspectionCreatesDraftAndOpensInspection() {
        awaitDashboardLoaded()
        assertDashboardVisible()

        scrollDashboardTo(hasTestTag("dashboard-start-inspection"))
        composeRule
            .onNodeWithTag("dashboard-start-inspection")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithTag("start-inspection-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("start-inspection-confirm").performClick()
        awaitInspectionEditorLoaded()
        composeRule.onNodeWithText("Section", substring = true).assertIsDisplayed()
    }

    private fun assertDashboardVisible() {
        composeRule.onNodeWithTag("dashboard-root").assertIsDisplayed()
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    /**
     * Sample data is seeded on a background scope at app start, so the dashboard renders its
     * loading state first and none of the inspection content exists yet. Waiting for the root
     * first guarantees the loading spinner has actually been composed, so waiting for it to
     * disappear cannot pass before the first Room emission arrives.
     */
    private fun awaitDashboardLoaded() {
        awaitNode(hasTestTag("dashboard-root"))
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasTestTag("dashboard-loading")).fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * The inspection presenter starts in its loading state while it collects the session and
     * template from Room, and that loading screen now carries its own "Back" top bar. Waiting for
     * the section header is what distinguishes the loaded editor from the loading screen.
     */
    private fun awaitInspectionEditorLoaded() {
        awaitNode(hasText("Section", substring = true))
    }

    /**
     * Back on the editor flushes the pending draft before popping, so the pop happens on a
     * coroutine rather than synchronously inside the click. The dashboard is only back once the
     * editor's "Back" top bar is gone — the dashboard top bar has no back affordance.
     */
    private fun awaitDashboardAfterInspection() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasTestTag("dashboard-root")).fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Back").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitForIdle()
    }

    /**
     * Both the dashboard and the checklist are lazy lists, so anything below the fold is not
     * composed at all and has to be reached through the list rather than through the node itself.
     * Scrolling the list keeps the tests independent of the screen size they run on.
     */
    private fun scrollDashboardTo(matcher: SemanticsMatcher) {
        composeRule.onNodeWithTag("dashboard-content").performScrollToNode(matcher)
    }

    private fun scrollChecklistTo(matcher: SemanticsMatcher) {
        composeRule.onNodeWithTag("inspection-checklist").performScrollToNode(matcher)
    }

    private fun awaitNode(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitNodeGone(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun openContinuedInspection() {
        scrollDashboardTo(hasTestTag("dashboard-continue-inspection"))
        composeRule.onNodeWithTag("dashboard-continue-inspection").performClick()
        awaitInspectionEditorLoaded()
    }

    /**
     * Reads the count out of the editor's "N of M items answered" progress line. Comparing the count
     * the editor reported before leaving against the one it reports after reopening keeps the
     * assertion independent of how much of the sample inspection an earlier test already answered.
     */
    private fun answeredItemCount(): Int {
        composeRule.waitForIdle()
        val node = composeRule.onNode(hasText("items answered", substring = true)).fetchSemanticsNode()
        val text = node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        return requireNotNull(ANSWERED_COUNT.find(text)) { "Unexpected progress text: $text" }
            .groupValues[1]
            .toInt()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000L

        /** Third seeded checklist item — the first one the sample draft leaves unanswered. */
        const val UNANSWERED_ITEM_PASS_TAG = "inspection-answer-pass-sample-item-2"

        /** Seeded asset whose name is not also a checklist item or an inspection title. */
        const val SAMPLE_ASSET_NAME = "Projector P-204"

        val ANSWERED_COUNT = Regex("""(\d+) of \d+ items answered""")
    }
}

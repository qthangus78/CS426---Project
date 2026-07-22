package com.topic11.cs426.feature.inspection

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionPresenterTest {

    private val screen = InspectionScreen("computer-lab-i-44")

    private fun presenter(navigator: FakeNavigator = FakeNavigator(DashboardScreen, screen)) =
        InspectionPresenter(screen = screen, navigator = navigator)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Editing at section zero`() = runTest {
        presenter().test {
            val state = awaitItem()
            assertTrue(state is InspectionState.Editing)
            state as InspectionState.Editing
            assertEquals(0, state.currentSectionIndex)
            assertEquals(FakeSession.sections.size, state.sections.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial progress has no completed items`() = runTest {
        presenter().test {
            val state = awaitItem() as InspectionState.Editing
            assertEquals(0, state.progress.completedItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── AnswerChanged ─────────────────────────────────────────────────────────

    @Test
    fun `AnswerChanged updates the item answer and increments progress`() = runTest {
        presenter().test {
            val editing = awaitItem() as InspectionState.Editing
            val firstItem = editing.sections.first().items.first()

            editing.eventSink(InspectionEvent.AnswerChanged(firstItem.id, ChecklistAnswerUi.Compliance(true)))

            val updated = awaitItem() as InspectionState.Editing
            val updatedItem = updated.sections.first().items.first()
            assertEquals(ChecklistAnswerUi.Compliance(true), updatedItem.answer)
            assertEquals(1, updated.progress.completedItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Section navigation ────────────────────────────────────────────────────

    @Test
    fun `NextSection advances the section index`() = runTest {
        presenter().test {
            val editing = awaitItem() as InspectionState.Editing
            assertEquals(0, editing.currentSectionIndex)

            editing.eventSink(InspectionEvent.NextSection)

            val next = awaitItem() as InspectionState.Editing
            assertEquals(1, next.currentSectionIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PreviousSection at index zero does not change section`() = runTest {
        presenter().test {
            val editing = awaitItem() as InspectionState.Editing
            assertFalse(editing.hasPreviousSection)

            editing.eventSink(InspectionEvent.PreviousSection)

            // No new state should be emitted since nothing changed
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `PreviousSection after NextSection returns to first section`() = runTest {
        presenter().test {
            val first = awaitItem() as InspectionState.Editing
            first.eventSink(InspectionEvent.NextSection)

            val second = awaitItem() as InspectionState.Editing
            assertEquals(1, second.currentSectionIndex)

            second.eventSink(InspectionEvent.PreviousSection)

            val backToFirst = awaitItem() as InspectionState.Editing
            assertEquals(0, backToFirst.currentSectionIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `NextSection at last section does not change section`() = runTest {
        val lastIndex = FakeSession.sections.lastIndex
        presenter().test {
            var state = awaitItem() as InspectionState.Editing
            repeat(lastIndex) {
                state.eventSink(InspectionEvent.NextSection)
                state = awaitItem() as InspectionState.Editing
            }
            assertEquals(lastIndex, state.currentSectionIndex)
            assertFalse(state.hasNextSection)

            state.eventSink(InspectionEvent.NextSection)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── ReviewSelected ────────────────────────────────────────────────────────

    @Test
    fun `ReviewSelected transitions to Reviewing`() = runTest {
        presenter().test {
            val editing = awaitItem() as InspectionState.Editing
            editing.eventSink(InspectionEvent.ReviewSelected)

            val reviewing = awaitItem()
            assertTrue(reviewing is InspectionState.Reviewing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `BackSelected from Reviewing returns to Editing`() = runTest {
        presenter().test {
            val editing = awaitItem() as InspectionState.Editing
            editing.eventSink(InspectionEvent.ReviewSelected)

            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.BackSelected)

            val backToEditing = awaitItem()
            assertTrue(backToEditing is InspectionState.Editing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── CompleteSelected — validation failure ─────────────────────────────────

    @Test
    fun `CompleteSelected with unanswered required items transitions to ValidationFailed`() = runTest {
        presenter().test {
            val editing = awaitItem() as InspectionState.Editing
            editing.eventSink(InspectionEvent.ReviewSelected)

            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val failed = awaitItem()
            assertTrue(failed is InspectionState.ValidationFailed)
            failed as InspectionState.ValidationFailed

            val requiredCount = FakeSession.sections
                .flatMap { it.items }
                .count { it.required }
            assertEquals(requiredCount, failed.errors.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `BackSelected from ValidationFailed returns to Editing and clears errors`() = runTest {
        presenter().test {
            // Editing (no answers) → Review → CompleteSelected triggers validation
            val editing = awaitItem() as InspectionState.Editing
            editing.eventSink(InspectionEvent.ReviewSelected)

            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val failed = awaitItem() as InspectionState.ValidationFailed
            assertTrue(failed.errors.isNotEmpty())

            failed.eventSink(InspectionEvent.BackSelected)

            val backToEditing = awaitItem()
            assertTrue(backToEditing is InspectionState.Editing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── CompleteSelected — success ────────────────────────────────────────────

    @Test
    fun `CompleteSelected with all required items answered transitions to Completed`() = runTest {
        presenter().test {
            // Answer all required items
            var state = awaitItem() as InspectionState.Editing
            FakeSession.sections.flatMap { it.items }.filter { it.required }.forEach { item ->
                state.eventSink(InspectionEvent.AnswerChanged(item.id, ChecklistAnswerUi.Compliance(true)))
                state = awaitItem() as InspectionState.Editing
            }

            state.eventSink(InspectionEvent.ReviewSelected)
            val reviewing = awaitItem() as InspectionState.Reviewing

            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val completed = awaitItem()
            assertTrue(completed is InspectionState.Completed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── BackSelected from Editing ─────────────────────────────────────────────

    @Test
    fun `BackSelected from Editing pops the navigator`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, screen)
        presenter(navigator).test {
            val editing = awaitItem() as InspectionState.Editing
            editing.eventSink(InspectionEvent.BackSelected)

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── BackSelected from Completed ────────────────────────────────────────────

    @Test
    fun `BackSelected from Completed pops the navigator`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, screen)
        presenter(navigator).test {
            var state = awaitItem() as InspectionState.Editing
            FakeSession.sections.flatMap { it.items }.filter { it.required }.forEach { item ->
                state.eventSink(InspectionEvent.AnswerChanged(item.id, ChecklistAnswerUi.Compliance(true)))
                state = awaitItem() as InspectionState.Editing
            }
            state.eventSink(InspectionEvent.ReviewSelected)
            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val completed = awaitItem() as InspectionState.Completed
            completed.eventSink(InspectionEvent.BackSelected)

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

package com.topic11.cs426.feature.inspection

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionPresenterTest {

    private val screen = InspectionScreen("computer-lab-i-44")

    private fun presenter(navigator: FakeNavigator = FakeNavigator(DashboardScreen, screen)): InspectionPresenter {
        val fakeInspectionRepo = FakeInspectionRepository()
        val fakeTemplateRepo = FakeTemplateRepository()
        return InspectionPresenter(
            screen = screen,
            navigator = navigator,
            observeInspection = ObserveInspectionUseCase(fakeInspectionRepo),
            templateRepository = fakeTemplateRepo,
        )
    }

    // Helper: consumes Loading states and returns the first Editing state.
    // Phase 2 presenters always emit Loading before the first Editing.
    private suspend fun ReceiveTurbine<InspectionState>.awaitEditing(): InspectionState.Editing {
        var state = awaitItem()
        while (state is InspectionState.Loading) state = awaitItem()
        return state as InspectionState.Editing
    }

    // ── Loading state ──────────────────────────────────────────────────────────

    @Test
    fun `loading state is emitted before session arrives`() = runTest {
        presenter().test {
            val first = awaitItem()
            assertTrue("Expected Loading as first state", first is InspectionState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Initial state ──────────────────────────────────────────────────────────

    @Test
    fun `initial state is Editing at section zero`() = runTest {
        presenter().test {
            val state = awaitEditing()
            assertEquals(0, state.currentSectionIndex)
            assertEquals(FakeSession.sections.size, state.sections.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial progress has no completed items`() = runTest {
        presenter().test {
            val state = awaitEditing()
            assertEquals(0, state.progress.completedItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── AnswerChanged ──────────────────────────────────────────────────────────

    @Test
    fun `AnswerChanged updates the item answer and increments progress`() = runTest {
        presenter().test {
            val editing = awaitEditing()
            val firstItem = editing.sections.first().items.first()

            editing.eventSink(InspectionEvent.AnswerChanged(firstItem.id, ChecklistAnswerUi.Compliance(true)))

            val updated = awaitItem() as InspectionState.Editing
            val updatedItem = updated.sections.first().items.first()
            assertEquals(ChecklistAnswerUi.Compliance(true), updatedItem.answer)
            assertEquals(1, updated.progress.completedItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Section navigation ─────────────────────────────────────────────────────

    @Test
    fun `NextSection advances the section index`() = runTest {
        presenter().test {
            val editing = awaitEditing()
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
            val editing = awaitEditing()
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
            val first = awaitEditing()
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
            var state = awaitEditing()
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

    // ── ReviewSelected ─────────────────────────────────────────────────────────

    @Test
    fun `ReviewSelected transitions to Reviewing`() = runTest {
        presenter().test {
            val editing = awaitEditing()
            editing.eventSink(InspectionEvent.ReviewSelected)

            val reviewing = awaitItem()
            assertTrue(reviewing is InspectionState.Reviewing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `BackSelected from Reviewing returns to Editing`() = runTest {
        presenter().test {
            val editing = awaitEditing()
            editing.eventSink(InspectionEvent.ReviewSelected)

            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.BackSelected)

            val backToEditing = awaitItem()
            assertTrue(backToEditing is InspectionState.Editing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── CompleteSelected — validation failure ──────────────────────────────────

    @Test
    fun `CompleteSelected with unanswered required items transitions to ValidationFailed`() = runTest {
        presenter().test {
            val editing = awaitEditing()
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
            val editing = awaitEditing()
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

    // ── CompleteSelected — success ─────────────────────────────────────────────

    @Test
    fun `CompleteSelected with all required items answered transitions to Completed`() = runTest {
        presenter().test {
            var state = awaitEditing()
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

    // ── BackSelected from Editing ──────────────────────────────────────────────

    @Test
    fun `BackSelected from Editing pops the navigator`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, screen)
        presenter(navigator).test {
            val editing = awaitEditing()
            editing.eventSink(InspectionEvent.BackSelected)

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── NoteChanged ───────────────────────────────────────────────────────────

    @Test
    fun `NoteChanged updates the item note`() = runTest {
        presenter().test {
            val editing = awaitEditing()
            val firstItem = editing.sections.first().items.first()

            editing.eventSink(InspectionEvent.NoteChanged(firstItem.id, "test note"))

            val updated = awaitItem() as InspectionState.Editing
            val updatedItem = updated.sections.first().items.first()
            assertEquals("test note", updatedItem.note)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── EvidenceAdded ─────────────────────────────────────────────────────────

    @Test
    fun `EvidenceAdded increments the evidence count for the item`() = runTest {
        presenter().test {
            val editing = awaitEditing()
            val firstItem = editing.sections.first().items.first()
            assertEquals(0, firstItem.evidenceCount)

            editing.eventSink(InspectionEvent.EvidenceAdded(firstItem.id, "photo-stub"))

            val updated = awaitItem() as InspectionState.Editing
            assertEquals(1, updated.sections.first().items.first().evidenceCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `EvidenceAdded accumulates count on repeated calls`() = runTest {
        presenter().test {
            var state = awaitEditing()
            val firstItem = state.sections.first().items.first()

            state.eventSink(InspectionEvent.EvidenceAdded(firstItem.id, "photo-1"))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.EvidenceAdded(firstItem.id, "photo-2"))
            state = awaitItem() as InspectionState.Editing

            assertEquals(2, state.sections.first().items.first().evidenceCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── BackSelected from Completed ────────────────────────────────────────────

    @Test
    fun `BackSelected from Completed pops the navigator`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, screen)
        presenter(navigator).test {
            var state = awaitEditing()
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

// ── Test fakes ────────────────────────────────────────────────────────────────

/**
 * Minimal fake that returns a single in-progress [InspectionSession] for the
 * "computer-lab-i-44" inspection used in all presenter tests.
 */
private class FakeInspectionRepository : InspectionRepository {
    private val session = InspectionSession(
        id = InspectionId("computer-lab-i-44"),
        assetId = AssetId("asset-lab-1"),
        assetName = "Computer Lab I.44",
        templateId = TemplateId("template-standard"),
        status = InspectionStatus.IN_PROGRESS,
        answers = emptyList(),
        startedAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSession?> =
        MutableStateFlow(session)

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> =
        flowOf(emptyList())

    override suspend fun getInspection(inspectionId: InspectionId): InspectionSession = session

    override suspend fun createInspection(
        assetId: String,
        assetName: String,
        templateId: String,
        startedAtMillis: Long,
    ): InspectionId = session.id

    override suspend fun saveDraft(session: InspectionSession) = Unit

    override suspend fun complete(completed: CompletedInspection) = Unit
}

/**
 * Fake template repository returning an [InspectionTemplate] whose section/item IDs
 * and required-flags mirror [FakeSession.sections], so existing test assertions on
 * item counts and required counts remain valid after the Phase 2 domain wiring.
 */
private class FakeTemplateRepository : TemplateRepository {
    private val templateId = TemplateId("template-standard")
    private val template = InspectionTemplate(
        id = templateId,
        name = "Standard Inspection",
        version = 1,
        sections = listOf(
            InspectionSection(
                id = SectionId("section-equipment"),
                templateId = templateId,
                title = "Equipment Condition",
                order = 0,
                items = listOf(
                    ChecklistItem(id = ChecklistItemId("item-power"),   sectionId = SectionId("section-equipment"), title = "Power supply is operational",        required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
                    ChecklistItem(id = ChecklistItemId("item-cables"),  sectionId = SectionId("section-equipment"), title = "Cables and connectors are undamaged", required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
                    ChecklistItem(id = ChecklistItemId("item-label"),   sectionId = SectionId("section-equipment"), title = "Asset label is visible",              required = false, answerType = ChecklistAnswerType.PASS_FAIL_NA),
                ),
            ),
            InspectionSection(
                id = SectionId("section-safety"),
                templateId = templateId,
                title = "Safety Checks",
                order = 1,
                items = listOf(
                    ChecklistItem(id = ChecklistItemId("item-fire"),    sectionId = SectionId("section-safety"), title = "Fire extinguisher is accessible",    required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
                    ChecklistItem(id = ChecklistItemId("item-exit"),    sectionId = SectionId("section-safety"), title = "Emergency exit is unobstructed",    required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
                ),
            ),
            InspectionSection(
                id = SectionId("section-environment"),
                templateId = templateId,
                title = "Environment",
                order = 2,
                items = listOf(
                    ChecklistItem(id = ChecklistItemId("item-lighting"), sectionId = SectionId("section-environment"), title = "Lighting is adequate",                        required = false, answerType = ChecklistAnswerType.PASS_FAIL_NA),
                    ChecklistItem(id = ChecklistItemId("item-temp"),     sectionId = SectionId("section-environment"), title = "Temperature is within operating range", required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
                ),
            ),
        ),
    )

    override fun observeTemplates(): Flow<List<InspectionTemplateSummary>> = flowOf(emptyList())
    override fun observeTemplate(id: TemplateId): Flow<InspectionTemplate?> = flowOf(template)
    override suspend fun getTemplate(id: TemplateId): InspectionTemplate = template
}

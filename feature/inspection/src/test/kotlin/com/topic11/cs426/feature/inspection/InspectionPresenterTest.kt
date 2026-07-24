package com.topic11.cs426.feature.inspection

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.InspectionAnswer
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
import com.topic11.cs426.domain.repository.IssueRepository
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.CalculateInspectionScoreUseCase
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.CreateMaintenanceIssueUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.SaveInspectionDraftUseCase
import com.topic11.cs426.domain.usecase.ScheduleNextInspectionUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionPresenterTest {

    private val screen = InspectionScreen("computer-lab-i-44")

    private fun presenter(
        navigator: FakeNavigator = FakeNavigator(DashboardScreen, screen),
        initialSession: InspectionSession = FakeInspectionRepository.createSession(),
        inspectionRepository: FakeInspectionRepository = FakeInspectionRepository(initialSession),
        issueRepository: FakeIssueRepository = FakeIssueRepository(),
    ): InspectionPresenter {
        val fakeTemplateRepo = FakeTemplateRepository()
        return InspectionPresenter(
            screen = screen,
            navigator = navigator,
            observeInspection = ObserveInspectionUseCase(inspectionRepository),
            templateRepository = fakeTemplateRepo,
            saveInspectionDraft = SaveInspectionDraftUseCase(inspectionRepository),
            validateInspection = ValidateInspectionUseCase(),
            completeInspection = CompleteInspectionUseCase(
                inspectionRepository = inspectionRepository,
                templateRepository = fakeTemplateRepo,
                issueRepository = issueRepository,
                validateInspection = ValidateInspectionUseCase(),
                calculateScore = CalculateInspectionScoreUseCase(),
                createIssue = CreateMaintenanceIssueUseCase(issueRepository),
                scheduleNext = ScheduleNextInspectionUseCase(),
            ),
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

    @Test
    fun `draft restore keeps answer note evidence and current section`() = runTest {
        val restoredSession = FakeInspectionRepository.createSession().copy(
            currentSectionId = SectionId("section-safety"),
            answers = listOf(
                InspectionAnswer(
                    inspectionId = InspectionId("computer-lab-i-44"),
                    checklistItemId = ChecklistItemId("item-power"),
                    value = com.topic11.cs426.domain.model.ChecklistAnswerValue.NotApplicable,
                    note = "Not installed in this room",
                    evidenceIds = listOf(EvidenceId("evidence-power-1")),
                    updatedAtMillis = 0L,
                ),
            ),
        )

        presenter(initialSession = restoredSession).test {
            val editing = awaitEditing()
            val restoredItem = editing.sections.first().items.first()

            assertEquals(1, editing.currentSectionIndex)
            assertEquals(ChecklistAnswerUi.NotApplicable, restoredItem.answer)
            assertEquals("Not installed in this room", restoredItem.note)
            assertEquals(listOf("evidence-power-1"), restoredItem.evidenceRefs)
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

    @Test
    fun `AnswerChanged with NotApplicable keeps the domain-backed UI value`() = runTest {
        presenter().test {
            val editing = awaitEditing()
            val firstItem = editing.sections.first().items.first()

            editing.eventSink(InspectionEvent.AnswerChanged(firstItem.id, ChecklistAnswerUi.NotApplicable))

            val updated = awaitItem() as InspectionState.Editing
            assertEquals(ChecklistAnswerUi.NotApplicable, updated.sections.first().items.first().answer)
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
    fun `BackSelected from ValidationFailed returns to Editing with the saved draft`() = runTest {
        val inspectionRepository = FakeInspectionRepository.create()

        presenter(inspectionRepository = inspectionRepository).test {
            var state = awaitEditing()
            val firstItem = state.sections.first().items.first()
            val criticalItemId = "item-fire"
            val remainingRequiredItemIds = state.sections
                .flatMap { it.items }
                .filter { it.required && it.id !in setOf(firstItem.id, criticalItemId) }
                .map { it.id }

            state.eventSink(InspectionEvent.AnswerChanged(firstItem.id, ChecklistAnswerUi.Compliance(true)))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.NoteChanged(firstItem.id, "Checked during follow-up"))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.EvidenceAdded(firstItem.id, "photo-power-1"))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.AnswerChanged(criticalItemId, ChecklistAnswerUi.Compliance(false)))
            state = awaitItem() as InspectionState.Editing
            remainingRequiredItemIds.forEach { itemId ->
                state.eventSink(InspectionEvent.AnswerChanged(itemId, ChecklistAnswerUi.Compliance(true)))
                state = awaitItem() as InspectionState.Editing
            }

            state.eventSink(InspectionEvent.ReviewSelected)
            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val failed = awaitItem() as InspectionState.ValidationFailed
            assertEquals(1, failed.errors.size)
            assertEquals(criticalItemId, failed.errors.single().itemId)

            failed.eventSink(InspectionEvent.BackSelected)

            val backToEditing = awaitItem() as InspectionState.Editing
            val restoredFirstItem = backToEditing.sections.first().items.first()
            val restoredCriticalItem = backToEditing.sections
                .flatMap { it.items }
                .single { it.id == criticalItemId }
            assertEquals(ChecklistAnswerUi.Compliance(true), restoredFirstItem.answer)
            assertEquals("Checked during follow-up", restoredFirstItem.note)
            assertEquals(listOf("photo-power-1"), restoredFirstItem.evidenceRefs)
            assertEquals(ChecklistAnswerUi.Compliance(false), restoredCriticalItem.answer)
            val savedFirstAnswer = inspectionRepository.savedSession.answers.single {
                it.checklistItemId == ChecklistItemId(firstItem.id)
            }
            assertEquals(ChecklistAnswerValue.Pass, savedFirstAnswer.value)
            assertEquals("Checked during follow-up", savedFirstAnswer.note)
            assertEquals(
                listOf(EvidenceId("photo-power-1")),
                savedFirstAnswer.evidenceIds,
            )
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

    @Test
    fun `CompleteSelected persists completion and updates the shared dashboard summary`() = runTest {
        val inspectionRepository = FakeInspectionRepository.create()
        val issueRepository = FakeIssueRepository()

        presenter(
            inspectionRepository = inspectionRepository,
            issueRepository = issueRepository,
        ).test {
            var state = awaitEditing()
            FakeSession.sections.flatMap { it.items }.filter { it.required }.forEach { item ->
                val answer = ChecklistAnswerUi.Compliance(isCompliant = item.id != "item-fire")
                state.eventSink(InspectionEvent.AnswerChanged(item.id, answer))
                state = awaitItem() as InspectionState.Editing
            }
            state.eventSink(InspectionEvent.EvidenceAdded("item-fire", "evidence-fire-1"))
            state = awaitItem() as InspectionState.Editing

            state.eventSink(InspectionEvent.ReviewSelected)
            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val completed = awaitItem() as InspectionState.Completed
            val persistedSession = inspectionRepository.savedSession
            val dashboardSummary = ObserveInspectionSummariesUseCase(inspectionRepository)()
                .first { summaries ->
                    summaries.single().status == InspectionStatus.COMPLETED
                }
                .single()

            assertEquals(5, persistedSession.answers.count { it.value != null })
            assertEquals(InspectionStatus.COMPLETED, persistedSession.status)
            assertEquals(completed.score, persistedSession.score?.percent)
            assertTrue(completed.score != null)
            assertEquals(1, completed.issueCount)
            assertEquals(1, issueRepository.createdIssues.size)
            assertEquals(
                ChecklistItemId("item-fire"),
                issueRepository.createdIssues.single().checklistItemId,
            )
            assertEquals(InspectionStatus.COMPLETED, dashboardSummary.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `CompleteSelected stays in review and shows an error when saving fails`() = runTest {
        val inspectionRepository = FakeInspectionRepository.create()

        presenter(inspectionRepository = inspectionRepository).test {
            var state = awaitEditing()
            FakeSession.sections.flatMap { it.items }.filter { it.required }.forEach { item ->
                state.eventSink(InspectionEvent.AnswerChanged(item.id, ChecklistAnswerUi.Compliance(true)))
                state = awaitItem() as InspectionState.Editing
            }

            state.eventSink(InspectionEvent.ReviewSelected)
            val reviewing = awaitItem() as InspectionState.Reviewing
            inspectionRepository.failSaves = true
            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val failedSave = awaitItem() as InspectionState.Reviewing
            assertEquals("Couldn't complete inspection.", failedSave.saveError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `CompleteSelected shows error when domain completion fails`() = runTest {
        val inspectionRepository = FakeInspectionRepository.create()
        // Force a domain error by using an inspection that doesn't exist in the "template" repo fake
        // (Our FakeTemplateRepository only knows about "template-standard")
        val invalidSession = FakeInspectionRepository.createSession().copy(
            id = InspectionId("non-existent"),
            templateId = TemplateId("missing-template")
        )

        presenter(
            initialSession = invalidSession,
            inspectionRepository = FakeInspectionRepository(invalidSession)
        ).test {
            awaitItem() // Loading
            // First state is Loading because session is found but template is missing in fake repo
            // Wait, our InspectionPresenter returns Loading if template is null.
            // Let's use a session that is NOT_STARTED to trigger a domain error from CompleteInspectionUseCase
            val notStartedSession = FakeInspectionRepository.createSession().copy(
                status = InspectionStatus.NOT_STARTED
            )
            presenter(
                initialSession = notStartedSession,
                inspectionRepository = FakeInspectionRepository(notStartedSession)
            ).test {
                awaitItem() // Loading
                var state = awaitEditing()
                // Answer all required items
                FakeSession.sections.flatMap { it.items }.filter { it.required }.forEach { item ->
                    state.eventSink(InspectionEvent.AnswerChanged(item.id, ChecklistAnswerUi.Compliance(true)))
                    state = awaitItem() as InspectionState.Editing
                }
                state.eventSink(InspectionEvent.ReviewSelected)
                val reviewing = awaitItem() as InspectionState.Reviewing
                reviewing.eventSink(InspectionEvent.CompleteSelected)
                val failed = awaitItem() as InspectionState.Reviewing
                assertTrue("Expected domain error message",
                    failed.saveError?.contains("NOT_STARTED") == true)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `CompleteSelected with critical failure missing evidence transitions to ValidationFailed`() = runTest {
        presenter().test {
            var state = awaitEditing()
            val criticalItemId = "item-fire"
            // Fail a critical item without evidence
            state.eventSink(InspectionEvent.AnswerChanged(criticalItemId, ChecklistAnswerUi.Compliance(false)))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.ReviewSelected)
            val reviewing = awaitItem() as InspectionState.Reviewing
            reviewing.eventSink(InspectionEvent.CompleteSelected)

            val failed = awaitItem() as InspectionState.ValidationFailed
            assertTrue("Expected error for missing evidence on critical failure",
                failed.errors.any { it.message.contains("requires evidence on failure") })
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

    @Test
    fun `SaveDraftSelected persists answers notes evidence and current section for a reopened presenter`() = runTest {
        val inspectionRepository = FakeInspectionRepository.create()

        presenter(inspectionRepository = inspectionRepository).test {
            var state = awaitEditing()
            val firstItem = state.sections.first().items.first()

            state.eventSink(InspectionEvent.AnswerChanged(firstItem.id, ChecklistAnswerUi.Compliance(true)))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.NoteChanged(firstItem.id, "Needs follow-up"))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.EvidenceAdded(firstItem.id, "photo-1"))
            state = awaitItem() as InspectionState.Editing
            state.eventSink(InspectionEvent.NextSection)
            state = awaitItem() as InspectionState.Editing

            state.eventSink(InspectionEvent.SaveDraftSelected)
            advanceUntilIdle()

            val saved = inspectionRepository.savedSession
            assertEquals(SectionId("section-safety"), saved.currentSectionId)
            assertEquals(ChecklistAnswerValue.Pass, saved.answers.single().value)
            assertEquals("Needs follow-up", saved.answers.single().note)
            assertEquals(listOf(EvidenceId("photo-1")), saved.answers.single().evidenceIds)
            cancelAndIgnoreRemainingEvents()
        }

        presenter(inspectionRepository = inspectionRepository).test {
            val restored = awaitEditing()
            val restoredItem = restored.sections.first().items.first()

            assertEquals(1, restored.currentSectionIndex)
            assertEquals(ChecklistAnswerUi.Compliance(true), restoredItem.answer)
            assertEquals("Needs follow-up", restoredItem.note)
            assertEquals(listOf("photo-1"), restoredItem.evidenceRefs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Opening a NOT_STARTED inspection defaults to first section and Editing phase`() = runTest {
        val notStarted = FakeInspectionRepository.createSession().copy(
            status = InspectionStatus.NOT_STARTED,
            currentSectionId = null
        )
        presenter(initialSession = notStarted).test {
            val state = awaitEditing()
            assertEquals(0, state.currentSectionIndex)
            assertEquals("Equipment Condition", state.currentSection?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SaveDraftSelected keeps editing and shows an error when saving fails`() = runTest {
        val inspectionRepository = FakeInspectionRepository.create().apply { failSaves = true }

        presenter(inspectionRepository = inspectionRepository).test {
            val editing = awaitEditing()
            editing.eventSink(InspectionEvent.SaveDraftSelected)

            val failedSave = awaitItem() as InspectionState.Editing
            assertEquals("Couldn't save draft.", failedSave.saveError)
            assertTrue(inspectionRepository.savedSession.answers.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── EvidenceAdded ─────────────────────────────────────────────────────────

    @Test
    fun `EvidenceAdded retains the evidence reference for the item`() = runTest {
        presenter().test {
            val editing = awaitEditing()
            val firstItem = editing.sections.first().items.first()
            assertEquals(0, firstItem.evidenceCount)

            editing.eventSink(InspectionEvent.EvidenceAdded(firstItem.id, "photo-stub"))

            val updated = awaitItem() as InspectionState.Editing
            assertEquals(1, updated.sections.first().items.first().evidenceCount)
            assertEquals(listOf("photo-stub"), updated.sections.first().items.first().evidenceRefs)
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
private class FakeInspectionRepository(
    session: InspectionSession,
) : InspectionRepository {

    private val sessions = MutableStateFlow(session)
    var failSaves: Boolean = false
    val savedSession: InspectionSession
        get() = sessions.value

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSession?> =
        sessions.map { current -> current.takeIf { it.id == inspectionId } }

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> =
        sessions.map { session ->
            listOf(
                InspectionSummary(
                    id = session.id,
                    title = session.assetName,
                    status = session.status,
                    completedItems = session.answers.count { it.value != null },
                    totalItems = 7,
                ),
            )
        }

    override suspend fun getInspection(inspectionId: InspectionId): InspectionSession? =
        sessions.value.takeIf { it.id == inspectionId }

    override suspend fun createInspection(
        assetId: String,
        assetName: String,
        templateId: String,
        startedAtMillis: Long,
    ): InspectionId = sessions.value.id

    override suspend fun saveDraft(session: InspectionSession) {
        if (failSaves) error("Repository unavailable")
        sessions.value = session
    }

    override suspend fun complete(completed: CompletedInspection) {
        if (sessions.value.id == completed.id) {
            sessions.value = sessions.value.copy(
                status = InspectionStatus.COMPLETED,
                answers = completed.answers,
                score = completed.score,
                completedAtMillis = completed.completedAtMillis,
            )
        }
    }

    companion object {
        fun create(): FakeInspectionRepository = FakeInspectionRepository(createSession())

        fun createSession(): InspectionSession = InspectionSession(
            id = InspectionId("computer-lab-i-44"),
            assetId = AssetId("asset-lab-1"),
            assetName = "Computer Lab I.44",
            templateId = TemplateId("template-standard"),
            status = InspectionStatus.IN_PROGRESS,
            answers = emptyList(),
            startedAtMillis = 0L,
            updatedAtMillis = 0L,
        )
    }
}

private class FakeIssueRepository : IssueRepository {
    private val issues = MutableStateFlow<List<com.topic11.cs426.domain.model.MaintenanceIssue>>(emptyList())

    val createdIssues: List<com.topic11.cs426.domain.model.MaintenanceIssue>
        get() = issues.value

    override fun observeIssues(): Flow<List<com.topic11.cs426.domain.model.MaintenanceIssue>> = issues

    override suspend fun createIssue(
        issue: com.topic11.cs426.domain.model.MaintenanceIssue,
    ): com.topic11.cs426.domain.model.IssueId {
        issues.value += issue
        return issue.id
    }

    override suspend fun updateIssue(issue: com.topic11.cs426.domain.model.MaintenanceIssue) {
        issues.value = issues.value.map { current ->
            if (current.id == issue.id) issue else current
        }
    }
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
                    ChecklistItem(id = ChecklistItemId("item-fire"),    sectionId = SectionId("section-safety"), title = "Fire extinguisher is accessible",    required = true, critical = true, answerType = ChecklistAnswerType.PASS_FAIL_NA),
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

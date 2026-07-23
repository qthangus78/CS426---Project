package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Drives the inspection workflow state machine.
 *
 * Phase 2 — constructor now accepts [observeInspection] and [templateRepository] instead of
 * a hardcoded sections list. present() emits [InspectionState.Loading] first, then collects
 * the live session + template pair via flatMapLatest, maps domain types onto the UI model
 * contract, and hydrates the mutable answer/note/evidence maps from the saved session once
 * on first load (draft restore).
 *
 * Phase 3 — replace [SaveDraftSelected] stub with SaveInspectionDraftUseCase once Huy ships
 * it. Replace composition-root DemoRepositories with RoomInspectionRepository /
 * RoomTemplateRepository once Lĩnh ships data layer.
 *
 * Rule: no business logic here. The Presenter only coordinates; [validate] is a
 * presentation-layer completeness check (required fields only), not a domain rule.
 */
internal class InspectionPresenter(
    private val screen: InspectionScreen,
    private val navigator: Navigator,
    private val observeInspection: ObserveInspectionUseCase,
    private val templateRepository: TemplateRepository,
) : Presenter<InspectionState> {

    @Composable
    override fun present(): InspectionState {

        // ── Mutable workflow state ────────────────────────────────────────────
        var sectionIndex by remember { mutableStateOf(0) }
        var phase by remember { mutableStateOf(WorkflowPhase.Editing) }
        var validationErrors by remember { mutableStateOf(emptyList<ValidationError>()) }
        val answers = remember { mutableStateMapOf<String, ChecklistAnswerUi>() }
        val notes = remember { mutableStateMapOf<String, String>() }
        val evidenceCounts = remember { mutableStateMapOf<String, Int>() }

        // Non-state ref: tracks which session ID has already been hydrated.
        // Plain mutableListOf so flipping it does NOT trigger recomposition and
        // avoids emitting a spurious extra state during tests.
        val hydratedIds = remember { mutableListOf<String>() }

        // ── Domain flow: session → flatMapLatest → template ───────────────────
        // Collect as Pair(session, template). Both are null until the first emission
        // arrives, which keeps the Loading state visible while data is fetched.
        @OptIn(ExperimentalCoroutinesApi::class)
        val sessionWithTemplate by remember {
            observeInspection(InspectionId(screen.inspectionId))
                .flatMapLatest { session ->
                    if (session == null) {
                        flowOf(null to null)
                    } else {
                        templateRepository.observeTemplate(session.templateId)
                            .map { template -> session to template }
                    }
                }
        }.collectAsState(initial = null to null)

        val (session, template) = sessionWithTemplate

        // ── Draft restore: hydrate UI maps from saved session once ────────────
        // LaunchedEffect runs after composition as a side effect, so it does NOT
        // trigger an extra recomposition unlike mutating mutableStateOf during
        // composition. The plain-list ref avoids Compose tracking the flag.
        LaunchedEffect(session?.id?.value) {
            val id = session?.id?.value ?: return@LaunchedEffect
            if (id !in hydratedIds) {
                hydratedIds.add(id)
                session.answers.forEach { answer ->
                    val itemId = answer.checklistItemId.value
                    answer.value?.let { value -> answers[itemId] = value.toUi() }
                    answer.note?.takeIf { it.isNotBlank() }?.let { notes[itemId] = it }
                    if (answer.evidenceIds.isNotEmpty()) evidenceCounts[itemId] = answer.evidenceIds.size
                }
            }
        }

        // ── Loading while session or template not yet arrived ─────────────────
        if (session == null || template == null) {
            return InspectionState.Loading(eventSink = {})
        }

        // ── Build section structure from template (sorted by declared order) ──
        val sections = remember(template) {
            template.sections
                .sortedBy { it.order }
                .map { it.toUi() }
        }

        fun resolvedSections(): List<InspectionSectionUi> = sections.map { section ->
            section.copy(
                items = section.items.map { item ->
                    item.copy(
                        answer = answers[item.id] ?: ChecklistAnswerUi.Unanswered,
                        note = notes[item.id] ?: "",
                        evidenceCount = evidenceCounts[item.id] ?: 0,
                    )
                },
            )
        }

        fun progress(resolved: List<InspectionSectionUi>): InspectionProgress {
            val all = resolved.flatMap { it.items }
            return InspectionProgress(
                completedItems = all.count { it.isAnswered },
                totalItems = all.size,
            )
        }

        // Presentation-layer completeness check only. Domain validation rules
        // (critical failures, evidence requirements, etc.) belong to Huy's
        // ValidateInspectionUseCase and will replace this in Phase 3.
        fun validate(resolved: List<InspectionSectionUi>): List<ValidationError> =
            resolved.flatMap { it.items }
                .filter { it.required && !it.isAnswered }
                .map { ValidationError(itemId = it.id, message = "${it.prompt} is required.") }

        // ── Event handler ─────────────────────────────────────────────────────
        val eventSink: (InspectionEvent) -> Unit = remember(navigator) {
            { event ->
                when (event) {
                    // BackSelected is context-aware: exits the screen from Editing/Completed,
                    // but returns to Editing from Reviewing/ValidationFailed.
                    InspectionEvent.BackSelected -> when (phase) {
                        WorkflowPhase.Editing, WorkflowPhase.Completed -> navigator.pop()
                        WorkflowPhase.Reviewing, WorkflowPhase.ValidationFailed ->
                            phase = WorkflowPhase.Editing
                    }

                    is InspectionEvent.AnswerChanged ->
                        answers[event.itemId] = event.answer

                    is InspectionEvent.NoteChanged ->
                        notes[event.itemId] = event.note

                    is InspectionEvent.EvidenceAdded ->
                        evidenceCounts[event.itemId] = (evidenceCounts[event.itemId] ?: 0) + 1

                    InspectionEvent.PreviousSection ->
                        if (sectionIndex > 0) sectionIndex--

                    InspectionEvent.NextSection ->
                        if (sectionIndex < sections.lastIndex) sectionIndex++

                    InspectionEvent.ReviewSelected ->
                        phase = WorkflowPhase.Reviewing

                    // Phase 3: call SaveInspectionDraftUseCase once Huy ships it.
                    InspectionEvent.SaveDraftSelected -> Unit

                    InspectionEvent.CompleteSelected -> {
                        val errors = validate(resolvedSections())
                        if (errors.isEmpty()) {
                            phase = WorkflowPhase.Completed
                        } else {
                            validationErrors = errors
                            phase = WorkflowPhase.ValidationFailed
                        }
                    }
                }
            }
        }

        // ── Build and return current state ────────────────────────────────────
        val resolved = resolvedSections()
        val prog = progress(resolved)
        val title = session.assetName

        return when (phase) {
            WorkflowPhase.Editing -> InspectionState.Editing(
                title = title,
                sections = resolved,
                currentSectionIndex = sectionIndex,
                progress = prog,
                eventSink = eventSink,
            )
            WorkflowPhase.Reviewing -> InspectionState.Reviewing(
                title = title,
                sections = resolved,
                progress = prog,
                eventSink = eventSink,
            )
            WorkflowPhase.ValidationFailed -> InspectionState.ValidationFailed(
                title = title,
                sections = resolved,
                errors = validationErrors,
                eventSink = eventSink,
            )
            WorkflowPhase.Completed -> InspectionState.Completed(
                title = title,
                summary = prog,
                eventSink = eventSink,
            )
        }
    }

    private enum class WorkflowPhase { Editing, Reviewing, ValidationFailed, Completed }
}

// ── Domain → UI mapping ───────────────────────────────────────────────────────

private fun InspectionSection.toUi(): InspectionSectionUi = InspectionSectionUi(
    id = id.value,
    title = title,
    items = items.map { it.toUi() },
)

/** Maps a domain [ChecklistItem] to a UI model with blank answer/note/evidence.
 *  The Presenter overlays live answer state on top of this structure in resolvedSections(). */
private fun ChecklistItem.toUi(): ChecklistItemUi = ChecklistItemUi(
    id = id.value,
    prompt = title,
    required = required,
    answer = ChecklistAnswerUi.Unanswered,
    note = "",
    evidenceCount = 0,
)

/** Maps a saved [ChecklistAnswerValue] back to a UI answer for draft restore.
 *  NotApplicable has no direct UI equivalent; rendered as "N/A" text until the
 *  UI model gains an explicit NA option. */
private fun ChecklistAnswerValue.toUi(): ChecklistAnswerUi = when (this) {
    ChecklistAnswerValue.Pass -> ChecklistAnswerUi.Compliance(isCompliant = true)
    ChecklistAnswerValue.Fail -> ChecklistAnswerUi.Compliance(isCompliant = false)
    ChecklistAnswerValue.NotApplicable -> ChecklistAnswerUi.Text(value = "N/A")
    is ChecklistAnswerValue.YesNo -> ChecklistAnswerUi.Compliance(isCompliant = value)
    is ChecklistAnswerValue.Text -> ChecklistAnswerUi.Text(value = value)
    is ChecklistAnswerValue.NumberValue -> ChecklistAnswerUi.Text(value = value.toString())
    is ChecklistAnswerValue.SingleChoice -> ChecklistAnswerUi.Text(value = optionId)
}

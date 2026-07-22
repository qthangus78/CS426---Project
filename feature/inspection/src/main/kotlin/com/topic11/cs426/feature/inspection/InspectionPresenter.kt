package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.navigation.InspectionScreen

/**
 * Drives the inspection workflow state machine.
 *
 * Phase 1 — all data comes from [FakeSession]. The Presenter owns the mutable workflow
 * state (current section, answers, notes, evidence counts, phase) and transitions it
 * in response to [InspectionEvent]s.
 *
 * Phase 2 — inject ObserveInspectionSessionUseCase, replace [FakeSession.sections]
 * with a domain Flow, and map domain types onto the existing UI model contract. The
 * state machine and event handling below remain unchanged.
 *
 * Rule: no business logic here. The Presenter only coordinates; [validate] is a
 * presentation-layer completeness check (required fields), not a domain rule.
 */
internal class InspectionPresenter(
    private val screen: InspectionScreen,
    private val navigator: Navigator,
) : Presenter<InspectionState> {

    @Composable
    override fun present(): InspectionState {
        val sections = remember { FakeSession.sections }

        var sectionIndex by remember { mutableStateOf(0) }
        var phase by remember { mutableStateOf(WorkflowPhase.Editing) }
        var validationErrors by remember { mutableStateOf(emptyList<ValidationError>()) }

        val answers = remember { mutableStateMapOf<String, ChecklistAnswerUi>() }
        val notes = remember { mutableStateMapOf<String, String>() }
        val evidenceCounts = remember { mutableStateMapOf<String, Int>() }

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

        fun validate(resolved: List<InspectionSectionUi>): List<ValidationError> =
            resolved.flatMap { it.items }
                .filter { it.required && !it.isAnswered }
                .map { ValidationError(itemId = it.id, message = "${it.prompt} is required.") }

        val eventSink: (InspectionEvent) -> Unit = remember {
            { event ->
                when (event) {
                    // BackSelected is context-aware: exits the screen from Editing/Completed,
                    // but returns to Editing from Reviewing/ValidationFailed.
                    InspectionEvent.BackSelected -> when (phase) {
                        WorkflowPhase.Editing, WorkflowPhase.Completed -> navigator.pop()
                        // validationErrors are not cleared here — they're invisible in
                        // Editing state and will be recalculated on the next CompleteSelected.
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

                    // Phase 1: no-op. Phase 3: persist draft to repository.
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

        val resolved = resolvedSections()
        val prog = progress(resolved)

        return when (phase) {
            WorkflowPhase.Editing -> InspectionState.Editing(
                title = screen.inspectionId,
                sections = resolved,
                currentSectionIndex = sectionIndex,
                progress = prog,
                eventSink = eventSink,
            )
            WorkflowPhase.Reviewing -> InspectionState.Reviewing(
                title = screen.inspectionId,
                sections = resolved,
                progress = prog,
                eventSink = eventSink,
            )
            WorkflowPhase.ValidationFailed -> InspectionState.ValidationFailed(
                title = screen.inspectionId,
                sections = resolved,
                errors = validationErrors,
                eventSink = eventSink,
            )
            WorkflowPhase.Completed -> InspectionState.Completed(
                title = screen.inspectionId,
                summary = prog,
                eventSink = eventSink,
            )
        }
    }

    private enum class WorkflowPhase { Editing, Reviewing, ValidationFailed, Completed }
}

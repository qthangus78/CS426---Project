package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

/**
 * Presentation contract for the Inspection screen.
 *
 * State and Event are the settled boundary the Presenter, UI, and tests all read from.
 * The [InspectionState] machine mirrors the inspection workflow that was chốt with the team:
 *
 *     Loading -> Editing -> (Reviewing | ValidationFailed) -> Completed
 *
 * The models below ([InspectionSectionUi], [ChecklistItemUi], [ChecklistAnswerUi],
 * [ValidationError]) are feature-owned presentation models. They intentionally do NOT
 * reference domain types so this slice can run on fake data in Phase 1. When the domain
 * session contract is ready, the Presenter maps domain models onto these UI models — the
 * mapping is the only thing that changes, not this contract.
 */
@Immutable
sealed interface InspectionState : CircuitUiState {
    val eventSink: (InspectionEvent) -> Unit

    /** The session is being loaded; nothing to show yet. */
    data class Loading(
        override val eventSink: (InspectionEvent) -> Unit,
    ) : InspectionState

    /** The inspector is filling in the checklist, one section at a time. */
    data class Editing(
        val title: String,
        val sections: List<InspectionSectionUi>,
        val currentSectionIndex: Int,
        val progress: InspectionProgress,
        override val eventSink: (InspectionEvent) -> Unit,
    ) : InspectionState {
        val currentSection: InspectionSectionUi?
            get() = sections.getOrNull(currentSectionIndex)

        val hasPreviousSection: Boolean
            get() = currentSectionIndex > 0

        val hasNextSection: Boolean
            get() = currentSectionIndex < sections.lastIndex
    }

    /** The inspector reviews every answer before completing the inspection. */
    data class Reviewing(
        val title: String,
        val sections: List<InspectionSectionUi>,
        val progress: InspectionProgress,
        override val eventSink: (InspectionEvent) -> Unit,
    ) : InspectionState

    /** Completion was attempted but required answers are missing or invalid. */
    data class ValidationFailed(
        val title: String,
        val sections: List<InspectionSectionUi>,
        val errors: List<ValidationError>,
        override val eventSink: (InspectionEvent) -> Unit,
    ) : InspectionState

    /** The inspection is finished and can no longer be edited. */
    data class Completed(
        val title: String,
        val summary: InspectionProgress,
        override val eventSink: (InspectionEvent) -> Unit,
    ) : InspectionState
}

sealed interface InspectionEvent : CircuitUiEvent {
    /** Leave the inspection screen. */
    data object BackSelected : InspectionEvent

    /** The answer for a checklist item changed. */
    data class AnswerChanged(
        val itemId: String,
        val answer: ChecklistAnswerUi,
    ) : InspectionEvent

    /** The free-text note for a checklist item changed. */
    data class NoteChanged(
        val itemId: String,
        val note: String,
    ) : InspectionEvent

    /** A piece of evidence was attached to a checklist item. */
    data class EvidenceAdded(
        val itemId: String,
        val evidenceRef: String,
    ) : InspectionEvent

    /** Move to the previous section while editing. */
    data object PreviousSection : InspectionEvent

    /** Move to the next section while editing. */
    data object NextSection : InspectionEvent

    /** Move from editing into the review step. */
    data object ReviewSelected : InspectionEvent

    /** Persist the current answers as a draft without completing. */
    data object SaveDraftSelected : InspectionEvent

    /** Attempt to complete the inspection; may trigger validation. */
    data object CompleteSelected : InspectionEvent
}

/** Completion progress shared across states that show a progress indicator. */
@Immutable
data class InspectionProgress(
    val completedItems: Int,
    val totalItems: Int,
) {
    val fraction: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems.toFloat()
}

/** A checklist section grouping related items. */
@Immutable
data class InspectionSectionUi(
    val id: String,
    val title: String,
    val items: List<ChecklistItemUi>,
)

/** A single checklist item the inspector answers. */
@Immutable
data class ChecklistItemUi(
    val id: String,
    val prompt: String,
    val required: Boolean,
    val answer: ChecklistAnswerUi,
    val note: String,
    val evidenceCount: Int,
) {
    val isAnswered: Boolean
        get() = answer !is ChecklistAnswerUi.Unanswered
}

/** The answer captured for a checklist item. */
@Immutable
sealed interface ChecklistAnswerUi {
    data object Unanswered : ChecklistAnswerUi

    /** Pass / fail style answer. */
    data class Compliance(val isCompliant: Boolean) : ChecklistAnswerUi

    /** Free-text answer. */
    data class Text(val value: String) : ChecklistAnswerUi
}

/** A validation problem surfaced when completion fails. */
@Immutable
data class ValidationError(
    val itemId: String,
    val message: String,
)

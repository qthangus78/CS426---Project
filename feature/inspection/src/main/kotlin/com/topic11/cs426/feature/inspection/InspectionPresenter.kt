package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.CompleteInspectionResult
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionValidationError
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplateUseCase
import com.topic11.cs426.domain.usecase.SaveInspectionDraftUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Coordinates the inspection workflow without owning business rules. */
internal class InspectionPresenter(
    private val screen: InspectionScreen,
    private val navigator: Navigator,
    private val observeInspection: ObserveInspectionUseCase,
    private val observeTemplate: ObserveTemplateUseCase,
    private val saveInspectionDraft: SaveInspectionDraftUseCase,
    private val validateInspection: ValidateInspectionUseCase,
    private val completeInspection: CompleteInspectionUseCase,
) : Presenter<InspectionState> {

    @Composable
    override fun present(): InspectionState {
        // Retained so a configuration change (rotation) does not discard unsaved checklist work.
        var requestedPhase by rememberRetained(screen.inspectionId) { mutableStateOf<WorkflowPhase?>(null) }
        var validationErrors by rememberRetained(screen.inspectionId) { mutableStateOf(emptyList<ValidationError>()) }
        var draftSession by rememberRetained(screen.inspectionId) { mutableStateOf<InspectionSession?>(null) }
        var saveError by rememberRetained(screen.inspectionId) { mutableStateOf<String?>(null) }
        var completionResult by rememberRetained(screen.inspectionId) {
            mutableStateOf<CompleteInspectionResult.Success?>(null)
        }
        val coroutineScope = rememberCoroutineScope()
        // Deliberately not retained: the flush coroutine dies with the composition, so a rotation
        // mid-leave must not come back with back permanently disabled.
        var isLeaving by remember { mutableStateOf(false) }

        @OptIn(ExperimentalCoroutinesApi::class)
        val load by remember(screen.inspectionId) {
            observeInspection(InspectionId(screen.inspectionId))
                .flatMapLatest { session ->
                    if (session == null) {
                        flowOf(SessionLoad.Unavailable)
                    } else {
                        observeTemplate(session.templateId).map { template ->
                            if (template == null) {
                                SessionLoad.Unavailable
                            } else {
                                SessionLoad.Ready(session, template)
                            }
                        }
                    }
                }
        }.collectAsState(initial = SessionLoad.Loading)

        // Flushes any debounced edit before leaving, so back never silently drops work.
        // Guarded because the flush makes the pop asynchronous: a second back arriving while the
        // draft is still being written would pop twice and unwind past the dashboard, which the
        // root navigator turns into finishing the Activity.
        val leaveScreen: () -> Unit = remember(navigator, coroutineScope, saveInspectionDraft) {
            {
                if (!isLeaving) {
                    isLeaving = true
                    val pending = draftSession
                    if (pending == null) {
                        navigator.pop()
                    } else {
                        coroutineScope.launch {
                            try {
                                saveInspectionDraft(pending)
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                // The autosave path already surfaced the error; do not trap the user here.
                            }
                            navigator.pop()
                        }
                    }
                }
                Unit
            }
        }

        val ready = load as? SessionLoad.Ready
        if (ready == null) {
            val eventSink: (InspectionEvent) -> Unit = remember(leaveScreen) {
                { event ->
                    if (event is InspectionEvent.BackSelected) leaveScreen()
                }
            }
            return if (load is SessionLoad.Unavailable) {
                InspectionState.Unavailable(
                    message = "This inspection is no longer available.",
                    eventSink = eventSink,
                )
            } else {
                InspectionState.Loading(eventSink = eventSink)
            }
        }

        val observedSession = ready.session
        val template = ready.template

        // Local edits accumulate in one complete domain session that autosave and the back flush persist.
        val session = draftSession?.takeIf { it.id == observedSession.id } ?: observedSession
        val phase = requestedPhase ?: observedSession.status.toWorkflowPhase()
        val sections = remember(template) {
            template.sections
                .sortedBy { it.order }
                .map { it.toUi() }
        }
        val currentSectionIndex = sections.indexOfFirst {
            it.id == session.currentSectionId?.value
        }.takeIf { it >= 0 } ?: 0

        fun resolvedSections(): List<InspectionSectionUi> {
            val answersByItemId = session.answers.associateBy { it.checklistItemId.value }
            return sections.map { section ->
                section.copy(
                    items = section.items.map { item ->
                        val answer = answersByItemId[item.id]
                        item.copy(
                            answer = answer?.value?.toUi() ?: ChecklistAnswerUi.Unanswered,
                            note = answer?.note.orEmpty(),
                            evidenceRefs = answer?.evidenceIds.orEmpty().map { it.value },
                        )
                    },
                )
            }
        }

        fun progress(resolved: List<InspectionSectionUi>): InspectionProgress {
            val items = resolved.flatMap { it.items }
            return InspectionProgress(
                completedItems = items.count { it.isAnswered },
                totalItems = items.size,
            )
        }

        // Debounced autosave: every edit restarts this effect, so the draft is persisted shortly
        // after the inspector stops typing instead of only when they remember to press Save Draft.
        val pendingDraft = draftSession.takeIf { phase != WorkflowPhase.Completed }
        LaunchedEffect(pendingDraft) {
            if (pendingDraft == null) return@LaunchedEffect
            delay(AUTO_SAVE_DEBOUNCE_MILLIS)
            try {
                saveInspectionDraft(pendingDraft)
                saveError = null
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                saveError = "Couldn't save draft."
            }
        }

        val eventSink: (InspectionEvent) -> Unit = remember(
            navigator,
            session,
            template,
            sections,
            currentSectionIndex,
            phase,
            coroutineScope,
            leaveScreen,
            saveInspectionDraft,
            validateInspection,
            completeInspection,
        ) {
            { event ->
                when (event) {
                    InspectionEvent.BackSelected -> when (phase) {
                        WorkflowPhase.Editing, WorkflowPhase.Completed -> leaveScreen()
                        WorkflowPhase.Reviewing, WorkflowPhase.ValidationFailed -> {
                            requestedPhase = WorkflowPhase.Editing
                        }
                    }

                    is InspectionEvent.AnswerChanged -> {
                        draftSession = session.withUpdatedAnswer(event.itemId) { answer ->
                            answer.copy(value = event.answer.toDomainValue())
                        }
                    }

                    is InspectionEvent.NoteChanged -> {
                        draftSession = session.withUpdatedAnswer(event.itemId) { answer ->
                            answer.copy(note = event.note)
                        }
                    }

                    is InspectionEvent.EvidenceCaptureRequested -> {
                        saveError = null
                    }

                    is InspectionEvent.EvidenceCaptured -> {
                        if (
                            event.reference.inspectionId == session.id &&
                            event.reference.checklistItemId.value == event.itemId
                        ) {
                            draftSession = session.withEvidence(
                                event.itemId,
                                event.reference.id.value,
                            )
                            saveError = null
                        } else {
                            saveError = "Couldn't attach evidence to this item."
                        }
                    }

                    is InspectionEvent.EvidenceCaptureFailed -> {
                        saveError = event.message
                    }

                    InspectionEvent.PreviousSection -> {
                        if (currentSectionIndex > 0) {
                            draftSession = session.withCurrentSection(
                                sections[currentSectionIndex - 1].id,
                            )
                        }
                    }

                    InspectionEvent.NextSection -> {
                        if (currentSectionIndex < sections.lastIndex) {
                            draftSession = session.withCurrentSection(
                                sections[currentSectionIndex + 1].id,
                            )
                        }
                    }

                    InspectionEvent.ReviewSelected -> {
                        requestedPhase = WorkflowPhase.Reviewing
                    }

                    InspectionEvent.SaveDraftSelected -> {
                        coroutineScope.launch {
                            try {
                                saveInspectionDraft(session)
                                saveError = null
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                saveError = "Couldn't save draft."
                            }
                        }
                    }

                    InspectionEvent.CompleteSelected -> {
                        coroutineScope.launch {
                            try {
                                // Validate the same draft that is persisted for the completion use case.
                                saveInspectionDraft(session)
                                saveError = null

                                val validation = validateInspection(session, template)
                                if (!validation.isValid) {
                                    validationErrors = validation.errors.map { error ->
                                        error.toUiValidationError()
                                    }
                                    requestedPhase = WorkflowPhase.ValidationFailed
                                    return@launch
                                }

                                when (val result = completeInspection(session.id)) {
                                    is CompleteInspectionResult.Success -> {
                                        completionResult = result
                                        // Drop the in-progress draft so autosave and the back flush
                                        // can never overwrite the completed row with stale answers.
                                        draftSession = null
                                        requestedPhase = WorkflowPhase.Completed
                                    }
                                    is CompleteInspectionResult.ValidationFailed -> {
                                        validationErrors = result.errors.map { error ->
                                            error.toUiValidationError()
                                        }
                                        requestedPhase = WorkflowPhase.ValidationFailed
                                    }
                                    is CompleteInspectionResult.Error -> {
                                        saveError = result.message
                                    }
                                }
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                saveError = "Couldn't complete inspection."
                            }
                        }
                    }
                }
            }
        }

        val resolved = resolvedSections()
        val inspectionProgress = progress(resolved)

        return when (phase) {
            WorkflowPhase.Editing -> InspectionState.Editing(
                title = session.assetName,
                sections = resolved,
                currentSectionIndex = currentSectionIndex,
                progress = inspectionProgress,
                saveError = saveError,
                eventSink = eventSink,
            )

            WorkflowPhase.Reviewing -> InspectionState.Reviewing(
                title = session.assetName,
                sections = resolved,
                progress = inspectionProgress,
                saveError = saveError,
                eventSink = eventSink,
            )

            WorkflowPhase.ValidationFailed -> InspectionState.ValidationFailed(
                title = session.assetName,
                sections = resolved,
                errors = validationErrors,
                saveError = saveError,
                eventSink = eventSink,
            )

            WorkflowPhase.Completed -> InspectionState.Completed(
                title = session.assetName,
                summary = inspectionProgress,
                score = completionResult?.score?.percent ?: session.score?.percent,
                issueCount = completionResult?.issues?.size ?: 0,
                nextInspectionDueAtMillis = completionResult?.nextInspectionDueAtMillis,
                eventSink = eventSink,
            )
        }
    }

}

/** How long the presenter waits after the last edit before persisting the draft. */
private const val AUTO_SAVE_DEBOUNCE_MILLIS = 750L

/**
 * Distinguishes "still loading" from "cannot be opened", so a missing inspection or template
 * surfaces as [InspectionState.Unavailable] instead of an endless spinner.
 */
private sealed interface SessionLoad {
    data object Loading : SessionLoad

    data object Unavailable : SessionLoad

    data class Ready(
        val session: InspectionSession,
        val template: InspectionTemplate,
    ) : SessionLoad
}

private enum class WorkflowPhase { Editing, Reviewing, ValidationFailed, Completed }

private fun InspectionStatus.toWorkflowPhase(): WorkflowPhase = when (this) {
    InspectionStatus.NOT_STARTED,
    InspectionStatus.IN_PROGRESS,
    -> WorkflowPhase.Editing

    InspectionStatus.REVIEWING -> WorkflowPhase.Reviewing
    InspectionStatus.COMPLETED,
    InspectionStatus.SYNC_PENDING,
    -> WorkflowPhase.Completed
}

private fun InspectionSection.toUi(): InspectionSectionUi = InspectionSectionUi(
    id = id.value,
    title = title,
    items = items.map { it.toUi() },
)

private fun ChecklistItem.toUi(): ChecklistItemUi = ChecklistItemUi(
    id = id.value,
    prompt = title,
    required = required,
    inputType = answerType.toUi(),
    answer = ChecklistAnswerUi.Unanswered,
    note = "",
)

private fun ChecklistAnswerType.toUi(): ChecklistAnswerInputUi = when (this) {
    ChecklistAnswerType.PASS_FAIL_NA -> ChecklistAnswerInputUi.PassFailNotApplicable
    ChecklistAnswerType.YES_NO,
    ChecklistAnswerType.TEXT,
    ChecklistAnswerType.NUMBER,
    ChecklistAnswerType.SINGLE_CHOICE,
    -> ChecklistAnswerInputUi.Text
}

private fun ChecklistAnswerValue.toUi(): ChecklistAnswerUi = when (this) {
    ChecklistAnswerValue.Pass -> ChecklistAnswerUi.Compliance(isCompliant = true)
    ChecklistAnswerValue.Fail -> ChecklistAnswerUi.Compliance(isCompliant = false)
    ChecklistAnswerValue.NotApplicable -> ChecklistAnswerUi.NotApplicable
    is ChecklistAnswerValue.YesNo -> ChecklistAnswerUi.Text(value = value.toString())
    is ChecklistAnswerValue.Text -> ChecklistAnswerUi.Text(value = value)
    is ChecklistAnswerValue.NumberValue -> ChecklistAnswerUi.Text(value = value.toString())
    is ChecklistAnswerValue.SingleChoice -> ChecklistAnswerUi.Text(value = optionId)
}

private fun InspectionValidationError.toUiValidationError(): ValidationError = ValidationError(
    itemId = itemId?.value.orEmpty(),
    message = message,
)

private fun ChecklistAnswerUi.toDomainValue(): ChecklistAnswerValue? = when (this) {
    ChecklistAnswerUi.Unanswered -> null
    is ChecklistAnswerUi.Compliance -> if (isCompliant) {
        ChecklistAnswerValue.Pass
    } else {
        ChecklistAnswerValue.Fail
    }

    ChecklistAnswerUi.NotApplicable -> ChecklistAnswerValue.NotApplicable
    is ChecklistAnswerUi.Text -> ChecklistAnswerValue.Text(value)
}

private fun InspectionSession.withUpdatedAnswer(
    itemId: String,
    transform: (InspectionAnswer) -> InspectionAnswer,
): InspectionSession {
    val now = System.currentTimeMillis()
    val checklistItemId = ChecklistItemId(itemId)
    val currentAnswer = answers.firstOrNull { it.checklistItemId == checklistItemId }
        ?: InspectionAnswer(
            inspectionId = id,
            checklistItemId = checklistItemId,
            updatedAtMillis = now,
        )
    val updatedAnswer = transform(currentAnswer).copy(updatedAtMillis = now)

    return copy(
        answers = answers.filterNot { it.checklistItemId == checklistItemId } + updatedAnswer,
        updatedAtMillis = now,
    )
}

private fun InspectionSession.withEvidence(
    itemId: String,
    evidenceRef: String,
): InspectionSession = withUpdatedAnswer(itemId) { answer ->
    val evidenceId = EvidenceId(evidenceRef)
    answer.copy(evidenceIds = (answer.evidenceIds + evidenceId).distinct())
}

private fun InspectionSession.withCurrentSection(sectionId: String): InspectionSession = copy(
    currentSectionId = SectionId(sectionId),
    updatedAtMillis = System.currentTimeMillis(),
)

package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.topic11.cs426.domain.model.InspectionValidationError
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import com.topic11.cs426.domain.usecase.SaveInspectionDraftUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Coordinates the inspection workflow without owning business rules. */
internal class InspectionPresenter(
    private val screen: InspectionScreen,
    private val navigator: Navigator,
    private val observeInspection: ObserveInspectionUseCase,
    private val templateRepository: TemplateRepository,
    private val saveInspectionDraft: SaveInspectionDraftUseCase,
    private val validateInspection: ValidateInspectionUseCase,
    private val completeInspection: CompleteInspectionUseCase,
) : Presenter<InspectionState> {

    @Composable
    override fun present(): InspectionState {
        var phase by remember { mutableStateOf(WorkflowPhase.Editing) }
        var validationErrors by remember { mutableStateOf(emptyList<ValidationError>()) }
        var draftSession by remember { mutableStateOf<InspectionSession?>(null) }
        var saveError by remember { mutableStateOf<String?>(null) }
        var completionResult by remember { mutableStateOf<CompleteInspectionResult.Success?>(null) }
        val coroutineScope = rememberCoroutineScope()

        @OptIn(ExperimentalCoroutinesApi::class)
        val sessionWithTemplate by remember(screen.inspectionId) {
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

        val (observedSession, template) = sessionWithTemplate
        if (observedSession == null || template == null) {
            return InspectionState.Loading(eventSink = {})
        }

        // Keep local edits in one complete domain session until the draft is saved.
        val session = draftSession?.takeIf { it.id == observedSession.id } ?: observedSession
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

        val eventSink: (InspectionEvent) -> Unit = remember(
            navigator,
            session,
            template,
            sections,
            currentSectionIndex,
            phase,
            coroutineScope,
            saveInspectionDraft,
            validateInspection,
            completeInspection,
        ) {
            { event ->
                when (event) {
                    InspectionEvent.BackSelected -> when (phase) {
                        WorkflowPhase.Editing, WorkflowPhase.Completed -> navigator.pop()
                        WorkflowPhase.Reviewing, WorkflowPhase.ValidationFailed -> {
                            phase = WorkflowPhase.Editing
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

                    is InspectionEvent.EvidenceAdded -> {
                        if (event.evidenceRef.isNotBlank()) {
                            draftSession = session.withEvidence(event.itemId, event.evidenceRef)
                        }
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
                        phase = WorkflowPhase.Reviewing
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
                                    phase = WorkflowPhase.ValidationFailed
                                    return@launch
                                }

                                when (val result = completeInspection(session.id)) {
                                    is CompleteInspectionResult.Success -> {
                                        completionResult = result
                                        phase = WorkflowPhase.Completed
                                    }
                                    is CompleteInspectionResult.ValidationFailed -> {
                                        validationErrors = result.errors.map { error ->
                                            error.toUiValidationError()
                                        }
                                        phase = WorkflowPhase.ValidationFailed
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
                score = completionResult?.score?.percent,
                issueCount = completionResult?.issues?.size ?: 0,
                nextInspectionDueAtMillis = completionResult?.nextInspectionDueAtMillis,
                eventSink = eventSink,
            )
        }
    }

    private enum class WorkflowPhase { Editing, Reviewing, ValidationFailed, Completed }
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

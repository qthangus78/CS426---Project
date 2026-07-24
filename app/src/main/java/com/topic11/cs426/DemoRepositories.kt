package com.topic11.cs426

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
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.IssueRepository
import com.topic11.cs426.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// ── Demo template ─────────────────────────────────────────────────────────────

private val DEMO_TEMPLATE_ID = TemplateId("template-standard")

/**
 * Demo [InspectionTemplate] used by [DemoTemplateRepository].
 *
 * Section/item IDs mirror the hardcoded demo data from Phase 1 so navigation
 * smoke tests and manual testing continue to work without modification.
 *
 * Phase 3: remove once [com.topic11.cs426.data.RoomTemplateRepository] is wired.
 */
internal val demoTemplate = InspectionTemplate(
    id = DEMO_TEMPLATE_ID,
    name = "Standard Field Inspection",
    version = 1,
    sections = listOf(
        InspectionSection(
            id = SectionId("section-equipment"),
            templateId = DEMO_TEMPLATE_ID,
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
            templateId = DEMO_TEMPLATE_ID,
            title = "Safety Checks",
            order = 1,
            items = listOf(
                ChecklistItem(id = ChecklistItemId("item-fire"),    sectionId = SectionId("section-safety"), title = "Fire extinguisher is accessible",  required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
                ChecklistItem(id = ChecklistItemId("item-exit"),    sectionId = SectionId("section-safety"), title = "Emergency exit is unobstructed",   required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
            ),
        ),
        InspectionSection(
            id = SectionId("section-environment"),
            templateId = DEMO_TEMPLATE_ID,
            title = "Environment",
            order = 2,
            items = listOf(
                ChecklistItem(id = ChecklistItemId("item-lighting"), sectionId = SectionId("section-environment"), title = "Lighting is adequate",                       required = false, answerType = ChecklistAnswerType.PASS_FAIL_NA),
                ChecklistItem(id = ChecklistItemId("item-temp"),     sectionId = SectionId("section-environment"), title = "Temperature is within operating range", required = true,  answerType = ChecklistAnswerType.PASS_FAIL_NA),
            ),
        ),
    ),
)

// ── Demo sessions ─────────────────────────────────────────────────────────────

private val demoSessions = listOf(
    InspectionSession(
        id = InspectionId("computer-lab-i-44"),
        assetId = AssetId("asset-lab-1"),
        assetName = "Computer Lab I.44",
        templateId = DEMO_TEMPLATE_ID,
        status = InspectionStatus.IN_PROGRESS,
        answers = emptyList(),
        startedAtMillis = 0L,
        updatedAtMillis = 0L,
    ),
    InspectionSession(
        id = InspectionId("projector-p-204"),
        assetId = AssetId("asset-proj-1"),
        assetName = "Projector P-204",
        templateId = DEMO_TEMPLATE_ID,
        status = InspectionStatus.NOT_STARTED,
        answers = emptyList(),
        startedAtMillis = 0L,
        updatedAtMillis = 0L,
    ),
    InspectionSession(
        id = InspectionId("laboratory-a2-safety-check"),
        assetId = AssetId("asset-lab-2"),
        assetName = "Laboratory A2 Safety Check",
        templateId = DEMO_TEMPLATE_ID,
        status = InspectionStatus.SYNC_PENDING,
        answers = emptyList(),
        startedAtMillis = 0L,
        updatedAtMillis = 0L,
    ),
)

// ── DemoInspectionRepository ──────────────────────────────────────────────────

/**
 * Provides demo [InspectionSession] and [InspectionSummary] data for runtime
 * use while the real Room implementation (Lĩnh's `:data` module) is not yet wired.
 *
 * Phase 3: replace with [com.topic11.cs426.data.RoomInspectionRepository].
 */
internal class DemoInspectionRepository : InspectionRepository {

    private val sessions = MutableStateFlow(demoSessions)

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> =
        sessions.map { list ->
            list.map { session ->
                InspectionSummary(
                    id = session.id,
                    title = session.assetName,
                    status = session.status,
                    completedItems = session.answers.count { it.value != null },
                    totalItems = demoTemplate.sections.sumOf { it.items.size },
                )
            }
        }

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSession?> =
        sessions
            .map { list -> list.firstOrNull { it.id == inspectionId } }
            .distinctUntilChanged()

    override suspend fun getInspection(inspectionId: InspectionId): InspectionSession? =
        sessions.value.firstOrNull { it.id == inspectionId }

    override suspend fun createInspection(
        assetId: String,
        assetName: String,
        templateId: String,
        startedAtMillis: Long,
    ): InspectionId = InspectionId(assetId)

    override suspend fun saveDraft(session: InspectionSession) {
        sessions.value = sessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun complete(completed: CompletedInspection) {
        sessions.update { list ->
            list.map { session ->
                if (session.id == completed.id) {
                    session.copy(
                        status = InspectionStatus.COMPLETED,
                        answers = completed.answers,
                        score = completed.score,
                        completedAtMillis = completed.completedAtMillis,
                    )
                } else {
                    session
                }
            }
        }
    }
}

// ── DemoIssueRepository ───────────────────────────────────────────────────────

/**
 * Demo [IssueRepository] used while the real Room implementation is not yet wired.
 */
internal class DemoIssueRepository : IssueRepository {
    private val issues = MutableStateFlow<List<MaintenanceIssue>>(emptyList())

    override fun observeIssues(): Flow<List<MaintenanceIssue>> = issues.asStateFlow()

    override suspend fun createIssue(issue: MaintenanceIssue): IssueId {
        issues.update { it + issue }
        return issue.id
    }

    override suspend fun updateIssue(issue: MaintenanceIssue) {
        issues.update { list ->
            list.map { if (it.id == issue.id) issue else it }
        }
    }
}

// ── DemoTemplateRepository ────────────────────────────────────────────────────

/**
 * Provides [demoTemplate] for runtime use while Lĩnh's Room implementation is
 * not yet wired.
 *
 * Phase 3: replace with [com.topic11.cs426.data.RoomTemplateRepository].
 */
internal class DemoTemplateRepository : TemplateRepository {
    override fun observeTemplates(): Flow<List<InspectionTemplateSummary>> = flowOf(emptyList())

    override fun observeTemplate(id: TemplateId): Flow<InspectionTemplate?> =
        flowOf(if (id == demoTemplate.id) demoTemplate else null)

    override suspend fun getTemplate(id: TemplateId): InspectionTemplate? =
        if (id == demoTemplate.id) demoTemplate else null
}

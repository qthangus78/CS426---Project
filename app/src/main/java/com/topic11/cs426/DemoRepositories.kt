package com.topic11.cs426

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
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
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.AssetRepository
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

private val demoAssets = listOf(
    Asset(
        id = AssetId("asset-lab-1"),
        name = "Computer Lab I.44",
        code = "LAB-I44",
        locationId = LocationId("hcmus"),
        recurrencePolicyDays = 90,
    ),
    Asset(
        id = AssetId("asset-proj-1"),
        name = "Projector P-204",
        code = "PROJ-P204",
        locationId = LocationId("hcmus"),
        recurrencePolicyDays = 120,
    ),
    Asset(
        id = AssetId("asset-lab-2"),
        name = "Laboratory A2 Safety Check",
        code = "LAB-A2",
        locationId = LocationId("hcmus"),
        recurrencePolicyDays = 60,
    ),
)

internal class DemoAssetRepository : AssetRepository {
    private val assets = MutableStateFlow(demoAssets)

    override fun observeAssets(): Flow<List<AssetSummary>> =
        assets.map { values ->
            values.map { asset ->
                AssetSummary(
                    id = asset.id,
                    name = asset.name,
                    code = asset.code,
                    locationName = "HCMUS",
                    nextInspectionDueAtMillis = asset.nextInspectionDueAtMillis,
                )
            }
        }

    override suspend fun getAsset(id: AssetId): Asset? =
        assets.value.firstOrNull { it.id == id }

    override suspend fun saveAsset(asset: Asset) {
        check(assets.value.any { it.id == asset.id }) {
            "Asset does not exist: ${asset.id.value}"
        }
        assets.update { values ->
            values.map { current -> if (current.id == asset.id) asset else current }
        }
    }
}

private val DEMO_TEMPLATE_ID = TemplateId("template-standard")

/**
 * Demo [InspectionTemplate] used by [DemoTemplateRepository] and local smoke fixtures.
 */
internal val demoTemplate = InspectionTemplate(
    id = DEMO_TEMPLATE_ID,
    name = "Standard Field Inspection",
    version = 1,
    recurrencePolicyDays = 365,
    sections = listOf(
        InspectionSection(
            id = SectionId("section-equipment"),
            templateId = DEMO_TEMPLATE_ID,
            title = "Equipment Condition",
            order = 0,
            items = listOf(
                ChecklistItem(
                    id = ChecklistItemId("item-power"),
                    sectionId = SectionId("section-equipment"),
                    title = "Power supply is operational",
                    required = true,
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
                ChecklistItem(
                    id = ChecklistItemId("item-cables"),
                    sectionId = SectionId("section-equipment"),
                    title = "Cables and connectors are undamaged",
                    required = true,
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
                ChecklistItem(
                    id = ChecklistItemId("item-label"),
                    sectionId = SectionId("section-equipment"),
                    title = "Asset label is visible",
                    required = false,
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
            ),
        ),
        InspectionSection(
            id = SectionId("section-safety"),
            templateId = DEMO_TEMPLATE_ID,
            title = "Safety Checks",
            order = 1,
            items = listOf(
                ChecklistItem(
                    id = ChecklistItemId("item-fire"),
                    sectionId = SectionId("section-safety"),
                    title = "Fire extinguisher is accessible",
                    required = true,
                    critical = true,
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
                ChecklistItem(
                    id = ChecklistItemId("item-exit"),
                    sectionId = SectionId("section-safety"),
                    title = "Emergency exit is unobstructed",
                    required = true,
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
            ),
        ),
        InspectionSection(
            id = SectionId("section-environment"),
            templateId = DEMO_TEMPLATE_ID,
            title = "Environment",
            order = 2,
            items = listOf(
                ChecklistItem(
                    id = ChecklistItemId("item-lighting"),
                    sectionId = SectionId("section-environment"),
                    title = "Lighting is adequate",
                    required = false,
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
                ChecklistItem(
                    id = ChecklistItemId("item-temp"),
                    sectionId = SectionId("section-environment"),
                    title = "Temperature is within operating range",
                    required = true,
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
            ),
        ),
    ),
)

private val demoSessions = listOf(
    InspectionSession(
        id = InspectionId("computer-lab-i-44"),
        assetId = AssetId("asset-lab-1"),
        assetName = "Computer Lab I.44",
        templateId = DEMO_TEMPLATE_ID,
        templateName = demoTemplate.name,
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
        templateName = demoTemplate.name,
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
        templateName = demoTemplate.name,
        status = InspectionStatus.SYNC_PENDING,
        answers = emptyList(),
        startedAtMillis = 0L,
        updatedAtMillis = 0L,
    ),
)

internal class DemoInspectionRepository : InspectionRepository {
    private val sessions = MutableStateFlow(demoSessions)

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> =
        sessions.map { values ->
            values.map { session ->
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
            .map { values -> values.firstOrNull { it.id == inspectionId } }
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
        sessions.update { values ->
            values.map { current -> if (current.id == session.id) session else current }
        }
    }

    override suspend fun complete(completed: CompletedInspection) {
        sessions.update { values ->
            values.map { session ->
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

internal class DemoIssueRepository : IssueRepository {
    private val issues = MutableStateFlow<List<MaintenanceIssue>>(emptyList())

    override fun observeIssues(): Flow<List<MaintenanceIssue>> = issues.asStateFlow()

    override suspend fun createIssue(issue: MaintenanceIssue): IssueId {
        issues.update { values -> values + issue }
        return issue.id
    }

    override suspend fun updateIssue(issue: MaintenanceIssue) {
        issues.update { values ->
            values.map { current -> if (current.id == issue.id) issue else current }
        }
    }
}

/**
 * Provides [demoTemplate] for demo-only callers.
 */
internal class DemoTemplateRepository : TemplateRepository {
    override fun observeTemplates(): Flow<List<InspectionTemplateSummary>> = flowOf(emptyList())

    override fun observeTemplate(id: TemplateId): Flow<InspectionTemplate?> =
        flowOf(if (id == demoTemplate.id) demoTemplate else null)

    override suspend fun getTemplate(id: TemplateId): InspectionTemplate? =
        if (id == demoTemplate.id) demoTemplate else null
}

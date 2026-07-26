package com.topic11.cs426.domain

import com.topic11.cs426.core.testing.FakeAssetRepository
import com.topic11.cs426.core.testing.FakeIssueRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.CompleteInspectionResult
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionValidationError
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.model.ReportGenerationError
import com.topic11.cs426.domain.model.ReportGenerationResult
import com.topic11.cs426.domain.usecase.CalculateInspectionScoreUseCase
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.CreateMaintenanceIssueUseCase
import com.topic11.cs426.domain.usecase.CriticalFailure
import com.topic11.cs426.domain.usecase.GenerateInspectionReportUseCase
import com.topic11.cs426.domain.usecase.IssueLifecycle
import com.topic11.cs426.domain.usecase.IssueStatusUpdateResult
import com.topic11.cs426.domain.usecase.ScheduleNextInspectionUseCase
import com.topic11.cs426.domain.usecase.UpdateIssueStatusUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainBusinessRulesTest {

    private val fixtures = InspectionTestFixtures

    @Test
    fun `critical issue IDs remain stable when completion is retried`() = runTest {
        val failure = CriticalFailure(
            inspectionId = fixtures.computerLab.id,
            assetId = fixtures.asset1Id,
            checklistItemId = fixtures.itemCriticalId,
            title = "Critical failure",
        )
        val useCase = CreateMaintenanceIssueUseCase()

        val firstAttempt = useCase(listOf(failure), createdAtMillis = 1_000L)
        val retry = useCase(listOf(failure), createdAtMillis = 2_000L)

        assertEquals(firstAttempt.single().id, retry.single().id)
        assertEquals(1_000L, firstAttempt.single().createdAtMillis)
        assertEquals(2_000L, retry.single().createdAtMillis)
    }

    @Test
    fun `cannotCompleteWithUnansweredRequiredItem`() = runTest {
        val answer = fixtures.createAnswer(
            itemId = fixtures.itemCriticalId,
            value = ChecklistAnswerValue.Pass,
        )
        val session = fixtures.createSampleSession(
            answers = listOf(answer),
            status = InspectionStatus.IN_PROGRESS,
        )

        val result = ValidateInspectionUseCase()(session, fixtures.sampleTemplate)

        assertEquals(false, result.isValid)
        val hasRequiredError = result.errors.any {
            it is InspectionValidationError.RequiredItemUnanswered &&
                it.itemId == fixtures.itemRequiredId
        }
        assertTrue("Expected RequiredItemUnanswered for itemRequiredId", hasRequiredError)
    }

    @Test
    fun `criticalFailureRequiresEvidence`() = runTest {
        val answer = fixtures.createAnswer(
            itemId = fixtures.itemRequiredId,
            value = ChecklistAnswerValue.Fail,
            evidenceIds = emptyList(),
        )
        val session = fixtures.createSampleSession(
            answers = listOf(answer),
            status = InspectionStatus.IN_PROGRESS,
        )

        val result = ValidateInspectionUseCase()(session, fixtures.sampleTemplate)

        assertEquals(false, result.isValid)
        val hasEvidenceError = result.errors.any {
            it is InspectionValidationError.CriticalFailureNeedsEvidence &&
                it.itemId == fixtures.itemRequiredId
        }
        assertTrue("Expected CriticalFailureNeedsEvidence", hasEvidenceError)
    }

    @Test
    fun `criticalFailureCreatesIssue`() = runTest {
        val failedAnswer = fixtures.createAnswer(
            itemId = fixtures.itemRequiredId,
            value = ChecklistAnswerValue.Fail,
            evidenceIds = listOf(fixtures.evidenceId),
        )
        val passingAnswer = fixtures.createAnswer(
            itemId = fixtures.itemCriticalId,
            value = ChecklistAnswerValue.Pass,
        )
        val session = fixtures.createSampleSession(
            answers = listOf(failedAnswer, passingAnswer),
            status = InspectionStatus.REVIEWING,
        )
        val inspectionRepo = RecordingInspectionRepository().apply { addSession(session) }
        val assetRepo = FakeAssetRepository().apply { addAsset(fixtures.sampleAsset) }
        val templateRepo = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        )

        val result = completeUseCase(
            inspectionRepo = inspectionRepo,
            templateRepo = templateRepo,
            assetRepo = assetRepo,
        )(session.id)

        assertTrue("Expected Success", result is CompleteInspectionResult.Success)
        val success = result as CompleteInspectionResult.Success
        assertEquals(1, success.issues.size)
        assertEquals("Critical failure: Fire extinguisher present", success.issues[0].title)
    }

    @Test
    fun `notApplicableIsExcludedFromScore`() = runTest {
        val answer1 = fixtures.createAnswer(
            itemId = fixtures.itemRequiredId,
            value = ChecklistAnswerValue.NotApplicable,
        )
        val answer2 = fixtures.createAnswer(
            itemId = fixtures.itemCriticalId,
            value = ChecklistAnswerValue.Pass,
        )
        val answer3 = fixtures.createAnswer(
            itemId = fixtures.itemOptionalId,
            value = ChecklistAnswerValue.Pass,
        )
        val session = fixtures.createSampleSession(
            answers = listOf(answer1, answer2, answer3),
        )

        val score = CalculateInspectionScoreUseCase()(session, fixtures.sampleTemplate)

        assertEquals(6, score.earnedWeight)
        assertEquals(6, score.totalWeight)
    }

    @Test
    fun `invalidLifecycleTransitionIsRejected`() = runTest {
        val session = fixtures.createSampleSession(
            status = InspectionStatus.NOT_STARTED,
        )
        val inspectionRepo = RecordingInspectionRepository().apply { addSession(session) }
        val assetRepo = FakeAssetRepository().apply { addAsset(fixtures.sampleAsset) }
        val templateRepo = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        )

        val result = completeUseCase(
            inspectionRepo = inspectionRepo,
            templateRepo = templateRepo,
            assetRepo = assetRepo,
        )(session.id)

        assertTrue("Expected ValidationFailed", result is CompleteInspectionResult.ValidationFailed)
        val validationFailed = result as CompleteInspectionResult.ValidationFailed
        assertTrue(validationFailed.errors.any { it is InspectionValidationError.InvalidLifecycleTransition })
    }

    @Test
    fun `reportRequiresCompletedInspection`() = runTest {
        val answer = fixtures.createAnswer(
            itemId = fixtures.itemCriticalId,
            value = ChecklistAnswerValue.Pass,
        )
        val session = fixtures.createSampleSession(
            answers = listOf(answer),
            status = InspectionStatus.REVIEWING,
        )
        val inspectionRepo = RecordingInspectionRepository().apply { addSession(session) }
        val assetRepo = FakeAssetRepository().apply { addAsset(fixtures.sampleAsset) }
        val templateRepo = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        )

        val result = completeUseCase(
            inspectionRepo = inspectionRepo,
            templateRepo = templateRepo,
            assetRepo = assetRepo,
        )(session.id)

        assertTrue("Expected ValidationFailed", result is CompleteInspectionResult.ValidationFailed)
    }

    @Test
    fun `nextInspectionDateUsesRecurrencePolicy`() = runTest {
        val nextDue = ScheduleNextInspectionUseCase()(
            fixtures.sampleTemplate,
            fixtures.sampleAsset,
            completedAtMillis = 1_000L,
        )

        assertEquals(1_000L + 365 * 86_400_000L, nextDue)
    }

    @Test
    fun `nextInspectionDateFallsBackToAssetPolicyWhenTemplateHasNoPolicy`() = runTest {
        val nextDue = ScheduleNextInspectionUseCase()(
            fixtures.templateWithNoRecurrence,
            fixtures.sampleAssetWithPolicy,
            completedAtMillis = 1_000L,
        )

        assertEquals(1_000L + 180 * 86_400_000L, nextDue)
    }

    @Test
    fun `nextInspectionDateIsNullWhenNoPolicyOnTemplateOrAsset`() = runTest {
        val nextDue = ScheduleNextInspectionUseCase()(
            fixtures.templateWithNoRecurrence,
            fixtures.sampleAsset,
            completedAtMillis = 1_000L,
        )

        assertEquals(null, nextDue)
    }

    private fun completeUseCase(
        inspectionRepo: RecordingInspectionRepository,
        templateRepo: FakeTemplateRepository,
        assetRepo: FakeAssetRepository,
    ) = CompleteInspectionUseCase(
        inspectionRepository = inspectionRepo,
        templateRepository = templateRepo,
        assetRepository = assetRepo,
        validateInspection = ValidateInspectionUseCase(),
        calculateScore = CalculateInspectionScoreUseCase(),
        createIssue = CreateMaintenanceIssueUseCase(),
        scheduleNext = ScheduleNextInspectionUseCase(),
    )

    @Test
    fun `issue lifecycle exposes only forward transitions`() {
        assertEquals(
            listOf(MaintenanceIssueStatus.IN_PROGRESS),
            IssueLifecycle.allowedNextStatuses(MaintenanceIssueStatus.OPEN),
        )
        assertEquals(
            listOf(MaintenanceIssueStatus.RESOLVED),
            IssueLifecycle.allowedNextStatuses(MaintenanceIssueStatus.IN_PROGRESS),
        )
        assertEquals(
            listOf(MaintenanceIssueStatus.CLOSED),
            IssueLifecycle.allowedNextStatuses(MaintenanceIssueStatus.RESOLVED),
        )
        assertEquals(
            emptyList<MaintenanceIssueStatus>(),
            IssueLifecycle.allowedNextStatuses(MaintenanceIssueStatus.CLOSED),
        )
    }

    @Test
    fun `same issue status update is rejected`() = runTest {
        val repository = FakeIssueRepository()
        val issue = MaintenanceIssue(
            id = IssueId("issue-status"),
            inspectionId = fixtures.computerLab.id,
            assetId = fixtures.asset1Id,
            severity = IssueSeverity.CRITICAL,
            title = "Critical issue",
            status = MaintenanceIssueStatus.OPEN,
            createdAtMillis = 1_000L,
        )
        repository.addIssue(issue)
        val useCase = UpdateIssueStatusUseCase(repository, clock = { 2_000L })

        val result = useCase(issue.id, MaintenanceIssueStatus.OPEN)

        assertTrue(result is IssueStatusUpdateResult.InvalidTransition)
        assertEquals(MaintenanceIssueStatus.OPEN, repository.getIssue(issue.id)?.status)
    }

    @Test
    fun `valid issue status update persists updated timestamp`() = runTest {
        val repository = FakeIssueRepository()
        val issue = MaintenanceIssue(
            id = IssueId("issue-status"),
            inspectionId = fixtures.computerLab.id,
            assetId = fixtures.asset1Id,
            severity = IssueSeverity.CRITICAL,
            title = "Critical issue",
            status = MaintenanceIssueStatus.OPEN,
            createdAtMillis = 1_000L,
        )
        repository.addIssue(issue)
        val useCase = UpdateIssueStatusUseCase(repository, clock = { 2_000L })

        useCase(issue.id, MaintenanceIssueStatus.IN_PROGRESS)

        val updated = requireNotNull(repository.getIssue(issue.id))
        assertEquals(MaintenanceIssueStatus.IN_PROGRESS, updated.status)
        assertEquals(2_000L, updated.updatedAtMillis)
    }

    @Test
    fun `generateReportRequiresCompletedInspection`() = runTest {
        val session = fixtures.createSampleSession(status = InspectionStatus.IN_PROGRESS)
        val inspectionRepo = RecordingInspectionRepository()
        inspectionRepo.addSession(session)
        val reportUseCase = reportUseCase(
            inspectionRepo = inspectionRepo,
            issueRepo = FakeIssueRepository(),
        )

        val result = reportUseCase(session.id)

        assertEquals(
            ReportGenerationResult.Failed(ReportGenerationError.NotEligible(InspectionStatus.IN_PROGRESS)),
            result,
        )
    }

    @Test
    fun `generateReportUsesCompletedInspectionScoreAndIssues`() = runTest {
        val session = fixtures.createSampleSession(
            status = InspectionStatus.COMPLETED,
        ).copy(score = InspectionScore(earnedWeight = 6, totalWeight = 10))
        val inspectionRepo = RecordingInspectionRepository()
        inspectionRepo.addSession(session)
        val issueRepo = FakeIssueRepository().apply {
            addIssue(
                MaintenanceIssue(
                    id = IssueId("issue-report"),
                    inspectionId = session.id,
                    assetId = session.assetId,
                    severity = IssueSeverity.CRITICAL,
                    title = "Critical report issue",
                    status = MaintenanceIssueStatus.OPEN,
                    createdAtMillis = 1_500L,
                ),
            )
        }
        val reportUseCase = reportUseCase(
            inspectionRepo = inspectionRepo,
            issueRepo = issueRepo,
        )

        val result = reportUseCase(session.id)

        assertTrue(result is ReportGenerationResult.Success)
        val report = (result as ReportGenerationResult.Success).report
        assertEquals(session.id, report.inspectionId)
        assertEquals("Inspection report for Computer Lab I.44", report.summary)
        assertEquals(InspectionScore(earnedWeight = 6, totalWeight = 10), report.score)
        assertEquals("Computer Lab I.44", report.assetName)
        assertEquals("Lab Safety Checklist", report.templateName)
        assertEquals(listOf("Critical report issue"), report.issues.map { it.title })
        assertEquals(2, report.sections.size)
        assertNotNull(report.id.value)
    }

    private fun reportUseCase(
        inspectionRepo: RecordingInspectionRepository,
        issueRepo: FakeIssueRepository,
    ) = GenerateInspectionReportUseCase(
        inspectionRepository = inspectionRepo,
        templateRepository = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        ),
        issueRepository = issueRepo,
        clock = { 3_000L },
    )
}

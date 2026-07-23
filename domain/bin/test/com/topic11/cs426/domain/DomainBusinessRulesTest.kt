package com.topic11.cs426.domain

import com.topic11.cs426.core.testing.FakeIssueRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.CompleteInspectionResult
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionValidationError
import com.topic11.cs426.domain.usecase.CalculateInspectionScoreUseCase
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.CreateMaintenanceIssueUseCase
import com.topic11.cs426.domain.usecase.CriticalFailure
import com.topic11.cs426.domain.usecase.ScheduleNextInspectionUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainBusinessRulesTest {

    private val fixtures = InspectionTestFixtures

    // ─────────────────────────────────────────────
    // TEST 1: cannotCompleteWithUnansweredRequiredItem
    // RULE 1: Cannot complete if required item is unanswered
    // ─────────────────────────────────────────────
    @Test
    fun `cannotCompleteWithUnansweredRequiredItem`() = runTest {
        // Arrange: session has answer for critical item but NOT for the required item
        val answer = fixtures.createAnswer(
            itemId = fixtures.itemCriticalId,
            value = ChecklistAnswerValue.Pass,
        )
        val session = fixtures.createSampleSession(
            answers = listOf(answer), // itemRequiredId is missing
            status = InspectionStatus.IN_PROGRESS,
        )
        val template = fixtures.sampleTemplate
        val validateUseCase = ValidateInspectionUseCase()

        // Act
        val result = validateUseCase(session, template)

        // Assert
        assertEquals(false, result.isValid)
        val hasRequiredError = result.errors.any {
            it is InspectionValidationError.RequiredItemUnanswered &&
                it.itemId == fixtures.itemRequiredId
        }
        assertTrue("Expected RequiredItemUnanswered for itemRequiredId", hasRequiredError)
    }

    // ─────────────────────────────────────────────
    // TEST 2: criticalFailureRequiresEvidence
    // RULE 2: Critical + Fail must have at least one evidence
    // ─────────────────────────────────────────────
    @Test
    fun `criticalFailureRequiresEvidence`() = runTest {
        // Arrange: itemRequiredId is both critical and required, answered Fail but NO evidence
        val answer = fixtures.createAnswer(
            itemId = fixtures.itemRequiredId,
            value = ChecklistAnswerValue.Fail,
            evidenceIds = emptyList(), // No evidence attached
        )
        val session = fixtures.createSampleSession(
            answers = listOf(answer),
            status = InspectionStatus.IN_PROGRESS,
        )
        val template = fixtures.sampleTemplate
        val validateUseCase = ValidateInspectionUseCase()

        // Act
        val result = validateUseCase(session, template)

        // Assert
        assertEquals(false, result.isValid)
        val hasEvidenceError = result.errors.any {
            it is InspectionValidationError.CriticalFailureNeedsEvidence &&
                it.itemId == fixtures.itemRequiredId
        }
        assertTrue("Expected CriticalFailureNeedsEvidence", hasEvidenceError)
    }

    // ─────────────────────────────────────────────
    // TEST 3: criticalFailureCreatesIssue
    // RULE 3: Critical + Fail must create a MaintenanceIssue
    // ─────────────────────────────────────────────
    @Test
    fun `criticalFailureCreatesIssue`() = runTest {
        // Arrange: critical+fail with evidence → complete should create an issue
        val answer = fixtures.createAnswer(
            itemId = fixtures.itemRequiredId,
            value = ChecklistAnswerValue.Fail,
            evidenceIds = listOf(fixtures.evidenceId), // Has evidence
        )
        val answer2 = fixtures.createAnswer(
            itemId = fixtures.itemCriticalId,
            value = ChecklistAnswerValue.Pass,
        )
        val session = fixtures.createSampleSession(
            answers = listOf(answer, answer2),
            status = InspectionStatus.REVIEWING,
        )
        val inspectionRepo = RecordingInspectionRepository()
        inspectionRepo.addSession(session)
        val templateRepo = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        )
        val issueRepo = FakeIssueRepository()
        val validateUseCase = ValidateInspectionUseCase()
        val scoreUseCase = CalculateInspectionScoreUseCase()
        val createIssueUseCase = CreateMaintenanceIssueUseCase(issueRepo)
        val scheduleUseCase = ScheduleNextInspectionUseCase()

        val completeUseCase = CompleteInspectionUseCase(
            inspectionRepository = inspectionRepo,
            templateRepository = templateRepo,
            issueRepository = issueRepo,
            validateInspection = validateUseCase,
            calculateScore = scoreUseCase,
            createIssue = createIssueUseCase,
            scheduleNext = scheduleUseCase,
        )

        // Act
        val result = completeUseCase(session.id)

        // Assert
        assertTrue("Expected Success", result is CompleteInspectionResult.Success)
        val success = result as CompleteInspectionResult.Success
        assertEquals(1, success.issues.size)
        assertEquals("Critical failure: Fire extinguisher present", success.issues[0].title)
    }

    // ─────────────────────────────────────────────
    // TEST 4: notApplicableIsExcludedFromScore
    // RULE 5+6: NotApplicable excluded from total, Pass earns weight
    // ─────────────────────────────────────────────
    @Test
    fun `notApplicableIsExcludedFromScore`() = runTest {
        // Arrange:
        // - item1 (weight 5, required+critical): NotApplicable → skipped entirely
        // - item2 (weight 5, required+critical): Pass → 5/5
        // - item3 (weight 1, optional): Pass → 1/1
        // Expected: earned=6, total=6 (item1 excluded from denominator)
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
        val template = fixtures.sampleTemplate
        val scoreUseCase = CalculateInspectionScoreUseCase()

        // Act
        val score = scoreUseCase(session, template)

        // Assert
        assertEquals(6, score.earnedWeight)  // item2(5) + item3(1)
        assertEquals(6, score.totalWeight)   // item2(5) + item3(1), item1 excluded
    }

    // ─────────────────────────────────────────────
    // TEST 5: invalidLifecycleTransitionIsRejected
    // RULE 4: Only REVIEWING/IN_PROGRESS can be completed
    // ─────────────────────────────────────────────
    @Test
    fun `invalidLifecycleTransitionIsRejected`() = runTest {
        // Arrange: session is NOT_STARTED
        val session = fixtures.createSampleSession(
            status = InspectionStatus.NOT_STARTED,
        )
        val inspectionRepo = RecordingInspectionRepository()
        inspectionRepo.addSession(session)
        val templateRepo = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        )
        val issueRepo = FakeIssueRepository()
        val validateUseCase = ValidateInspectionUseCase()
        val scoreUseCase = CalculateInspectionScoreUseCase()
        val createIssueUseCase = CreateMaintenanceIssueUseCase(issueRepo)
        val scheduleUseCase = ScheduleNextInspectionUseCase()

        val completeUseCase = CompleteInspectionUseCase(
            inspectionRepository = inspectionRepo,
            templateRepository = templateRepo,
            issueRepository = issueRepo,
            validateInspection = validateUseCase,
            calculateScore = scoreUseCase,
            createIssue = createIssueUseCase,
            scheduleNext = scheduleUseCase,
        )

        // Act
        val result = completeUseCase(session.id)

        // Assert
        assertTrue("Expected Error", result is CompleteInspectionResult.Error)
        val error = result as CompleteInspectionResult.Error
        assertTrue(error.message.contains("NOT_STARTED"))
    }

    // ─────────────────────────────────────────────
    // TEST 6: reportRequiresCompletedInspection
    // RULE 7: Report can only be generated after validation + complete
    // (Tests that CompleteInspectionUseCase validates before completing)
    // ─────────────────────────────────────────────
    @Test
    fun `reportRequiresCompletedInspection`() = runTest {
        // Arrange: session has required unanswered → validation will fail
        val answer = fixtures.createAnswer(
            itemId = fixtures.itemCriticalId,
            value = ChecklistAnswerValue.Pass,
        )
        // itemRequiredId is unanswered → validation fails
        val session = fixtures.createSampleSession(
            answers = listOf(answer),
            status = InspectionStatus.REVIEWING,
        )
        val inspectionRepo = RecordingInspectionRepository()
        inspectionRepo.addSession(session)
        val templateRepo = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        )
        val issueRepo = FakeIssueRepository()
        val validateUseCase = ValidateInspectionUseCase()
        val scoreUseCase = CalculateInspectionScoreUseCase()
        val createIssueUseCase = CreateMaintenanceIssueUseCase(issueRepo)
        val scheduleUseCase = ScheduleNextInspectionUseCase()

        val completeUseCase = CompleteInspectionUseCase(
            inspectionRepository = inspectionRepo,
            templateRepository = templateRepo,
            issueRepository = issueRepo,
            validateInspection = validateUseCase,
            calculateScore = scoreUseCase,
            createIssue = createIssueUseCase,
            scheduleNext = scheduleUseCase,
        )

        // Act
        val result = completeUseCase(session.id)

        // Assert
        // Because validation fails → returns ValidationFailed, not Success
        assertTrue("Expected ValidationFailed", result is CompleteInspectionResult.ValidationFailed)
    }

    // ─────────────────────────────────────────────
    // TEST 7: nextInspectionDateUsesRecurrencePolicy
    // RULE 8: Next date = completedAt + policyDays * 86400000
    // ─────────────────────────────────────────────
    @Test
    fun `nextInspectionDateUsesRecurrencePolicy`() = runTest {
        // Arrange
        val scheduleUseCase = ScheduleNextInspectionUseCase()
        val template = fixtures.sampleTemplate // recurrencePolicyDays = 365
        val completedAt = 1000L

        // Act
        val nextDue = scheduleUseCase(template, completedAt)

        // Assert
        assertEquals(1000L + 365 * 86_400_000L, nextDue)
    }

    @Test
    fun `nextInspectionDateIsNullWhenNoRecurrencePolicy`() = runTest {
        // Arrange
        val scheduleUseCase = ScheduleNextInspectionUseCase()
        val template = fixtures.templateWithNoRecurrence
        val completedAt = 1000L

        // Act
        val nextDue = scheduleUseCase(template, completedAt)

        // Assert
        assertEquals(null, nextDue)
    }
}

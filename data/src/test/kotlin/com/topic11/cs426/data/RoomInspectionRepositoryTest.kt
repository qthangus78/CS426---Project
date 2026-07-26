package com.topic11.cs426.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomInspectionRepositoryTest {
    private lateinit var database: FieldFlowDatabase
    private lateinit var repository: RoomInspectionRepository

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FieldFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomInspectionRepository(
            database = database,
            inspectionIdFactory = { "inspection-created" },
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `Room summaries and sessions expose the same local source of truth`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()

        val summaries = repository.observeInspectionSummaries().first()
        val session = repository.observeInspection(InspectionId("computer-lab-i-44")).first()

        assertEquals(3, summaries.size)
        assertEquals("Computer Lab I.44", session?.assetName)
        assertEquals("sample-template-v1", session?.templateId?.value)
        assertEquals(InspectionStatus.IN_PROGRESS, session?.status)
        assertEquals(2, session?.answers?.size)
    }

    @Test
    fun `missing Room inspection emits null`() = runTest {
        assertNull(repository.observeInspection(InspectionId("missing")).first())
        assertNull(repository.getInspection(InspectionId("missing")))
    }

    @Test
    fun `create and save draft preserve template revision`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val id = repository.createInspection(
            assetId = "sample-asset-projector-p204",
            assetName = "Projector P-204",
            templateId = "sample-template-v1",
            startedAtMillis = 4_000L,
        )
        val created = requireNotNull(repository.getInspection(id))

        repository.saveDraft(
            created.copy(
                status = InspectionStatus.IN_PROGRESS,
                answers = listOf(answer(id, "sample-item-0")),
                updatedAtMillis = 4_500L,
            ),
        )

        val recovered = requireNotNull(repository.getInspection(id))

        assertEquals(InspectionId("inspection-created"), id)
        assertEquals("sample-template-v1", recovered.templateId.value)
        assertEquals(ChecklistAnswerValue.Pass, recovered.answers.single().value)
    }

    @Test
    fun `complete writes inspection issue and pending sync atomically and idempotently`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val inspectionId = InspectionId("computer-lab-i-44")
        val issue = MaintenanceIssue(
            id = IssueId("issue-sample"),
            inspectionId = inspectionId,
            assetId = AssetId("sample-asset-lab-i44"),
            checklistItemId = ChecklistItemId("sample-item-0"),
            severity = IssueSeverity.CRITICAL,
            title = "Critical failure",
            status = MaintenanceIssueStatus.OPEN,
            createdAtMillis = 5_000L,
        )

        val completed = CompletedInspection(
            id = inspectionId,
            answers = listOf(answer(inspectionId, "sample-item-0")),
            score = InspectionScore(earnedWeight = 1, totalWeight = 1),
            issues = listOf(issue),
            nextInspectionDueAtMillis = 86_400_000L,
            completedAtMillis = 5_000L,
        )
        repository.complete(completed)
        repository.complete(completed)

        val recovered = requireNotNull(repository.getInspection(inspectionId))
        assertEquals(InspectionStatus.SYNC_PENDING, recovered.status)
        assertEquals(
            86_400_000L,
            database.catalogDao()
                .getAsset("sample-asset-lab-i44")
                ?.nextInspectionDueAtMillis,
        )
        assertEquals(issue.id.value, database.issueDao().getIssue(issue.id.value)?.id)
        assertEquals(1, database.issueDao().observeIssues().first().size)
        assertEquals(
            1,
            database.syncDao().getCommands()
                .count { it.id == "sync-complete-${inspectionId.value}" },
        )
        assertEquals(
            "PENDING",
            database.syncDao().getCommand("sync-complete-${inspectionId.value}")?.state,
        )
    }

    @Test
    fun `complete rolls back inspection and asset due date when issue persistence fails`() = runTest {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val inspectionId = InspectionId("computer-lab-i-44")
        val assetId = "sample-asset-lab-i44"
        val originalAssetDue = database.catalogDao()
            .getAsset(assetId)
            ?.nextInspectionDueAtMillis
        val invalidIssue = MaintenanceIssue(
            id = IssueId("issue-invalid-checklist-item"),
            inspectionId = inspectionId,
            assetId = AssetId(assetId),
            checklistItemId = ChecklistItemId("missing-checklist-item"),
            severity = IssueSeverity.CRITICAL,
            title = "Cannot be persisted",
            status = MaintenanceIssueStatus.OPEN,
            createdAtMillis = 5_000L,
        )

        val failure = runCatching {
            repository.complete(
                CompletedInspection(
                    id = inspectionId,
                    answers = listOf(answer(inspectionId, "sample-item-0")),
                    score = InspectionScore(earnedWeight = 1, totalWeight = 1),
                    issues = listOf(invalidIssue),
                    nextInspectionDueAtMillis = 86_400_000L,
                    completedAtMillis = 5_000L,
                ),
            )
        }.exceptionOrNull()

        assertTrue("Expected the invalid issue foreign key to fail", failure != null)
        assertEquals(
            InspectionStatus.IN_PROGRESS,
            repository.getInspection(inspectionId)?.status,
        )
        assertEquals(
            originalAssetDue,
            database.catalogDao().getAsset(assetId)?.nextInspectionDueAtMillis,
        )
        assertNull(database.issueDao().getIssue(invalidIssue.id.value))
        assertNull(database.syncDao().getCommand("sync-complete-${inspectionId.value}"))
    }

    private fun answer(inspectionId: InspectionId, itemId: String) = InspectionAnswer(
        inspectionId = inspectionId,
        checklistItemId = ChecklistItemId(itemId),
        value = ChecklistAnswerValue.Pass,
        updatedAtMillis = 4_500L,
    )
}

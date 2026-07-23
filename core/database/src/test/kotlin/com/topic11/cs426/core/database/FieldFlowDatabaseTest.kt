package com.topic11.cs426.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.entity.AssetEntity
import com.topic11.cs426.core.database.entity.ChecklistItemEntity
import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.core.database.entity.InspectionSectionEntity
import com.topic11.cs426.core.database.entity.InspectionTemplateEntity
import com.topic11.cs426.core.database.entity.LocationEntity
import com.topic11.cs426.core.database.entity.MaintenanceIssueEntity
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FieldFlowDatabaseTest {
    private lateinit var database: FieldFlowDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FieldFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `catalog queries preserve template section and item order`() = runTest {
        insertCatalog()

        val sections = database.catalogDao().getSections(TEMPLATE_REVISION_ID)
        val items = database.catalogDao().getChecklistItems(TEMPLATE_REVISION_ID)

        assertEquals(listOf("section-safety", "section-equipment"), sections.map { it.id })
        assertEquals(listOf("item-extinguisher", "item-projector"), items.map { it.id })
    }

    @Test
    fun `saveDraft writes inspection answers and evidence atomically`() = runTest {
        insertCatalog()
        val draft = inspection(lifecycleStatus = "IN_PROGRESS", syncStatus = "NOT_REQUIRED")
        val answer = answer(note = "Checked locally")
        val evidence = evidence()

        database.inspectionDao().saveDraft(draft, listOf(answer), listOf(evidence))

        assertEquals(draft, database.inspectionDao().getInspection(INSPECTION_ID))
        assertEquals(listOf(answer), database.inspectionDao().observeAnswers(INSPECTION_ID).first())
        assertEquals(
            listOf(evidence),
            database.inspectionDao().observeEvidence(INSPECTION_ID).first(),
        )
    }

    @Test
    fun `completeInspection writes issue and pending sync in one transaction`() = runTest {
        insertCatalog()
        database.inspectionDao().saveDraft(
            inspection(lifecycleStatus = "IN_PROGRESS", syncStatus = "NOT_REQUIRED"),
            listOf(answer(note = null)),
            emptyList(),
        )
        val completed = inspection(lifecycleStatus = "COMPLETED", syncStatus = "PENDING")
        val issue = issue()
        val command = pendingSync()

        database.inspectionDao().completeInspection(
            inspection = completed,
            answers = listOf(answer(note = "Critical failure")),
            evidence = listOf(evidence()),
            issues = listOf(issue),
            pendingSync = listOf(command),
        )

        assertEquals(completed, database.inspectionDao().getInspection(INSPECTION_ID))
        assertEquals(
            listOf(issue),
            database.issueDao().observeIssuesForInspection(INSPECTION_ID).first(),
        )
        assertEquals(listOf(command), database.syncDao().observeRetryableCommands().first())
    }

    @Test
    fun `delete inspection cascades owned answers and evidence`() = runTest {
        insertCatalog()
        database.inspectionDao().saveDraft(
            inspection(lifecycleStatus = "IN_PROGRESS", syncStatus = "NOT_REQUIRED"),
            listOf(answer(note = null)),
            listOf(evidence()),
        )

        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM inspections WHERE id = ?",
            arrayOf(INSPECTION_ID),
        )

        assertNull(database.inspectionDao().getInspection(INSPECTION_ID))
        assertEquals(emptyList<InspectionAnswerEntity>(), database.inspectionDao().observeAnswers(INSPECTION_ID).first())
        assertEquals(emptyList<EvidenceEntity>(), database.inspectionDao().observeEvidence(INSPECTION_ID).first())
    }

    private suspend fun insertCatalog() {
        val catalogDao = database.catalogDao()
        catalogDao.upsertLocations(
            listOf(LocationEntity(id = "location-lab", name = "Laboratory", parentId = null)),
        )
        catalogDao.upsertAssets(
            listOf(
                AssetEntity(
                    id = ASSET_ID,
                    name = "Computer Lab I.44",
                    code = "LAB-I44",
                    locationId = "location-lab",
                    nextInspectionDueAtMillis = null,
                ),
            ),
        )
        catalogDao.upsertTemplates(
            listOf(
                InspectionTemplateEntity(
                    revisionId = TEMPLATE_REVISION_ID,
                    templateId = "template-lab",
                    version = 1,
                    name = "Computer Lab Inspection",
                    recurrenceIntervalDays = 30,
                ),
            ),
        )
        catalogDao.upsertSections(
            listOf(
                InspectionSectionEntity(
                    id = "section-equipment",
                    templateRevisionId = TEMPLATE_REVISION_ID,
                    title = "Equipment",
                    position = 1,
                ),
                InspectionSectionEntity(
                    id = "section-safety",
                    templateRevisionId = TEMPLATE_REVISION_ID,
                    title = "Safety",
                    position = 0,
                ),
            ),
        )
        catalogDao.upsertChecklistItems(
            listOf(
                ChecklistItemEntity(
                    id = "item-projector",
                    sectionId = "section-equipment",
                    title = "Projector works",
                    description = null,
                    position = 0,
                    isRequired = true,
                    isCritical = false,
                    weight = 1.0,
                    answerType = "PASS_FAIL_NA",
                    choiceOptionsJson = null,
                ),
                ChecklistItemEntity(
                    id = ITEM_ID,
                    sectionId = "section-safety",
                    title = "Fire extinguisher available",
                    description = null,
                    position = 0,
                    isRequired = true,
                    isCritical = true,
                    weight = 2.0,
                    answerType = "PASS_FAIL_NA",
                    choiceOptionsJson = null,
                ),
            ),
        )
    }

    private fun inspection(lifecycleStatus: String, syncStatus: String) = InspectionEntity(
        id = INSPECTION_ID,
        assetId = ASSET_ID,
        templateRevisionId = TEMPLATE_REVISION_ID,
        lifecycleStatus = lifecycleStatus,
        syncStatus = syncStatus,
        currentSectionId = "section-safety",
        startedAtMillis = 1_000L,
        updatedAtMillis = 2_000L,
        completedAtMillis = if (lifecycleStatus == "COMPLETED") 2_000L else null,
        earnedWeight = if (lifecycleStatus == "COMPLETED") 0.0 else null,
        totalWeight = if (lifecycleStatus == "COMPLETED") 2.0 else null,
    )

    private fun answer(note: String?) = InspectionAnswerEntity(
        inspectionId = INSPECTION_ID,
        checklistItemId = ITEM_ID,
        answerType = "PASS_FAIL_NA",
        valueText = "FAIL",
        valueNumber = null,
        valueBoolean = null,
        unit = null,
        note = note,
        updatedAtMillis = 2_000L,
    )

    private fun evidence() = EvidenceEntity(
        id = "evidence-photo",
        inspectionId = INSPECTION_ID,
        checklistItemId = ITEM_ID,
        storageKey = "evidence/evidence-photo.jpg",
        mimeType = "image/jpeg",
        createdAtMillis = 2_000L,
    )

    private fun issue() = MaintenanceIssueEntity(
        id = "issue-fire-extinguisher",
        inspectionId = INSPECTION_ID,
        assetId = ASSET_ID,
        checklistItemId = ITEM_ID,
        severity = "CRITICAL",
        title = "Fire extinguisher missing",
        description = "Critical inspection failure",
        status = "OPEN",
        createdAtMillis = 2_000L,
        updatedAtMillis = 2_000L,
    )

    private fun pendingSync() = PendingSyncEntity(
        id = "sync-inspection-complete",
        aggregateType = "INSPECTION",
        aggregateId = INSPECTION_ID,
        operation = "COMPLETE",
        payloadVersion = 1,
        payloadJson = """{"inspectionId":"$INSPECTION_ID"}""",
        state = "PENDING",
        attemptCount = 0,
        lastErrorCode = null,
        createdAtMillis = 2_000L,
        updatedAtMillis = 2_000L,
    )

    private companion object {
        const val ASSET_ID = "asset-lab-i44"
        const val TEMPLATE_REVISION_ID = "template-lab-v1"
        const val INSPECTION_ID = "inspection-lab-i44"
        const val ITEM_ID = "item-extinguisher"
    }
}

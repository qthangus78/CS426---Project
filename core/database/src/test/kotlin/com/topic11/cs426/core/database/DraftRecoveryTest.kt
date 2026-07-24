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
class DraftRecoveryTest {
    private lateinit var context: Context
    private var database: FieldFlowDatabase? = null

    @Before
    fun prepareDatabaseFile() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @After
    fun removeDatabaseFile() {
        database?.close()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun `draft survives database close and reopen`() = runTest {
        val expectedInspection = inspection()
        val expectedAnswer = answer()
        val expectedEvidence = evidence()
        openDatabase()
        insertCatalog()
        requireNotNull(database).inspectionDao().saveDraft(
            inspection = expectedInspection,
            answers = listOf(expectedAnswer),
            evidence = listOf(expectedEvidence),
        )

        reopenDatabase()
        val recovered = requireNotNull(database).inspectionDao().getDraft(INSPECTION_ID)

        requireNotNull(recovered)
        assertEquals(expectedInspection, recovered.inspection)
        assertEquals(CURRENT_SECTION_ID, recovered.inspection.currentSectionId)
        assertEquals(listOf(expectedAnswer), recovered.answers)
        assertEquals(listOf(expectedEvidence), recovered.evidence)
    }

    @Test
    fun `missing draft returns null after database reopen`() = runTest {
        openDatabase()
        reopenDatabase()

        val recovered = requireNotNull(database).inspectionDao().getDraft("missing")

        assertNull(recovered)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            FieldFlowDatabase::class.java,
            TEST_DATABASE_NAME,
        ).allowMainThreadQueries().build()
    }

    private fun reopenDatabase() {
        requireNotNull(database).close()
        openDatabase()
    }

    private suspend fun insertCatalog() {
        val catalogDao = requireNotNull(database).catalogDao()
        catalogDao.upsertLocations(
            listOf(LocationEntity(LOCATION_ID, "Laboratory", null)),
        )
        catalogDao.upsertAssets(
            listOf(
                AssetEntity(
                    id = ASSET_ID,
                    name = "Computer Lab I.44",
                    code = "LAB-I44",
                    locationId = LOCATION_ID,
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
                    id = CURRENT_SECTION_ID,
                    templateRevisionId = TEMPLATE_REVISION_ID,
                    title = "Safety",
                    position = 0,
                ),
            ),
        )
        catalogDao.upsertChecklistItems(
            listOf(
                ChecklistItemEntity(
                    id = ITEM_ID,
                    sectionId = CURRENT_SECTION_ID,
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

    private fun inspection() = InspectionEntity(
        id = INSPECTION_ID,
        assetId = ASSET_ID,
        templateRevisionId = TEMPLATE_REVISION_ID,
        lifecycleStatus = "IN_PROGRESS",
        syncStatus = "NOT_REQUIRED",
        currentSectionId = CURRENT_SECTION_ID,
        startedAtMillis = 1_000L,
        updatedAtMillis = 2_000L,
        completedAtMillis = null,
        earnedWeight = null,
        totalWeight = null,
    )

    private fun answer() = InspectionAnswerEntity(
        inspectionId = INSPECTION_ID,
        checklistItemId = ITEM_ID,
        answerType = "PASS_FAIL_NA",
        valueText = "FAIL",
        valueNumber = null,
        valueBoolean = null,
        unit = null,
        note = "Extinguisher is missing",
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

    private companion object {
        const val TEST_DATABASE_NAME = "draft-recovery-test.db"
        const val LOCATION_ID = "location-lab"
        const val ASSET_ID = "asset-lab-i44"
        const val TEMPLATE_REVISION_ID = "template-lab-v1"
        const val CURRENT_SECTION_ID = "section-safety"
        const val ITEM_ID = "item-extinguisher"
        const val INSPECTION_ID = "inspection-lab-i44"
    }
}

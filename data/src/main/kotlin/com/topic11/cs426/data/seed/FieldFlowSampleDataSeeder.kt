package com.topic11.cs426.data.seed

import androidx.room.withTransaction
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.core.database.entity.AssetEntity
import com.topic11.cs426.core.database.entity.ChecklistItemEntity
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.core.database.entity.InspectionSectionEntity
import com.topic11.cs426.core.database.entity.InspectionTemplateEntity
import com.topic11.cs426.core.database.entity.LocationEntity
import com.topic11.cs426.core.database.entity.PendingSyncEntity

class FieldFlowSampleDataSeeder(
    private val database: FieldFlowDatabase,
) {
    suspend fun seedIfEmpty(): Boolean = database.withTransaction {
        val inspectionDao = database.inspectionDao()
        if (inspectionDao.getInspectionCount() > 0) {
            return@withTransaction false
        }

        database.catalogDao().apply {
            upsertLocations(listOf(location))
            upsertAssets(assets)
            upsertTemplates(listOf(template))
            upsertSections(listOf(section))
            upsertChecklistItems(checklistItems)
        }

        inspectionDao.saveDraft(
            inspection = inspections[0],
            answers = answersFor(inspections[0].id, count = 2),
            evidence = emptyList(),
        )
        inspectionDao.saveDraft(
            inspection = inspections[1],
            answers = emptyList(),
            evidence = emptyList(),
        )
        inspectionDao.completeInspection(
            inspection = inspections[2],
            answers = answersFor(inspections[2].id, count = checklistItems.size),
            evidence = emptyList(),
            issues = emptyList(),
            pendingSync = listOf(pendingSync),
        )
        true
    }

    private fun answersFor(inspectionId: String, count: Int) =
        checklistItems.take(count).map { item ->
            InspectionAnswerEntity(
                inspectionId = inspectionId,
                checklistItemId = item.id,
                answerType = "PASS_FAIL_NA",
                valueText = "PASS",
                valueNumber = null,
                valueBoolean = null,
                unit = null,
                note = null,
                updatedAtMillis = SAMPLE_TIME,
            )
        }

    private companion object {
        const val SAMPLE_TIME = 1_735_689_600_000L
        const val TEMPLATE_REVISION_ID = "sample-template-v1"

        val location = LocationEntity(
            id = "sample-location",
            name = "HCMUS",
            parentId = null,
        )
        val assets = listOf(
            AssetEntity(
                id = "sample-asset-lab-i44",
                name = "Computer Lab I.44",
                code = "LAB-I44",
                locationId = location.id,
                nextInspectionDueAtMillis = null,
            ),
            AssetEntity(
                id = "sample-asset-projector-p204",
                name = "Projector P-204",
                code = "P-204",
                locationId = location.id,
                nextInspectionDueAtMillis = null,
            ),
            AssetEntity(
                id = "sample-asset-lab-a2",
                name = "Laboratory A2 Safety Check",
                code = "LAB-A2",
                locationId = location.id,
                nextInspectionDueAtMillis = null,
            ),
        )
        val template = InspectionTemplateEntity(
            revisionId = TEMPLATE_REVISION_ID,
            templateId = "sample-template",
            version = 1,
            name = "Sample Facility Inspection",
            recurrenceIntervalDays = 30,
        )
        val section = InspectionSectionEntity(
            id = "sample-section-safety",
            templateRevisionId = TEMPLATE_REVISION_ID,
            title = "Safety and equipment",
            position = 0,
        )
        val checklistItems = listOf(
            "Workspace is clean",
            "Electrical equipment is safe",
            "Emergency exit is accessible",
            "Fire extinguisher is available",
        ).mapIndexed { index, title ->
            ChecklistItemEntity(
                id = "sample-item-$index",
                sectionId = section.id,
                title = title,
                description = null,
                position = index,
                isRequired = true,
                isCritical = index == 3,
                weight = 1.0,
                answerType = "PASS_FAIL_NA",
                choiceOptionsJson = null,
            )
        }
        val inspections = listOf(
            InspectionEntity(
                id = "computer-lab-i-44",
                assetId = assets[0].id,
                templateRevisionId = TEMPLATE_REVISION_ID,
                lifecycleStatus = "IN_PROGRESS",
                syncStatus = "NOT_REQUIRED",
                currentSectionId = section.id,
                startedAtMillis = SAMPLE_TIME,
                updatedAtMillis = SAMPLE_TIME + 3_000L,
                completedAtMillis = null,
                earnedWeight = null,
                totalWeight = null,
            ),
            InspectionEntity(
                id = "projector-p-204",
                assetId = assets[1].id,
                templateRevisionId = TEMPLATE_REVISION_ID,
                lifecycleStatus = "NOT_STARTED",
                syncStatus = "NOT_REQUIRED",
                currentSectionId = null,
                startedAtMillis = SAMPLE_TIME,
                updatedAtMillis = SAMPLE_TIME + 2_000L,
                completedAtMillis = null,
                earnedWeight = null,
                totalWeight = null,
            ),
            InspectionEntity(
                id = "laboratory-a2-safety-check",
                assetId = assets[2].id,
                templateRevisionId = TEMPLATE_REVISION_ID,
                lifecycleStatus = "COMPLETED",
                syncStatus = "PENDING",
                currentSectionId = section.id,
                startedAtMillis = SAMPLE_TIME,
                updatedAtMillis = SAMPLE_TIME + 1_000L,
                completedAtMillis = SAMPLE_TIME + 1_000L,
                earnedWeight = 4.0,
                totalWeight = 4.0,
            ),
        )
        val pendingSync = PendingSyncEntity(
            id = "sample-sync-lab-a2",
            aggregateType = "INSPECTION",
            aggregateId = inspections[2].id,
            operation = "COMPLETE",
            payloadVersion = 1,
            payloadJson = """{"inspectionId":"${inspections[2].id}"}""",
            state = "PENDING",
            attemptCount = 0,
            lastErrorCode = null,
            createdAtMillis = SAMPLE_TIME + 1_000L,
            updatedAtMillis = SAMPLE_TIME + 1_000L,
        )
    }
}

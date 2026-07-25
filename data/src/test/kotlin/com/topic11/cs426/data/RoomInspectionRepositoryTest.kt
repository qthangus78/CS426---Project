package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.InspectionDao
import com.topic11.cs426.core.database.dao.InspectionSessionRecord
import com.topic11.cs426.core.database.dao.InspectionSummaryRecord
import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.core.database.entity.MaintenanceIssueEntity
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import com.topic11.cs426.data.mapping.PersistenceMappingException
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomInspectionRepositoryTest {
    @Test
    fun `observeInspectionSummaries maps DAO records in database order`() = runTest {
        val records = listOf(
            record(
                id = "inspection-lab",
                title = "Computer Lab I.44",
                lifecycle = "IN_PROGRESS",
                completedItems = 3,
                totalItems = 5,
            ),
            record(
                id = "inspection-projector",
                title = "Projector P-204",
                lifecycle = "NOT_STARTED",
                completedItems = 0,
                totalItems = 8,
            ),
        )
        val repository = RoomInspectionRepository(ControlledInspectionDao(records))

        val result = repository.observeInspectionSummaries().first()

        assertEquals(records.map { it.inspectionId }, result.map { it.id.value })
        assertEquals(listOf(InspectionStatus.IN_PROGRESS, InspectionStatus.NOT_STARTED), result.map { it.status })
        assertEquals(listOf(3, 0), result.map { it.completedItems })
    }

    @Test
    fun `observeInspection maps matching DAO record`() = runTest {
        val repository = RoomInspectionRepository(
            ControlledInspectionDao(
                listOf(
                    record(
                        id = "inspection-lab",
                        title = "Computer Lab I.44",
                        lifecycle = "COMPLETED",
                        sync = "PENDING",
                        completedItems = 5,
                        totalItems = 5,
                    ),
                ),
            ),
        )

        val result = repository.observeInspection(InspectionId("inspection-lab")).first()

        assertEquals("Computer Lab I.44", result?.assetName)
        assertEquals("Standard Inspection", result?.templateName)
        assertEquals(InspectionStatus.SYNC_PENDING, result?.status)
    }

    @Test
    fun `local DAO update becomes the next repository emission`() = runTest {
        val dao = ControlledInspectionDao(
            listOf(
                record(
                    id = "inspection-lab",
                    title = "Computer Lab I.44",
                    lifecycle = "IN_PROGRESS",
                    completedItems = 1,
                    totalItems = 5,
                ),
            ),
        )
        val repository = RoomInspectionRepository(dao)
        val emissions = mutableListOf<List<Int>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeInspectionSummaries()
                .map { summaries -> summaries.map { it.completedItems } }
                .take(2)
                .toList(emissions)
        }

        dao.replaceRecords(
            listOf(
                record(
                    id = "inspection-lab",
                    title = "Computer Lab I.44",
                    lifecycle = "IN_PROGRESS",
                    completedItems = 2,
                    totalItems = 5,
                ),
            ),
        )
        runCurrent()

        assertEquals(listOf(listOf(1), listOf(2)), emissions)
    }

    @Test
    fun `observeInspection emits null when DAO has no matching record`() = runTest {
        val repository = RoomInspectionRepository(ControlledInspectionDao(emptyList()))

        val result = repository.observeInspection(InspectionId("missing")).first()

        assertNull(result)
    }

    @Test
    fun `repository does not hide malformed persistence data`() {
        val repository = RoomInspectionRepository(
            ControlledInspectionDao(
                listOf(
                    record(
                        id = "inspection-lab",
                        title = "Computer Lab I.44",
                        lifecycle = "UNKNOWN",
                        completedItems = 0,
                        totalItems = 1,
                    ),
                ),
            ),
        )

        assertThrows(PersistenceMappingException::class.java) {
            runTest {
                repository.observeInspectionSummaries().first()
            }
        }
    }

    private fun record(
        id: String,
        title: String,
        lifecycle: String,
        sync: String = "NOT_REQUIRED",
        completedItems: Int,
        totalItems: Int,
    ) = InspectionSummaryRecord(
        inspectionId = id,
        title = title,
        lifecycleStatus = lifecycle,
        syncStatus = sync,
        completedItems = completedItems,
        totalItems = totalItems,
    )
}

private class ControlledInspectionDao(
    initialRecords: List<InspectionSummaryRecord>,
) : InspectionDao {
    private val records = MutableStateFlow(initialRecords)
    private val sessionRecords = MutableStateFlow(initialRecords.map(InspectionSummaryRecord::toSessionRecord))

    override suspend fun getInspectionCount(): Int = records.value.size

    override fun observeInspectionSummaries(): Flow<List<InspectionSummaryRecord>> = records

    override fun observeInspectionSummary(
        inspectionId: String,
    ): Flow<InspectionSummaryRecord?> {
        return records.map { summaries ->
            summaries.firstOrNull { it.inspectionId == inspectionId }
        }
    }

    override fun observeInspectionSession(inspectionId: String): Flow<InspectionSessionRecord?> {
        return sessionRecords.map { sessions ->
            sessions.firstOrNull { it.inspectionId == inspectionId }
        }
    }

    override suspend fun getInspectionSession(inspectionId: String): InspectionSessionRecord? {
        return sessionRecords.value.firstOrNull { it.inspectionId == inspectionId }
    }

    override suspend fun getLatestTemplateRevisionId(templateId: String): String? {
        return "template-standard-v1"
    }

    override fun observeInspections(): Flow<List<InspectionEntity>> = unused()

    override fun observeInspection(inspectionId: String): Flow<InspectionEntity?> = unused()

    override suspend fun getInspection(inspectionId: String): InspectionEntity? = unused()

    override suspend fun getAnswers(
        inspectionId: String,
    ): List<InspectionAnswerEntity> = emptyList()

    override suspend fun getEvidence(inspectionId: String): List<EvidenceEntity> = emptyList()

    override fun observeAnswers(
        inspectionId: String,
    ): Flow<List<InspectionAnswerEntity>> = flowOf(emptyList())

    override fun observeEvidence(inspectionId: String): Flow<List<EvidenceEntity>> = flowOf(emptyList())

    override suspend fun upsertInspection(inspection: InspectionEntity): Unit = unused()

    override suspend fun upsertAnswers(answers: List<InspectionAnswerEntity>): Unit = unused()

    override suspend fun upsertEvidence(evidence: List<EvidenceEntity>): Unit = unused()

    override suspend fun upsertIssues(issues: List<MaintenanceIssueEntity>): Unit = unused()

    override suspend fun insertPendingSync(commands: List<PendingSyncEntity>): Unit = unused()

    fun replaceRecords(updatedRecords: List<InspectionSummaryRecord>) {
        records.value = updatedRecords
        sessionRecords.value = updatedRecords.map(InspectionSummaryRecord::toSessionRecord)
    }

    private fun unused(): Nothing = error("DAO operation is outside this repository test")
}

private fun InspectionSummaryRecord.toSessionRecord() = InspectionSessionRecord(
    inspectionId = inspectionId,
    assetId = "asset-$inspectionId",
    assetName = title,
    templateId = "template-standard",
    templateName = "Standard Inspection",
    lifecycleStatus = lifecycleStatus,
    syncStatus = syncStatus,
    currentSectionId = null,
    startedAtMillis = 0L,
    updatedAtMillis = 0L,
    completedAtMillis = if (lifecycleStatus == "COMPLETED") 1L else null,
    earnedWeight = null,
    totalWeight = null,
)

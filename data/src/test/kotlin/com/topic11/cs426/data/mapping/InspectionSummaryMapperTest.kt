package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.dao.InspectionSummaryRecord
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InspectionSummaryMapperTest {
    @Test
    fun `maps every supported lifecycle and sync combination explicitly`() {
        val cases = listOf(
            record(lifecycle = "NOT_STARTED") to InspectionStatus.NOT_STARTED,
            record(lifecycle = "IN_PROGRESS") to InspectionStatus.IN_PROGRESS,
            record(lifecycle = "REVIEWING") to InspectionStatus.IN_PROGRESS,
            record(lifecycle = "COMPLETED") to InspectionStatus.COMPLETED,
            record(lifecycle = "COMPLETED", sync = "SYNCED") to InspectionStatus.COMPLETED,
            record(lifecycle = "COMPLETED", sync = "PENDING") to InspectionStatus.SYNC_PENDING,
            record(lifecycle = "COMPLETED", sync = "SYNCING") to InspectionStatus.SYNC_PENDING,
            record(lifecycle = "COMPLETED", sync = "FAILED") to InspectionStatus.SYNC_PENDING,
        )

        cases.forEach { (persistence, expectedStatus) ->
            assertEquals(expectedStatus, persistence.toDomain().status)
        }
    }

    @Test
    fun `maps all summary fields`() {
        val persistence = record(
            lifecycle = "IN_PROGRESS",
            completedItems = 3,
            totalItems = 5,
        )

        val result = persistence.toDomain()

        assertEquals(
            InspectionSummary(
                id = InspectionId("inspection-lab-i44"),
                title = "Computer Lab I.44",
                status = InspectionStatus.IN_PROGRESS,
                completedItems = 3,
                totalItems = 5,
            ),
            result,
        )
    }

    @Test
    fun `rejects unknown lifecycle status`() {
        assertThrows(PersistenceMappingException::class.java) {
            record(lifecycle = "ARCHIVED").toDomain()
        }
    }

    @Test
    fun `rejects unknown sync status`() {
        assertThrows(PersistenceMappingException::class.java) {
            record(lifecycle = "COMPLETED", sync = "UNKNOWN").toDomain()
        }
    }

    @Test
    fun `rejects sync state before inspection is completed`() {
        assertThrows(PersistenceMappingException::class.java) {
            record(lifecycle = "IN_PROGRESS", sync = "PENDING").toDomain()
        }
    }

    @Test
    fun `rejects malformed progress counts`() {
        assertThrows(PersistenceMappingException::class.java) {
            record(lifecycle = "IN_PROGRESS", completedItems = 6, totalItems = 5).toDomain()
        }
    }

    private fun record(
        lifecycle: String,
        sync: String = "NOT_REQUIRED",
        completedItems: Int = 0,
        totalItems: Int = 5,
    ) = InspectionSummaryRecord(
        inspectionId = "inspection-lab-i44",
        title = "Computer Lab I.44",
        lifecycleStatus = lifecycle,
        syncStatus = sync,
        completedItems = completedItems,
        totalItems = totalItems,
    )
}

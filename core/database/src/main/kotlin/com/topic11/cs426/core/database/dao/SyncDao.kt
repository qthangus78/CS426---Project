package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query(
        """
        SELECT * FROM pending_sync
        WHERE state IN ('PENDING', 'FAILED')
        ORDER BY updated_at_ms, id
        """,
    )
    fun observeRetryableCommands(): Flow<List<PendingSyncEntity>>

    @Query("SELECT * FROM pending_sync WHERE id = :id")
    suspend fun getCommand(id: String): PendingSyncEntity?

    @Query("SELECT * FROM pending_sync ORDER BY created_at_ms, id")
    suspend fun getCommands(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueueCommand(command: PendingSyncEntity): Long

    @Query(
        """
        UPDATE pending_sync
        SET state = 'SYNCING',
            attempt_count = attempt_count + 1,
            last_error_code = NULL,
            updated_at_ms = :updatedAtMillis
        WHERE id = :id AND state IN ('PENDING', 'FAILED')
        """,
    )
    suspend fun markSyncing(
        id: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET state = 'SYNCED',
            last_error_code = NULL,
            updated_at_ms = :updatedAtMillis
        WHERE id = :id AND state = 'SYNCING'
        """,
    )
    suspend fun markSynced(
        id: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET state = 'FAILED',
            last_error_code = :errorCode,
            updated_at_ms = :updatedAtMillis
        WHERE id = :id AND state = 'SYNCING'
        """,
    )
    suspend fun markFailed(
        id: String,
        errorCode: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        UPDATE pending_sync
        SET state = 'PENDING',
            last_error_code = NULL,
            updated_at_ms = :updatedAtMillis
        WHERE id = :id AND state = 'FAILED'
        """,
    )
    suspend fun retryFailed(
        id: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        """
        UPDATE inspections
        SET sync_status = :syncStatus,
            updated_at_ms = :updatedAtMillis
        WHERE id = :inspectionId
        """,
    )
    suspend fun updateInspectionSyncStatus(
        inspectionId: String,
        syncStatus: String,
        updatedAtMillis: Long,
    ): Int

    @Transaction
    suspend fun markSyncingWithInspection(
        id: String,
        updatedAtMillis: Long,
    ): Int {
        val command = getCommand(id) ?: return 0
        val updated = markSyncing(id, updatedAtMillis)
        if (updated == 1) {
            updateInspectionStatusFor(command, "SYNCING", updatedAtMillis)
        }
        return updated
    }

    @Transaction
    suspend fun markSyncedWithInspection(
        id: String,
        updatedAtMillis: Long,
    ): Int {
        val command = getCommand(id) ?: return 0
        val updated = markSynced(id, updatedAtMillis)
        if (updated == 1) {
            updateInspectionStatusFor(command, "SYNCED", updatedAtMillis)
        }
        return updated
    }

    @Transaction
    suspend fun markFailedWithInspection(
        id: String,
        errorCode: String,
        updatedAtMillis: Long,
    ): Int {
        val command = getCommand(id) ?: return 0
        val updated = markFailed(id, errorCode, updatedAtMillis)
        if (updated == 1) {
            updateInspectionStatusFor(command, "FAILED", updatedAtMillis)
        }
        return updated
    }

    private suspend fun updateInspectionStatusFor(
        command: PendingSyncEntity,
        syncStatus: String,
        updatedAtMillis: Long,
    ) {
        if (command.aggregateType != "INSPECTION") return
        check(
            updateInspectionSyncStatus(
                inspectionId = command.aggregateId,
                syncStatus = syncStatus,
                updatedAtMillis = updatedAtMillis,
            ) == 1,
        ) {
            "Sync command references a missing inspection: ${command.aggregateId}"
        }
    }
}

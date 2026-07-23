package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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

    @Upsert
    suspend fun upsertCommand(command: PendingSyncEntity)

    @Query(
        """
        UPDATE pending_sync
        SET state = :state,
            attempt_count = :attemptCount,
            last_error_code = :lastErrorCode,
            updated_at_ms = :updatedAtMillis
        WHERE id = :id
        """,
    )
    suspend fun updateState(
        id: String,
        state: String,
        attemptCount: Int,
        lastErrorCode: String?,
        updatedAtMillis: Long,
    ): Int
}

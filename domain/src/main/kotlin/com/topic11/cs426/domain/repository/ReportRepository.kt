package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.ReportHistoryEntry
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun observeExportHistory(): Flow<List<ReportHistoryEntry>>

    suspend fun saveExport(entry: ReportHistoryEntry)
}

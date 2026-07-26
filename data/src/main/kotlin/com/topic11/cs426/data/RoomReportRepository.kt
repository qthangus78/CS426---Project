package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.ReportHistoryDao
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.data.mapping.toEntity
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomReportRepository(
    private val reportHistoryDao: ReportHistoryDao,
) : ReportRepository {
    override fun observeExportHistory(): Flow<List<ReportHistoryEntry>> =
        reportHistoryDao.observeExports()
            .map { exports -> exports.map { it.toDomain() } }
            .distinctUntilChanged()

    override suspend fun saveExport(entry: ReportHistoryEntry) {
        reportHistoryDao.upsertExport(entry.toEntity())
    }
}

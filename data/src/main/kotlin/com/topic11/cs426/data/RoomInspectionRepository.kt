package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.InspectionDao
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomInspectionRepository(
    private val inspectionDao: InspectionDao,
) : InspectionRepository {
    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> {
        return inspectionDao.observeInspectionSummaries()
            .map { records -> records.map { it.toDomain() } }
            .distinctUntilChanged()
    }

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSummary?> {
        return inspectionDao.observeInspectionSummary(inspectionId.value)
            .map { record -> record?.toDomain() }
            .distinctUntilChanged()
    }
}

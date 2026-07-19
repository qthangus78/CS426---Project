package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSummary
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun observeInspectionSummaries(): Flow<List<InspectionSummary>>

    fun observeInspection(
        inspectionId: InspectionId,
    ): Flow<InspectionSummary?>
}

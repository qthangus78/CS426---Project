package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionSummary
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun observeInspectionSummaries(): Flow<List<InspectionSummary>>

    fun observeInspection(
        inspectionId: InspectionId,
    ): Flow<InspectionSession?>

    suspend fun getInspection(
        inspectionId: InspectionId,
    ): InspectionSession?

    suspend fun createInspection(
        assetId: String,
        assetName: String,
        templateId: String,
        startedAtMillis: Long,
    ): InspectionId

    suspend fun saveDraft(session: InspectionSession)

    suspend fun complete(completed: CompletedInspection)
}

package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveInspectionUseCase(
    private val inspectionRepository: InspectionRepository,
) {
    operator fun invoke(inspectionId: InspectionId): Flow<InspectionSummary?> {
        return inspectionRepository.observeInspection(inspectionId)
    }
}

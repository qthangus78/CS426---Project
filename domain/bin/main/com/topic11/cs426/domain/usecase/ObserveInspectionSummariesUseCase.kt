package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveInspectionSummariesUseCase(
    private val inspectionRepository: InspectionRepository,
) {
    operator fun invoke(): Flow<List<InspectionSummary>> {
        return inspectionRepository.observeInspectionSummaries()
    }
}

package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.repository.InspectionRepository

class SaveInspectionDraftUseCase(
    private val inspectionRepository: InspectionRepository,
) {
    suspend operator fun invoke(session: InspectionSession) {
        val updatedSession = session.copy(
            updatedAtMillis = System.currentTimeMillis(),
        )
        inspectionRepository.saveDraft(updatedSession)
    }
}

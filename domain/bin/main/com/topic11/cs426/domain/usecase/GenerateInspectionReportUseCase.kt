package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.ReportId
import com.topic11.cs426.domain.repository.InspectionRepository
import java.util.UUID

class GenerateInspectionReportUseCase(
    private val inspectionRepository: InspectionRepository,
) {
    /**
     * RULE 7: Report chỉ được tạo khi inspection đã COMPLETED hoặc SYNC_PENDING.
     *
     * @throws IllegalStateException nếu inspection chưa hoàn tất.
     */
    suspend operator fun invoke(inspectionId: InspectionId): InspectionReport {
        val session = inspectionRepository.getInspection(inspectionId)
            ?: throw IllegalStateException("Inspection not found")

        if (session.status != InspectionStatus.COMPLETED &&
            session.status != InspectionStatus.SYNC_PENDING
        ) {
            throw IllegalStateException(
                "Report requires completed inspection, current status: ${session.status}",
            )
        }

        val score = session.score ?: InspectionScore(earnedWeight = 0, totalWeight = 0)

        return InspectionReport(
            id = ReportId(UUID.randomUUID().toString()),
            inspectionId = inspectionId,
            summary = "Inspection report for ${session.assetName}",
            score = score,
            issues = emptyList(), // Issues stored in CompletedInspection; full issue list TBD in P0
            generatedAtMillis = System.currentTimeMillis(),
        )
    }
}
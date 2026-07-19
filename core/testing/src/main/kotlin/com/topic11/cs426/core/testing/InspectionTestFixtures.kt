package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary

object InspectionTestFixtures {
    val computerLab = InspectionSummary(
        id = InspectionId("computer-lab-i-44"),
        title = "Computer Lab I.44",
        status = InspectionStatus.IN_PROGRESS,
        completedItems = 6,
        totalItems = 10,
    )

    val projector = InspectionSummary(
        id = InspectionId("projector-p-204"),
        title = "Projector P-204",
        status = InspectionStatus.NOT_STARTED,
        completedItems = 0,
        totalItems = 8,
    )

    val laboratorySafetyCheck = InspectionSummary(
        id = InspectionId("laboratory-a2-safety-check"),
        title = "Laboratory A2 Safety Check",
        status = InspectionStatus.SYNC_PENDING,
        completedItems = 12,
        totalItems = 12,
    )

    val inspectionSummaries = listOf(
        computerLab,
        projector,
        laboratorySafetyCheck,
    )
}

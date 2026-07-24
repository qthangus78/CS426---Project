package com.topic11.cs426.core.database.dao

import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.core.database.entity.InspectionEntity

data class InspectionDraftRecord(
    val inspection: InspectionEntity,
    val answers: List<InspectionAnswerEntity>,
    val evidence: List<EvidenceEntity>,
)

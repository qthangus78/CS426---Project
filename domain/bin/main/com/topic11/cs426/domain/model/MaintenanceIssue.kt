package com.topic11.cs426.domain.model

data class MaintenanceIssue(
    val id: IssueId,
    val inspectionId: InspectionId,
    val assetId: AssetId,
    val checklistItemId: ChecklistItemId? = null,
    val severity: IssueSeverity,
    val title: String,
    val description: String? = null,
    val status: MaintenanceIssueStatus,
    val createdAtMillis: Long,
) {
    init {
        require(title.isNotBlank()) { "Issue title cannot be blank." }
    }
}

enum class MaintenanceIssueStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
}

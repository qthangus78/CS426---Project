package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.repository.IssueRepository

class CreateMaintenanceIssueUseCase(
    private val issueRepository: IssueRepository,
) {
    suspend operator fun invoke(
        failures: List<CriticalFailure>,
    ): List<MaintenanceIssue> {
        val issues = failures.map { failure ->
            MaintenanceIssue(
                id = IssueId("issue-${System.currentTimeMillis()}-${failures.indexOf(failure)}"),
                inspectionId = failure.inspectionId,
                assetId = failure.assetId,
                checklistItemId = failure.checklistItemId,
                severity = failure.severity,
                title = failure.title,
                description = failure.description,
                status = MaintenanceIssueStatus.OPEN,
                createdAtMillis = System.currentTimeMillis(),
            )
        }

        return issues.map { issue ->
            issueRepository.createIssue(issue)
            issue.copy(id = issue.id)
        }
    }
}

data class CriticalFailure(
    val inspectionId: com.topic11.cs426.domain.model.InspectionId,
    val assetId: com.topic11.cs426.domain.model.AssetId,
    val checklistItemId: com.topic11.cs426.domain.model.ChecklistItemId,
    val severity: IssueSeverity = IssueSeverity.CRITICAL,
    val title: String,
    val description: String? = null,
)

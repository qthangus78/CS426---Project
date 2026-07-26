package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.repository.IssueRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class ObserveIssuesUseCase(
    private val issueRepository: IssueRepository,
) {
    operator fun invoke(): Flow<List<MaintenanceIssue>> = issueRepository.observeIssues()
}

class ObserveIssueUseCase(
    private val issueRepository: IssueRepository,
) {
    operator fun invoke(issueId: IssueId): Flow<MaintenanceIssue?> =
        issueRepository.observeIssue(issueId)
}

class UpdateIssueStatusUseCase(
    private val issueRepository: IssueRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        issueId: IssueId,
        newStatus: MaintenanceIssueStatus,
    ): IssueStatusUpdateResult {
        val issue = issueRepository.getIssue(issueId)
            ?: return IssueStatusUpdateResult.MissingIssue
        val allowed = IssueLifecycle.allowedNextStatuses(issue.status)

        if (newStatus !in allowed) {
            return IssueStatusUpdateResult.InvalidTransition(
                from = issue.status,
                to = newStatus,
                allowedStatuses = allowed,
            )
        }

        val updated = issue.copy(
            status = newStatus,
            updatedAtMillis = clock(),
        )

        return try {
            issueRepository.updateIssue(updated)
            IssueStatusUpdateResult.Success(updated)
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            IssueStatusUpdateResult.UpdateFailed
        }
    }

    fun allowedNextStatuses(status: MaintenanceIssueStatus): List<MaintenanceIssueStatus> =
        IssueLifecycle.allowedNextStatuses(status)
}

object IssueLifecycle {
    fun allowedNextStatuses(status: MaintenanceIssueStatus): List<MaintenanceIssueStatus> =
        when (status) {
            MaintenanceIssueStatus.OPEN -> listOf(MaintenanceIssueStatus.IN_PROGRESS)
            MaintenanceIssueStatus.IN_PROGRESS -> listOf(MaintenanceIssueStatus.RESOLVED)
            MaintenanceIssueStatus.RESOLVED -> listOf(MaintenanceIssueStatus.CLOSED)
            MaintenanceIssueStatus.CLOSED -> emptyList()
        }
}

sealed interface IssueStatusUpdateResult {
    data class Success(val issue: MaintenanceIssue) : IssueStatusUpdateResult

    data object MissingIssue : IssueStatusUpdateResult

    data class InvalidTransition(
        val from: MaintenanceIssueStatus,
        val to: MaintenanceIssueStatus,
        val allowedStatuses: List<MaintenanceIssueStatus>,
    ) : IssueStatusUpdateResult

    data object UpdateFailed : IssueStatusUpdateResult
}

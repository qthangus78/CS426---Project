package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.MaintenanceIssue
import kotlinx.coroutines.flow.Flow

interface IssueRepository {
    fun observeIssues(): Flow<List<MaintenanceIssue>>

    fun observeIssue(issueId: IssueId): Flow<MaintenanceIssue?>

    suspend fun getIssue(issueId: IssueId): MaintenanceIssue?

    suspend fun getIssuesForInspection(inspectionId: InspectionId): List<MaintenanceIssue>

    suspend fun createIssue(issue: MaintenanceIssue): IssueId

    suspend fun updateIssue(issue: MaintenanceIssue)
}

package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.IssueDao
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.data.mapping.toEntity
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.repository.IssueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomIssueRepository(
    private val issueDao: IssueDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : IssueRepository {
    override fun observeIssues(): Flow<List<MaintenanceIssue>> =
        issueDao.observeIssues()
            .map { issues -> issues.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun observeIssue(issueId: IssueId): Flow<MaintenanceIssue?> =
        issueDao.observeIssue(issueId.value)
            .map { issue -> issue?.toDomain() }
            .distinctUntilChanged()

    override suspend fun getIssue(issueId: IssueId): MaintenanceIssue? =
        issueDao.getIssue(issueId.value)?.toDomain()

    override suspend fun getIssuesForInspection(inspectionId: InspectionId): List<MaintenanceIssue> =
        issueDao.getIssuesForInspection(inspectionId.value)
            .map { it.toDomain() }

    override suspend fun createIssue(issue: MaintenanceIssue): IssueId {
        check(issueDao.getIssue(issue.id.value) == null) {
            "Issue already exists: ${issue.id.value}"
        }
        issueDao.upsertIssue(issue.toEntity(clock()))
        return issue.id
    }

    override suspend fun updateIssue(issue: MaintenanceIssue) {
        checkNotNull(issueDao.getIssue(issue.id.value)) {
            "Issue does not exist: ${issue.id.value}"
        }
        issueDao.upsertIssue(issue.toEntity(clock()))
    }
}

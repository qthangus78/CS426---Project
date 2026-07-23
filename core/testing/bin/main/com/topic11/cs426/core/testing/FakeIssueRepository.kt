package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.repository.IssueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeIssueRepository : IssueRepository {
    private val _issues = mutableListOf<MaintenanceIssue>()
    private val issuesFlow = MutableStateFlow<List<MaintenanceIssue>>(emptyList())

    var createIssueCalls: Int = 0
        private set

    val createdIssues: List<MaintenanceIssue>
        get() = _issues.toList()

    override fun observeIssues(): Flow<List<MaintenanceIssue>> = issuesFlow

    override suspend fun createIssue(issue: MaintenanceIssue): IssueId {
        createIssueCalls += 1
        _issues.add(issue)
        issuesFlow.value = _issues.toList()
        return issue.id
    }

    override suspend fun updateIssue(issue: MaintenanceIssue) {
        val index = _issues.indexOfFirst { it.id == issue.id }
        if (index >= 0) {
            _issues[index] = issue
            issuesFlow.value = _issues.toList()
        }
    }
}

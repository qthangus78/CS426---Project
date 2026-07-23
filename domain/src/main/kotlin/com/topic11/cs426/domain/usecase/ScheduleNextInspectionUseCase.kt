package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionTemplate

class ScheduleNextInspectionUseCase {

    /**
     * RULE 8: Next inspection date ưu tiên recurrence policy của template.
     * Nếu template không có → null.
     *
     * @return nextDueAtMillis hoặc null nếu không có policy.
     */
    operator fun invoke(
        template: InspectionTemplate,
        completedAtMillis: Long,
    ): Long? {
        val policyDays = template.recurrencePolicyDays ?: return null
        require(policyDays > 0) { "Recurrence policy must be positive." }
        return completedAtMillis + (policyDays * 86_400_000L)
    }
}

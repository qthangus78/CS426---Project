package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.InspectionTemplate

class ScheduleNextInspectionUseCase {

    /**
     * RULE 8: Next inspection date prioritises template recurrence policy,
     * then falls back to asset recurrence policy. Returns null if neither exists.
     *
     * @return nextDueAtMillis or null if no policy is configured.
     */
    operator fun invoke(
        template: InspectionTemplate,
        asset: Asset,
        completedAtMillis: Long,
    ): Long? {
        val policyDays = template.recurrencePolicyDays
            ?: asset.recurrencePolicyDays
            ?: return null

        require(policyDays > 0) { "Recurrence policy must be positive." }
        return completedAtMillis + (policyDays * 86_400_000L)
    }
}
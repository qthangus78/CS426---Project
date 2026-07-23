package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionTemplate

class CalculateInspectionScoreUseCase {

    operator fun invoke(
        session: InspectionSession,
        template: InspectionTemplate,
    ): InspectionScore {
        var earnedWeight = 0
        var totalWeight = 0

        val answerMap = session.answers.associateBy { it.checklistItemId }

        for (section in template.sections) {
            for (item in section.items) {
                val answer = answerMap[item.id]

                // RULE 6: NotApplicable is excluded from denominator
                if (answer?.value == ChecklistAnswerValue.NotApplicable) continue

                // RULE 5: Pass / Yes(true) earns weight, Fail / No(false) does not
                val scored = when (val value = answer?.value) {
                    is ChecklistAnswerValue.Pass -> true
                    is ChecklistAnswerValue.YesNo -> (value as ChecklistAnswerValue.YesNo).value
                    else -> false
                }

                totalWeight += item.weight
                if (scored) earnedWeight += item.weight
            }
        }

        return InspectionScore(
            earnedWeight = earnedWeight,
            totalWeight = totalWeight,
        )
    }
}

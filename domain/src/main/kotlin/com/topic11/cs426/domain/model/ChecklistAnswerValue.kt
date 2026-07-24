package com.topic11.cs426.domain.model

sealed interface ChecklistAnswerValue {
    data object Pass : ChecklistAnswerValue
    data object Fail : ChecklistAnswerValue
    data object NotApplicable : ChecklistAnswerValue
    data class YesNo(val value: Boolean) : ChecklistAnswerValue
    data class Text(val value: String) : ChecklistAnswerValue
    data class NumberValue(val value: Double, val unit: String? = null) : ChecklistAnswerValue
    data class SingleChoice(val optionId: String) : ChecklistAnswerValue
}

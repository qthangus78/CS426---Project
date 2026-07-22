package com.topic11.cs426.domain.model

@JvmInline
value class InspectionId(val value: String) {
    init {
        require(value.isNotBlank()) { "InspectionId cannot be blank." }
    }
}

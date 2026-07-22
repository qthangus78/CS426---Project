package com.topic11.cs426.domain.model

@JvmInline
value class AssetId(val value: String) {
    init {
        require(value.isNotBlank()) { "AssetId cannot be blank." }
    }
}

@JvmInline
value class TemplateId(val value: String) {
    init {
        require(value.isNotBlank()) { "TemplateId cannot be blank." }
    }
}

@JvmInline
value class SectionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SectionId cannot be blank." }
    }
}

@JvmInline
value class ChecklistItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "ChecklistItemId cannot be blank." }
    }
}

@JvmInline
value class EvidenceId(val value: String) {
    init {
        require(value.isNotBlank()) { "EvidenceId cannot be blank." }
    }
}

@JvmInline
value class IssueId(val value: String) {
    init {
        require(value.isNotBlank()) { "IssueId cannot be blank." }
    }
}

@JvmInline
value class LocationId(val value: String) {
    init {
        require(value.isNotBlank()) { "LocationId cannot be blank." }
    }
}

@JvmInline
value class ReportId(val value: String) {
    init {
        require(value.isNotBlank()) { "ReportId cannot be blank." }
    }
}

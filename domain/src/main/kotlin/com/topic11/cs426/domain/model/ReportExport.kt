package com.topic11.cs426.domain.model

data class ReportCandidate(
    val inspectionId: InspectionId,
    val title: String,
    val status: InspectionStatus,
    val completedItems: Int,
    val totalItems: Int,
) {
    init {
        require(title.isNotBlank()) { "Report candidate title cannot be blank." }
    }
}

enum class ReportFormat {
    JSON,
    PDF,
}

data class ReportExportArtifact(
    val storageKey: String,
    val displayFilename: String,
    val mimeType: String,
    val sizeBytes: Long,
) {
    init {
        require(storageKey.isNotBlank()) { "Report storage key cannot be blank." }
        require(displayFilename.isNotBlank()) { "Report filename cannot be blank." }
        require(mimeType.isNotBlank()) { "Report MIME type cannot be blank." }
        require(sizeBytes >= 0L) { "Report size cannot be negative." }
    }
}

data class ReportHistoryEntry(
    val id: ReportId,
    val inspectionId: InspectionId,
    val format: ReportFormat,
    val generatedAtMillis: Long,
    val displayFilename: String,
    val storageKey: String,
    val mimeType: String,
    val sizeBytes: Long,
) {
    init {
        require(displayFilename.isNotBlank()) { "Report history filename cannot be blank." }
        require(storageKey.isNotBlank()) { "Report history storage key cannot be blank." }
        require(mimeType.isNotBlank()) { "Report history MIME type cannot be blank." }
        require(sizeBytes >= 0L) { "Report history size cannot be negative." }
    }
}

sealed interface ReportGenerationResult {
    data class Success(val report: InspectionReport) : ReportGenerationResult

    data class Failed(val error: ReportGenerationError) : ReportGenerationResult
}

sealed interface ReportGenerationError {
    data object InspectionMissing : ReportGenerationError

    data class NotEligible(val status: InspectionStatus) : ReportGenerationError

    data object TemplateMissing : ReportGenerationError

    data object IssueLookupFailed : ReportGenerationError
}

sealed interface ReportExportResult {
    data class Success(val entry: ReportHistoryEntry) : ReportExportResult

    data class Failed(
        val error: ReportExportError,
        val generationError: ReportGenerationError? = null,
    ) : ReportExportResult
}

enum class ReportExportError {
    REPORT_GENERATION_FAILED,
    EXPORT_FAILED,
    HISTORY_PERSISTENCE_FAILED,
}

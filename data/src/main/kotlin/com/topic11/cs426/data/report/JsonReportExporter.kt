package com.topic11.cs426.data.report

import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.ReportExportArtifact
import java.io.File

class JsonReportExporter(
    private val fileStorage: ReportFileStorage,
) {
    suspend fun export(report: InspectionReport): ReportExportArtifact {
        val displayFilename = report.displayFilename("json")
        val stored = fileStorage.write(displayFilename) { target ->
            target.writeText(report.toJson(), Charsets.UTF_8)
        }
        return ReportExportArtifact(
            storageKey = stored.storageKey,
            displayFilename = stored.displayFilename,
            mimeType = "application/json",
            sizeBytes = stored.sizeBytes,
        )
    }
}

internal fun InspectionReport.toJson(): String = buildString {
    append("{\n")
    appendJsonField("reportId", id.value, comma = true, indent = 1)
    appendJsonField("inspectionId", inspectionId.value, comma = true, indent = 1)
    appendJsonField("assetName", assetName, comma = true, indent = 1)
    appendJsonField("templateName", templateName, comma = true, indent = 1)
    appendJsonField("summary", summary, comma = true, indent = 1)
    appendJsonField("completedAtMillis", completedAtMillis, comma = true, indent = 1)
    appendJsonField("generatedAtMillis", generatedAtMillis, comma = true, indent = 1)
    appendScore(this@toJson)
    append(",\n")
    appendSections(this@toJson)
    append(",\n")
    appendIssues(issues)
    append("\n}\n")
}

private fun StringBuilder.appendScore(report: InspectionReport) {
    appendIndent(1)
    append("\"score\": {\n")
    appendJsonField("earnedWeight", report.score.earnedWeight, comma = true, indent = 2)
    appendJsonField("totalWeight", report.score.totalWeight, comma = false, indent = 2)
    append('\n')
    appendIndent(1)
    append('}')
}

private fun StringBuilder.appendSections(report: InspectionReport) {
    appendIndent(1)
    append("\"sections\": [")
    if (report.sections.isNotEmpty()) append('\n')
    report.sections.forEachIndexed { sectionIndex, section ->
        appendIndent(2)
        append("{\n")
        appendJsonField("id", section.id.value, comma = true, indent = 3)
        appendJsonField("title", section.title, comma = true, indent = 3)
        appendIndent(3)
        append("\"items\": [")
        if (section.items.isNotEmpty()) append('\n')
        section.items.forEachIndexed { itemIndex, item ->
            appendIndent(4)
            append("{\n")
            appendJsonField("id", item.id.value, comma = true, indent = 5)
            appendJsonField("title", item.title, comma = true, indent = 5)
            appendJsonField("required", item.required, comma = true, indent = 5)
            appendJsonField("critical", item.critical, comma = true, indent = 5)
            appendJsonField("weight", item.weight, comma = true, indent = 5)
            appendJsonField("answer", item.answer?.toExportValue(), comma = true, indent = 5)
            appendJsonField("note", item.note, comma = true, indent = 5)
            appendIndent(5)
            append("\"evidenceIds\": [")
            append(item.evidenceIds.joinToString(", ") { id -> "\"${jsonEscape(id.value)}\"" })
            append("]\n")
            appendIndent(4)
            append('}')
            if (itemIndex < section.items.lastIndex) append(',')
            append('\n')
        }
        appendIndent(3)
        append("]\n")
        appendIndent(2)
        append('}')
        if (sectionIndex < report.sections.lastIndex) append(',')
        append('\n')
    }
    appendIndent(1)
    append(']')
}

private fun StringBuilder.appendIssues(issues: List<MaintenanceIssue>) {
    appendIndent(1)
    append("\"issues\": [")
    if (issues.isNotEmpty()) append('\n')
    issues.forEachIndexed { index, issue ->
        appendIndent(2)
        append("{\n")
        appendJsonField("id", issue.id.value, comma = true, indent = 3)
        appendJsonField("title", issue.title, comma = true, indent = 3)
        appendJsonField("description", issue.description, comma = true, indent = 3)
        appendJsonField("severity", issue.severity.name, comma = true, indent = 3)
        appendJsonField("status", issue.status.name, comma = true, indent = 3)
        appendJsonField("assetId", issue.assetId.value, comma = true, indent = 3)
        appendJsonField("inspectionId", issue.inspectionId.value, comma = true, indent = 3)
        appendJsonField("checklistItemId", issue.checklistItemId?.value, comma = true, indent = 3)
        appendJsonField("createdAtMillis", issue.createdAtMillis, comma = true, indent = 3)
        appendJsonField("updatedAtMillis", issue.updatedAtMillis, comma = false, indent = 3)
        append('\n')
        appendIndent(2)
        append('}')
        if (index < issues.lastIndex) append(',')
        append('\n')
    }
    appendIndent(1)
    append(']')
}

private fun StringBuilder.appendJsonField(
    name: String,
    value: Any?,
    comma: Boolean,
    indent: Int,
) {
    appendIndent(indent)
    append('"')
    append(jsonEscape(name))
    append("\": ")
    when (value) {
        null -> append("null")
        is String -> {
            append('"')
            append(jsonEscape(value))
            append('"')
        }
        is Number,
        is Boolean,
        -> append(value)
        else -> {
            append('"')
            append(jsonEscape(value.toString()))
            append('"')
        }
    }
    if (comma) append(',')
    append('\n')
}

private fun StringBuilder.appendIndent(count: Int) {
    repeat(count) { append("  ") }
}

private fun ChecklistAnswerValue.toExportValue(): String = when (this) {
    ChecklistAnswerValue.Pass -> "PASS"
    ChecklistAnswerValue.Fail -> "FAIL"
    ChecklistAnswerValue.NotApplicable -> "NOT_APPLICABLE"
    is ChecklistAnswerValue.YesNo -> value.toString()
    is ChecklistAnswerValue.Text -> value
    is ChecklistAnswerValue.NumberValue -> buildString {
        append(value)
        if (!unit.isNullOrBlank()) {
            append(' ')
            append(unit)
        }
    }
    is ChecklistAnswerValue.SingleChoice -> optionId
}

internal fun InspectionReport.displayFilename(extension: String): String {
    val inspection = inspectionId.value.toSafeFilenameToken()
    val report = id.value.toSafeFilenameToken()
    return "fieldflow-$inspection-$report.$extension"
}

private fun String.toSafeFilenameToken(): String =
    replace(Regex("[^A-Za-z0-9._-]"), "-")

private fun jsonEscape(value: String): String = buildString {
    for (character in value) {
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (character.code < 0x20) {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
    }
}

internal fun readReportText(file: File): String = file.readText(Charsets.UTF_8)

package com.topic11.cs426.data.report

import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.ReportExportArtifact
import com.topic11.cs426.domain.usecase.toReportLabel

class PdfReportExporter(
    private val fileStorage: ReportFileStorage,
) {
    suspend fun export(report: InspectionReport): ReportExportArtifact {
        val displayFilename = report.displayFilename("pdf")
        val stored = fileStorage.write(displayFilename) { target ->
            target.writeBytes(SimplePdfReportWriter().write(report))
        }
        return ReportExportArtifact(
            storageKey = stored.storageKey,
            displayFilename = stored.displayFilename,
            mimeType = "application/pdf",
            sizeBytes = stored.sizeBytes,
        )
    }
}

private class SimplePdfReportWriter {
    fun write(report: InspectionReport): ByteArray {
        val lines = buildReportLines(report)
        val pages = paginate(lines)
        return PdfDocumentBuilder(pages).build()
    }

    private fun buildReportLines(report: InspectionReport): List<PdfLine> {
        val lines = mutableListOf<PdfLine>()
        lines += PdfLine("FieldFlow Inspection Report", Font.Bold, 18, 26)
        lines += PdfLine(report.assetName, Font.Bold, 16, 22)
        lines += wrapped(report.summary, Font.Regular, 11, 15)
        lines += PdfLine("", Font.Regular, 11, 8)
        lines += PdfLine("Inspection ID: ${report.inspectionId.value}", Font.Regular, 11, 15)
        lines += PdfLine("Template: ${report.templateName}", Font.Regular, 11, 15)
        lines += PdfLine("Completed: ${report.completedAtMillis}", Font.Regular, 11, 15)
        lines += PdfLine("Generated: ${report.generatedAtMillis}", Font.Regular, 11, 15)
        lines += PdfLine("Score: ${report.score.earnedWeight}/${report.score.totalWeight}", Font.Regular, 11, 15)
        lines += PdfLine("", Font.Regular, 11, 10)
        lines += PdfLine("Checklist", Font.Bold, 14, 20)
        report.sections.forEach { section ->
            lines += PdfLine(section.title, Font.Bold, 12, 18)
            section.items.forEach { item ->
                val badges = buildList {
                    if (item.required) add("required")
                    if (item.critical) add("critical")
                    add("weight ${item.weight}")
                }.joinToString(", ")
                lines += wrapped("${item.title} ($badges)", Font.Regular, 11, 15)
                lines += wrapped("Answer: ${item.answer?.toReportLabel() ?: "No answer"}", Font.Regular, 11, 15)
                if (!item.note.isNullOrBlank()) {
                    lines += wrapped("Note: ${item.note}", Font.Regular, 10, 14)
                }
                if (item.evidenceIds.isNotEmpty()) {
                    lines += wrapped(
                        "Evidence: ${item.evidenceIds.joinToString { it.value }}",
                        Font.Regular,
                        10,
                        14,
                    )
                }
                lines += PdfLine("", Font.Regular, 11, 6)
            }
        }
        lines += PdfLine("", Font.Regular, 11, 10)
        lines += PdfLine("Issues", Font.Bold, 14, 20)
        if (report.issues.isEmpty()) {
            lines += wrapped("No maintenance issues were recorded for this inspection.", Font.Regular, 11, 15)
        } else {
            report.issues.forEach { issue ->
                lines += wrapped("${issue.title} - ${issue.status.name}", Font.Bold, 11, 16)
                lines += PdfLine("Severity: ${issue.severity.name}", Font.Regular, 11, 15)
                val description = issue.description
                if (!description.isNullOrBlank()) {
                    lines += wrapped(description, Font.Regular, 10, 14)
                }
                lines += PdfLine("Created: ${issue.createdAtMillis}", Font.Regular, 10, 14)
                lines += PdfLine("Updated: ${issue.updatedAtMillis}", Font.Regular, 10, 14)
                lines += PdfLine("", Font.Regular, 11, 6)
            }
        }
        return lines
    }

    private fun wrapped(
        text: String,
        font: Font,
        size: Int,
        leading: Int,
    ): List<PdfLine> =
        wrapText(text, maxChars = if (size >= 14) 58 else 88)
            .map { line -> PdfLine(line, font, size, leading) }

    private fun wrapText(
        text: String,
        maxChars: Int,
    ): List<String> {
        if (text.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        text.lines().forEach { rawLine ->
            val words = rawLine.split(Regex("\\s+")).filter { it.isNotBlank() }
            var current = ""
            words.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (candidate.length <= maxChars) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) result += current
                    current = word.chunked(maxChars).also { chunks ->
                        if (chunks.size > 1) {
                            result += chunks.dropLast(1)
                        }
                    }.last()
                }
            }
            if (current.isNotEmpty()) result += current
        }
        return result
    }

    private fun paginate(lines: List<PdfLine>): List<List<PdfLine>> {
        val pages = mutableListOf<MutableList<PdfLine>>(mutableListOf())
        var y = TOP_Y
        lines.forEach { line ->
            if (y - line.leading < BOTTOM_Y) {
                pages += mutableListOf<PdfLine>()
                y = TOP_Y
            }
            pages.last() += line
            y -= line.leading
        }
        return pages
    }

    private companion object {
        const val TOP_Y = 744
        const val BOTTOM_Y = 54
    }
}

private class PdfDocumentBuilder(
    private val pages: List<List<PdfLine>>,
) {
    fun build(): ByteArray {
        val objects = mutableListOf<String>()
        val pageObjectIds = pages.indices.map { index -> 5 + index * 2 }
        val contentObjectIds = pages.indices.map { index -> 6 + index * 2 }
        val pageKids = pageObjectIds.joinToString(" ") { "$it 0 R" }

        objects += "<< /Type /Catalog /Pages 2 0 R >>"
        objects += "<< /Type /Pages /Kids [$pageKids] /Count ${pages.size} >>"
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"

        pages.forEachIndexed { index, lines ->
            val content = pageContent(lines, pageNumber = index + 1)
            objects += "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> " +
                "/Contents ${contentObjectIds[index]} 0 R >>"
            objects += "<< /Length ${content.toByteArray(Charsets.US_ASCII).size} >>\nstream\n$content\nendstream"
        }

        return render(objects)
    }

    private fun pageContent(
        lines: List<PdfLine>,
        pageNumber: Int,
    ): String = buildString {
        append("BT\n")
        var y = 744
        lines.forEach { line ->
            if (line.text.isBlank()) {
                y -= line.leading
            } else {
                val fontName = if (line.font == Font.Bold) "F2" else "F1"
                append("/$fontName ${line.size} Tf\n")
                append("1 0 0 1 48 $y Tm (${line.text.pdfEscape()}) Tj\n")
                y -= line.leading
            }
        }
        append("/F1 9 Tf\n")
        append("1 0 0 1 522 28 Tm (Page $pageNumber) Tj\n")
        append("ET")
    }

    private fun render(objects: List<String>): ByteArray {
        val output = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEachIndexed { index, body ->
            offsets += output.toString().toByteArray(Charsets.US_ASCII).size
            output.append("${index + 1} 0 obj\n")
            output.append(body)
            output.append("\nendobj\n")
        }
        val xrefOffset = output.toString().toByteArray(Charsets.US_ASCII).size
        output.append("xref\n")
        output.append("0 ${objects.size + 1}\n")
        output.append("0000000000 65535 f \n")
        offsets.forEach { offset ->
            output.append(offset.toString().padStart(10, '0'))
            output.append(" 00000 n \n")
        }
        output.append("trailer\n")
        output.append("<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        output.append("startxref\n")
        output.append(xrefOffset)
        output.append("\n%%EOF\n")
        return output.toString().toByteArray(Charsets.US_ASCII)
    }
}

private data class PdfLine(
    val text: String,
    val font: Font,
    val size: Int,
    val leading: Int,
)

private enum class Font {
    Regular,
    Bold,
}

private fun String.pdfEscape(): String =
    map { character ->
        when (character) {
            '(' -> "\\("
            ')' -> "\\)"
            '\\' -> "\\\\"
            '\n',
            '\r',
            '\t',
            -> " "
            else -> if (character.code in 32..126) character.toString() else "?"
        }
    }.joinToString("")

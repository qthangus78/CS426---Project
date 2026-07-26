package com.topic11.cs426.data.report

import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.InspectionReportItem
import com.topic11.cs426.domain.model.InspectionReportSection
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportId
import com.topic11.cs426.domain.model.SectionId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReportExporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `json exporter writes deterministic escaped report content`() = runTest {
        val storage = ReportFileStorage(temporaryFolder.root)
        val exporter = JsonReportExporter(storage)

        val artifact = exporter.export(report())
        val json = storage.resolve(artifact.storageKey).readText(Charsets.UTF_8)

        assertEquals("application/json", artifact.mimeType)
        assertTrue(json.startsWith("{\n"))
        assertTrue(json.contains(""""assetName": "Computer Lab \"I.44\"""""))
        assertTrue(json.contains(""""answer": "Needs cable\nreplacement""""))
        assertTrue(json.contains(""""title": "Critical failure: Fire extinguisher""""))
        assertTrue(json.contains(""""evidenceIds": ["evidence-photo"]"""))
    }

    @Test
    fun `pdf exporter writes non empty pdf file`() = runTest {
        val storage = ReportFileStorage(temporaryFolder.root)
        val exporter = PdfReportExporter(storage)

        val artifact = exporter.export(report())
        val bytes = storage.resolve(artifact.storageKey).readBytes()

        assertEquals("application/pdf", artifact.mimeType)
        assertTrue(bytes.size > 100)
        assertEquals("%PDF", bytes.decodeToString(endIndex = 4))
    }

    @Test
    fun `format dispatcher routes to requested exporter`() = runTest {
        val storage = ReportFileStorage(temporaryFolder.root)
        val exporter = FieldFlowReportExporter(
            jsonReportExporter = JsonReportExporter(storage),
            pdfReportExporter = PdfReportExporter(storage),
        )

        val json = exporter.export(report(ReportId("report-json")), ReportFormat.JSON)
        val pdf = exporter.export(report(ReportId("report-pdf")), ReportFormat.PDF)

        assertTrue(json.displayFilename.endsWith(".json"))
        assertTrue(pdf.displayFilename.endsWith(".pdf"))
    }

    @Test
    fun `failed storage write cleans partial report file`() = runTest {
        val rootFile = temporaryFolder.newFile("not-a-directory")
        val storage = ReportFileStorage(rootFile)
        val exporter = JsonReportExporter(storage)

        val failure = runCatching { exporter.export(report()) }.exceptionOrNull()

        assertTrue(failure != null)
        assertFalse(rootFile.resolveSibling("not-a-directory/reports").exists())
    }

    private fun report(reportId: ReportId = ReportId("report-1")) = InspectionReport(
        id = reportId,
        inspectionId = InspectionId("inspection-1"),
        assetName = "Computer Lab \"I.44\"",
        templateName = "Lab Safety",
        summary = "Inspection report for Computer Lab \"I.44\"",
        score = InspectionScore(earnedWeight = 6, totalWeight = 10),
        completedAtMillis = 2_000L,
        generatedAtMillis = 3_000L,
        sections = listOf(
            InspectionReportSection(
                id = SectionId("section-safety"),
                title = "Safety",
                items = listOf(
                    InspectionReportItem(
                        id = ChecklistItemId("item-fire"),
                        title = "Fire extinguisher",
                        required = true,
                        critical = true,
                        weight = 5,
                        answer = ChecklistAnswerValue.Text("Needs cable\nreplacement"),
                        note = "Escaped note",
                        evidenceIds = listOf(EvidenceId("evidence-photo")),
                    ),
                ),
            ),
        ),
        issues = listOf(
            MaintenanceIssue(
                id = IssueId("issue-1"),
                inspectionId = InspectionId("inspection-1"),
                assetId = AssetId("asset-1"),
                checklistItemId = ChecklistItemId("item-fire"),
                severity = IssueSeverity.CRITICAL,
                title = "Critical failure: Fire extinguisher",
                description = "Missing at rear door",
                status = MaintenanceIssueStatus.OPEN,
                createdAtMillis = 2_000L,
                updatedAtMillis = 2_500L,
            ),
        ),
    )
}

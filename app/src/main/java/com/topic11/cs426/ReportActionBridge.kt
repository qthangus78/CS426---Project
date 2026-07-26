package com.topic11.cs426

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.topic11.cs426.data.report.ReportFileStorage
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.feature.reports.ReportActionHandler
import com.topic11.cs426.feature.reports.ReportActionResult

internal class AndroidReportActionHandler(
    private val context: Context,
    private val fileStorage: ReportFileStorage,
) : ReportActionHandler {
    override fun open(entry: ReportHistoryEntry): ReportActionResult =
        startReportIntent(entry) { uri ->
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, entry.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    override fun share(entry: ReportHistoryEntry): ReportActionResult =
        startReportIntent(entry) { uri ->
            val sendIntent = Intent(Intent.ACTION_SEND)
                .setType(entry.mimeType)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Intent.createChooser(sendIntent, "Share report")
        }

    private fun startReportIntent(
        entry: ReportHistoryEntry,
        createIntent: (android.net.Uri) -> Intent,
    ): ReportActionResult {
        val file = try {
            fileStorage.resolve(entry.storageKey)
        } catch (failure: IllegalArgumentException) {
            return ReportActionResult.Failed("Report file is no longer available.")
        }
        if (!file.isFile) {
            return ReportActionResult.Failed("Report file is no longer available.")
        }

        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.report-files",
                file,
            )
        } catch (failure: IllegalArgumentException) {
            return ReportActionResult.Failed("Report file could not be opened.")
        }
        val intent = createIntent(uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            ReportActionResult.Started
        } catch (failure: ActivityNotFoundException) {
            ReportActionResult.Failed("No app is available for this report.")
        } catch (failure: SecurityException) {
            ReportActionResult.Failed("Report file could not be opened.")
        }
    }
}

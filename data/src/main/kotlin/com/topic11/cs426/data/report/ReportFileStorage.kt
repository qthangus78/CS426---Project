package com.topic11.cs426.data.report

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportFileStorage(
    rootDirectory: File,
) {
    private val reportDirectory = File(rootDirectory, REPORT_DIRECTORY)

    suspend fun write(
        displayFilename: String,
        writeFile: (File) -> Unit,
    ): StoredReportFile = withContext(Dispatchers.IO) {
        require(FILE_NAME_PATTERN.matches(displayFilename)) {
            "Report filename contains unsupported characters."
        }
        check(reportDirectory.exists() || reportDirectory.mkdirs()) {
            "Cannot create report directory."
        }

        val storageKey = "$REPORT_DIRECTORY/$displayFilename"
        val target = resolve(storageKey)
        check(!target.exists()) {
            "Report already exists: $displayFilename"
        }
        val temporary = File.createTempFile(displayFilename.substringBeforeLast('.'), ".tmp", reportDirectory)

        try {
            writeFile(temporary)
            check(temporary.renameTo(target)) {
                "Cannot move report into managed storage."
            }
            StoredReportFile(
                storageKey = storageKey,
                displayFilename = displayFilename,
                sizeBytes = target.length(),
            )
        } catch (failure: Throwable) {
            temporary.delete()
            target.delete()
            throw failure
        }
    }

    fun resolve(storageKey: String): File {
        require(storageKey.startsWith("$REPORT_DIRECTORY/")) {
            "Storage key is outside report storage."
        }
        val file = File(reportDirectory.parentFile, storageKey).canonicalFile
        val managedDirectory = reportDirectory.canonicalFile
        require(file.parentFile == managedDirectory) {
            "Storage key escapes report storage."
        }
        return file
    }

    private companion object {
        const val REPORT_DIRECTORY = "reports"
        val FILE_NAME_PATTERN = Regex("[A-Za-z0-9._-]+")
    }
}

data class StoredReportFile(
    val storageKey: String,
    val displayFilename: String,
    val sizeBytes: Long,
)

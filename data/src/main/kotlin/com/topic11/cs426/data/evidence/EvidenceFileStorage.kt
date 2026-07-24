package com.topic11.cs426.data.evidence

import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EvidenceFileStorage(
    rootDirectory: File,
) {
    private val evidenceDirectory = File(rootDirectory, EVIDENCE_DIRECTORY)

    suspend fun persist(
        evidenceId: String,
        source: InputStream,
    ): StoredEvidenceFile = withContext(Dispatchers.IO) {
        require(EVIDENCE_ID_PATTERN.matches(evidenceId)) {
            "Evidence ID contains unsupported characters."
        }
        check(evidenceDirectory.exists() || evidenceDirectory.mkdirs()) {
            "Cannot create evidence directory."
        }

        val target = resolve("$EVIDENCE_DIRECTORY/$evidenceId")
        check(!target.exists()) { "Evidence already exists: $evidenceId" }
        val temporary = File.createTempFile("$evidenceId-", ".tmp", evidenceDirectory)

        try {
            temporary.outputStream().buffered().use { output ->
                source.copyTo(output)
            }
            check(temporary.renameTo(target)) {
                "Cannot move evidence into managed storage."
            }
            StoredEvidenceFile(
                storageKey = "$EVIDENCE_DIRECTORY/$evidenceId",
                sizeBytes = target.length(),
            )
        } catch (failure: Throwable) {
            temporary.delete()
            target.delete()
            throw failure
        }
    }

    suspend fun delete(storageKey: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolve(storageKey)
        !file.exists() || file.delete()
    }

    suspend fun exists(storageKey: String): Boolean = withContext(Dispatchers.IO) {
        resolve(storageKey).isFile
    }

    private fun resolve(storageKey: String): File {
        require(storageKey.startsWith("$EVIDENCE_DIRECTORY/")) {
            "Storage key is outside evidence storage."
        }
        val file = File(evidenceDirectory.parentFile, storageKey).canonicalFile
        val managedDirectory = evidenceDirectory.canonicalFile
        require(file.parentFile == managedDirectory) {
            "Storage key escapes evidence storage."
        }
        return file
    }

    private companion object {
        const val EVIDENCE_DIRECTORY = "evidence"
        val EVIDENCE_ID_PATTERN = Regex("[A-Za-z0-9._-]+")
    }
}

data class StoredEvidenceFile(
    val storageKey: String,
    val sizeBytes: Long,
)

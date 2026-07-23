package com.topic11.cs426.data.evidence

import android.content.Context
import android.net.Uri
import com.topic11.cs426.core.database.dao.InspectionDao
import com.topic11.cs426.core.database.entity.EvidenceEntity
import java.io.InputStream

class AndroidEvidenceStore internal constructor(
    private val openSource: (Uri) -> InputStream?,
    private val fileStorage: EvidenceFileStorage,
    private val persistMetadata: suspend (EvidenceEntity) -> Unit,
) {
    constructor(
        context: Context,
        fileStorage: EvidenceFileStorage,
        inspectionDao: InspectionDao,
    ) : this(
        openSource = { uri -> context.contentResolver.openInputStream(uri) },
        fileStorage = fileStorage,
        persistMetadata = { evidence -> inspectionDao.upsertEvidence(listOf(evidence)) },
    )

    suspend fun persist(request: EvidencePersistRequest): EvidenceEntity {
        val storedFile = openSource(request.sourceUri)?.use { source ->
            fileStorage.persist(request.evidenceId, source)
        } ?: throw EvidenceSourceUnavailableException(request.sourceUri)

        val evidence = EvidenceEntity(
            id = request.evidenceId,
            inspectionId = request.inspectionId,
            checklistItemId = request.checklistItemId,
            storageKey = storedFile.storageKey,
            mimeType = request.mimeType,
            createdAtMillis = request.createdAtMillis,
        )

        try {
            persistMetadata(evidence)
        } catch (failure: Throwable) {
            fileStorage.delete(storedFile.storageKey)
            throw failure
        }
        return evidence
    }
}

data class EvidencePersistRequest(
    val evidenceId: String,
    val inspectionId: String,
    val checklistItemId: String?,
    val sourceUri: Uri,
    val mimeType: String?,
    val createdAtMillis: Long,
)

class EvidenceSourceUnavailableException(
    sourceUri: Uri,
) : IllegalArgumentException("Cannot open evidence source: $sourceUri")

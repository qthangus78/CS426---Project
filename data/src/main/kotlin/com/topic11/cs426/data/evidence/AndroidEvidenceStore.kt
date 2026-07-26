package com.topic11.cs426.data.evidence

import android.content.Context
import android.net.Uri
import com.topic11.cs426.core.database.dao.InspectionDao
import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.EvidenceReference
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.repository.EvidenceSource
import com.topic11.cs426.domain.repository.EvidenceStore
import java.io.InputStream
import java.util.UUID

class AndroidEvidenceStore internal constructor(
    private val openSource: (Uri) -> InputStream?,
    private val fileStorage: EvidenceFileStorage,
    private val persistMetadata: suspend (EvidenceEntity) -> Unit,
    private val findMetadata: suspend (String) -> EvidenceEntity? = { null },
    private val deleteMetadata: suspend (String) -> Unit = {},
    private val evidenceIdFactory: () -> String = { "evidence-${UUID.randomUUID()}" },
    private val clock: () -> Long = System::currentTimeMillis,
) : EvidenceStore {
    constructor(
        context: Context,
        fileStorage: EvidenceFileStorage,
        inspectionDao: InspectionDao,
    ) : this(
        openSource = { uri -> context.contentResolver.openInputStream(uri) },
        fileStorage = fileStorage,
        persistMetadata = { evidence -> inspectionDao.upsertEvidence(listOf(evidence)) },
        findMetadata = inspectionDao::getEvidenceById,
        deleteMetadata = { evidenceId -> inspectionDao.deleteEvidence(evidenceId) },
    )

    override suspend fun persist(source: EvidenceSource): EvidenceReference {
        val evidenceId = evidenceIdFactory()
        val entity = persist(
            EvidencePersistRequest(
                evidenceId = evidenceId,
                inspectionId = source.inspectionId,
                checklistItemId = source.checklistItemId,
                sourceUri = Uri.parse(source.uriString),
                mimeType = source.mimeType,
                createdAtMillis = clock(),
            ),
        )
        return entity.toDomainReference()
    }

    override suspend fun delete(reference: EvidenceReference) {
        val entity = findMetadata(reference.id.value) ?: return
        fileStorage.delete(entity.storageKey)
        deleteMetadata(entity.id)
    }

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

private fun EvidenceEntity.toDomainReference(): EvidenceReference {
    val itemId = checkNotNull(checklistItemId) {
        "Domain evidence must belong to a checklist item: $id"
    }
    return EvidenceReference(
        id = EvidenceId(id),
        inspectionId = InspectionId(inspectionId),
        checklistItemId = ChecklistItemId(itemId),
        uriString = "fieldflow-evidence://managed/$id",
        mimeType = mimeType,
        createdAtMillis = createdAtMillis,
    )
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

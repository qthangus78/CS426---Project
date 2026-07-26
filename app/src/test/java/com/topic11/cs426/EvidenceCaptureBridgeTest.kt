package com.topic11.cs426

import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.EvidenceReference
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.repository.EvidenceSource
import com.topic11.cs426.domain.repository.EvidenceStore
import com.topic11.cs426.feature.inspection.InspectionEvidenceCaptureRequest
import com.topic11.cs426.feature.inspection.InspectionEvidenceCaptureSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvidenceCaptureBridgeTest {
    @Test
    fun `persist selection stores picker source and returns captured reference`() = runBlocking {
        val store = RecordingEvidenceStore()
        var captured: EvidenceReference? = null
        var failure: String? = null
        val request = InspectionEvidenceCaptureRequest(
            inspectionId = "inspection-created",
            itemId = "item-power",
            source = InspectionEvidenceCaptureSource.Gallery,
            onCaptured = { reference -> captured = reference },
            onFailure = { message -> failure = message },
        )

        val result = persistEvidenceSelection(
            evidenceStore = store,
            request = request,
            uriString = "content://fieldflow/gallery/photo",
            mimeType = "image/png",
        )

        assertEquals(store.reference, result)
        assertEquals(store.reference, captured)
        assertNull(failure)
        assertEquals(
            EvidenceSource(
                inspectionId = InspectionId("inspection-created"),
                checklistItemId = ChecklistItemId("item-power"),
                sourceReference = "content://fieldflow/gallery/photo",
                mimeType = "image/png",
            ),
            store.source,
        )
    }

    private class RecordingEvidenceStore : EvidenceStore {
        val reference = EvidenceReference(
            id = EvidenceId("evidence-managed"),
            inspectionId = InspectionId("inspection-created"),
            checklistItemId = ChecklistItemId("item-power"),
            storageKey = "evidence/evidence-managed",
            mimeType = "image/png",
            createdAtMillis = 3_000L,
        )
        var source: EvidenceSource? = null

        override suspend fun persist(source: EvidenceSource): EvidenceReference {
            this.source = source
            return reference
        }

        override suspend fun delete(reference: EvidenceReference) = Unit
    }
}

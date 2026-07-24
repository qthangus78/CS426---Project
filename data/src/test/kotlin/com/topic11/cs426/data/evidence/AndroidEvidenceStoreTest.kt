package com.topic11.cs426.data.evidence

import android.net.Uri
import com.topic11.cs426.core.database.entity.EvidenceEntity
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidEvidenceStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `persist copies source into managed storage and writes metadata`() = runTest {
        val fileStorage = EvidenceFileStorage(temporaryFolder.root)
        val savedMetadata = mutableListOf<EvidenceEntity>()
        val store = store(
            fileStorage = fileStorage,
            bytes = "photo-content".encodeToByteArray(),
            persistMetadata = savedMetadata::add,
        )

        val result = store.persist(request())

        assertEquals("evidence/evidence-photo", result.storageKey)
        assertEquals("image/jpeg", result.mimeType)
        assertEquals(listOf(result), savedMetadata)
        assertTrue(fileStorage.exists(result.storageKey))
        assertEquals(
            "photo-content",
            temporaryFolder.root
                .resolve(result.storageKey)
                .readText(),
        )
    }

    @Test
    fun `persist removes copied file when metadata write fails`() = runTest {
        val fileStorage = EvidenceFileStorage(temporaryFolder.root)
        val store = store(
            fileStorage = fileStorage,
            bytes = "photo-content".encodeToByteArray(),
            persistMetadata = { error("database unavailable") },
        )

        assertThrows(IllegalStateException::class.java) {
            runTest { store.persist(request()) }
        }

        assertFalse(fileStorage.exists("evidence/evidence-photo"))
    }

    @Test
    fun `persist rejects unavailable content source`() {
        val store = AndroidEvidenceStore(
            openSource = { null },
            fileStorage = EvidenceFileStorage(temporaryFolder.root),
            persistMetadata = {},
        )

        assertThrows(EvidenceSourceUnavailableException::class.java) {
            runTest { store.persist(request()) }
        }
    }

    @Test
    fun `file storage rejects evidence id that can escape managed directory`() {
        val fileStorage = EvidenceFileStorage(temporaryFolder.root)

        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                fileStorage.persist(
                    evidenceId = "../outside",
                    source = ByteArrayInputStream(byteArrayOf(1)),
                )
            }
        }

        assertFalse(temporaryFolder.root.resolve("outside").exists())
    }

    private fun store(
        fileStorage: EvidenceFileStorage,
        bytes: ByteArray,
        persistMetadata: suspend (EvidenceEntity) -> Unit,
    ) = AndroidEvidenceStore(
        openSource = { ByteArrayInputStream(bytes) },
        fileStorage = fileStorage,
        persistMetadata = persistMetadata,
    )

    private fun request() = EvidencePersistRequest(
        evidenceId = "evidence-photo",
        inspectionId = "inspection-lab-i44",
        checklistItemId = "item-extinguisher",
        sourceUri = Uri.parse("content://fieldflow/photo"),
        mimeType = "image/jpeg",
        createdAtMillis = 2_000L,
    )
}

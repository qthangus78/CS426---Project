package com.topic11.cs426

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.topic11.cs426.domain.model.EvidenceReference
import com.topic11.cs426.domain.repository.EvidenceSource
import com.topic11.cs426.domain.repository.EvidenceStore
import com.topic11.cs426.feature.inspection.InspectionEvidenceCaptureHandler
import com.topic11.cs426.feature.inspection.InspectionEvidenceCaptureRequest
import com.topic11.cs426.feature.inspection.InspectionEvidenceCaptureSource
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberEvidenceCaptureHandler(
    evidenceStore: EvidenceStore,
): InspectionEvidenceCaptureHandler {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingRequest by remember { mutableStateOf<InspectionEvidenceCaptureRequest?>(null) }

    fun persistSelectedEvidence(
        uri: Uri,
        mimeType: String?,
        deleteAfterPersist: Boolean,
    ) {
        val request = pendingRequest ?: return
        coroutineScope.launch {
            try {
                persistEvidenceSelection(
                    evidenceStore = evidenceStore,
                    request = request,
                    uriString = uri.toString(),
                    mimeType = mimeType,
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                request.onFailure("Couldn't attach evidence.")
            } finally {
                pendingRequest = null
                if (deleteAfterPersist) {
                    uri.deleteFileUri()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            persistSelectedEvidence(
                uri = uri,
                mimeType = context.contentResolver.getType(uri),
                deleteAfterPersist = false,
            )
        } else {
            pendingRequest = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                try {
                    val uri = writeCameraBitmapToCache(context, bitmap)
                    persistSelectedEvidence(
                        uri = uri,
                        mimeType = "image/jpeg",
                        deleteAfterPersist = true,
                    )
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    pendingRequest?.onFailure("Couldn't attach evidence.")
                    pendingRequest = null
                }
            }
        } else {
            pendingRequest = null
        }
    }

    return remember(galleryLauncher, cameraLauncher) {
        InspectionEvidenceCaptureHandler { request ->
            if (pendingRequest != null) {
                request.onFailure("Finish the current evidence capture first.")
                return@InspectionEvidenceCaptureHandler
            }
            pendingRequest = request
            when (request.source) {
                InspectionEvidenceCaptureSource.Camera -> cameraLauncher.launch(null)
                InspectionEvidenceCaptureSource.Gallery -> galleryLauncher.launch("image/*")
            }
        }
    }
}

internal suspend fun persistEvidenceSelection(
    evidenceStore: EvidenceStore,
    request: InspectionEvidenceCaptureRequest,
    uriString: String,
    mimeType: String?,
): EvidenceReference {
    val reference = evidenceStore.persist(
        EvidenceSource(
            inspectionId = request.inspectionId,
            checklistItemId = request.itemId,
            uriString = uriString,
            mimeType = mimeType,
        ),
    )
    request.onCaptured(reference)
    return reference
}

private suspend fun writeCameraBitmapToCache(
    context: Context,
    bitmap: Bitmap,
): Uri = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "evidence-capture")
    check(directory.exists() || directory.mkdirs()) {
        "Cannot create evidence capture cache."
    }
    val file = File.createTempFile("camera-", ".jpg", directory)
    file.outputStream().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
            "Cannot encode camera evidence."
        }
    }
    Uri.fromFile(file)
}

private fun Uri.deleteFileUri() {
    if (scheme == "file") {
        path?.let { path -> runCatching { File(path).delete() } }
    }
}

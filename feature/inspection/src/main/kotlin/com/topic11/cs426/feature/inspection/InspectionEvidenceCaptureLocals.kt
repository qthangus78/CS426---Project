package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.staticCompositionLocalOf

val LocalInspectionEvidenceCaptureHandler = staticCompositionLocalOf<InspectionEvidenceCaptureHandler> {
    InspectionEvidenceCaptureHandler { request ->
        request.onFailure("Evidence capture is not available.")
    }
}

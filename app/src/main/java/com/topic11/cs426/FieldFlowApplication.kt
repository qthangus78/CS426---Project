package com.topic11.cs426

import android.app.Application

/**
 * Owns the app-scoped object graph.
 *
 * The composition root holds the Room database and an application-scoped [kotlinx.coroutines.CoroutineScope],
 * so it must outlive Activity recreation. Creating it here means a configuration change no longer
 * closes and reopens the database or cancels in-flight draft saves.
 */
class FieldFlowApplication : Application() {
    val compositionRoot: FieldFlowCompositionRoot by lazy {
        FieldFlowCompositionRoot.create(applicationContext)
    }
}

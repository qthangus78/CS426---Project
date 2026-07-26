package com.topic11.cs426

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Startup sample-data seeding as the UI sees it.
 *
 * Seeding runs on the app scope, so the only thing the UI can do about a failure is report it and
 * offer a retry — every screen that reads Room stays empty until it succeeds.
 */
sealed interface SampleDataSeedingState {
    data object InProgress : SampleDataSeedingState

    data object Succeeded : SampleDataSeedingState

    data class Failed(val cause: Throwable) : SampleDataSeedingState
}

/**
 * Owns the startup seeding job and publishes its outcome as observable state.
 *
 * A plain [java.util.concurrent.atomic.AtomicReference] was enough while only tests read the
 * failure, but nothing recomposes when one is written — a failed seed left the dashboard silently
 * empty. The [StateFlow] is what makes the failure reach the screen.
 */
internal class SampleDataSeedingCoordinator(
    private val scope: CoroutineScope,
    private val seedSampleData: suspend () -> Unit,
) {
    private val mutableState =
        MutableStateFlow<SampleDataSeedingState>(SampleDataSeedingState.InProgress)
    val state: StateFlow<SampleDataSeedingState> = mutableState.asStateFlow()

    private var seedJob: Job? = null

    /**
     * Starts seeding, or returns the in-flight job when one is already running. Retrying is the same
     * call: a finished job — failed or succeeded — is replaced by a fresh attempt.
     */
    @Synchronized
    fun start(): Job {
        seedJob?.takeIf(Job::isActive)?.let { return it }

        mutableState.value = SampleDataSeedingState.InProgress
        return launchFieldFlowStartupSeeding(
            scope = scope,
            seedSampleData = seedSampleData,
            onFailure = { throwable -> mutableState.value = SampleDataSeedingState.Failed(throwable) },
            onSuccess = { mutableState.value = SampleDataSeedingState.Succeeded },
        ).also { seedJob = it }
    }
}

internal fun launchFieldFlowStartupSeeding(
    scope: CoroutineScope,
    seedSampleData: suspend () -> Unit,
    onFailure: (Throwable) -> Unit,
    onSuccess: () -> Unit = {},
): Job = scope.launch {
    try {
        seedSampleData()
        onSuccess()
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        onFailure(throwable)
    }
}

package com.topic11.cs426

import com.topic11.cs426.core.database.entity.PendingSyncEntity
import com.topic11.cs426.data.sync.FakeSyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * How many times a single command may be handed to the remote adapter before the drain loop gives up
 * on it. The command stays in the queue as `FAILED`, so nothing is lost — it just stops being retried
 * automatically.
 */
internal const val DEFAULT_MAX_SYNC_ATTEMPTS = 5

private const val BASE_BACKOFF_MILLIS = 1_000L
private const val MAX_BACKOFF_MILLIS = 60_000L

/** Exponential backoff, capped. [attempt] is 1-based: the attempt that just failed. */
internal fun pendingSyncBackoffMillis(attempt: Int): Long {
    require(attempt > 0) { "Sync attempt numbers must be positive." }

    // Shifting by more than 5 already exceeds the cap; bounding it keeps the shift away from Long
    // overflow for a command that somehow accumulated a large attempt count.
    val exponent = (attempt - 1).coerceAtMost(6)
    return minOf(MAX_BACKOFF_MILLIS, BASE_BACKOFF_MILLIS shl exponent)
}

/**
 * Drains the durable `pending_sync` queue by handing each retryable command to the remote adapter.
 *
 * Without this loop nothing ever consumed the queue: `RoomInspectionRepository.complete` enqueued a
 * `PENDING` command and the row stayed `PENDING` forever, so every completed inspection mapped to
 * `InspectionStatus.SYNC_PENDING` and the Dashboard "Sync pending" tile only ever counted up.
 *
 * Two things keep a permanently failing command from burning the CPU. `observeRetryableCommands`
 * re-emits as soon as a failure writes the row back to `FAILED`, so [awaitBackoff] runs *inside* the
 * collector: the re-emission is conflated away while we wait, which paces retries instead of
 * spinning. And [maxAttempts] eventually stops the command being claimed at all — once every
 * command in the snapshot is skipped, the pass writes nothing, Room raises no invalidation, and the
 * loop goes quiet.
 */
internal class PendingSyncDrain(
    private val retryableCommands: Flow<List<PendingSyncEntity>>,
    private val sync: suspend (commandId: String) -> FakeSyncResult,
    private val maxAttempts: Int = DEFAULT_MAX_SYNC_ATTEMPTS,
    private val backoffMillisFor: (attempt: Int) -> Long = ::pendingSyncBackoffMillis,
    private val awaitBackoff: suspend (Long) -> Unit = { delay(it) },
    private val onDrainFailure: (Throwable) -> Unit = {},
) {
    init {
        require(maxAttempts > 0) { "A sync command must be allowed at least one attempt." }
    }

    /** Collects the queue until the calling scope is cancelled. Never returns normally. */
    suspend fun run() {
        retryableCommands.conflate().collect(::drain)
    }

    private suspend fun drain(commands: List<PendingSyncEntity>) {
        var backoffMillis = 0L

        for (command in commands) {
            // The claim inside the adapter is what increments attempt_count, so a command that has
            // already spent its budget has to be filtered out here rather than re-claimed.
            if (command.attemptCount >= maxAttempts) continue

            val failedAttempt = command.attemptCount + 1
            val failed = try {
                claimAndSync(command.id) is FakeSyncResult.Failed
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                // A throw here means the row moved under us. Reporting and carrying on beats letting
                // one bad command tear down the loop for every other queued inspection.
                onDrainFailure(throwable)
                true
            }

            if (failed) {
                backoffMillis = maxOf(backoffMillis, backoffMillisFor(failedAttempt))
            }
        }

        if (backoffMillis > 0L) {
            awaitBackoff(backoffMillis)
        }
    }

    /**
     * A sync is two writes — claim, then settle as `SYNCED`/`FAILED`. Cancelling between them parks
     * the command in `SYNCING`, which `observeRetryableCommands` never returns, so the inspection
     * would sit on "Sync pending" with nothing left to retry it. Closing the graph cancels the app
     * scope, which makes that window reachable in normal use, hence the critical section.
     */
    private suspend fun claimAndSync(commandId: String): FakeSyncResult =
        withContext(NonCancellable) { sync(commandId) }
}

internal fun launchFieldFlowPendingSyncDrain(
    scope: CoroutineScope,
    drain: PendingSyncDrain,
): Job = scope.launch { drain.run() }

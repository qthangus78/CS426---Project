package com.topic11.cs426

import com.topic11.cs426.core.database.entity.PendingSyncEntity
import com.topic11.cs426.data.sync.FakeSyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingSyncDrainTest {
    @Test
    fun `queued command is handed to the adapter once`() = runTest {
        val attempted = mutableListOf<String>()
        val backoffs = mutableListOf<Long>()
        val drain = PendingSyncDrain(
            retryableCommands = flowOf(listOf(command(id = "sync-complete-a"))),
            sync = { commandId ->
                attempted += commandId
                FakeSyncResult.Synced
            },
            awaitBackoff = { backoffs += it },
        )

        val job = launch { drain.run() }
        runCurrent()
        job.cancelAndJoin()

        assertEquals(listOf("sync-complete-a"), attempted)
        assertEquals(emptyList<Long>(), backoffs)
    }

    /**
     * The regression this loop was written against: a failure writes the row back to `FAILED`, which
     * makes `observeRetryableCommands` re-emit immediately. Without backoff and an attempt budget the
     * collector would re-claim the same command forever at full speed.
     */
    @Test
    fun `failing command backs off and stops at the attempt budget`() = runTest {
        val commands = MutableStateFlow(listOf(command(id = COMMAND_ID)))
        val attempted = mutableListOf<String>()
        val backoffs = mutableListOf<Long>()
        val drain = PendingSyncDrain(
            retryableCommands = commands,
            sync = { commandId ->
                attempted += commandId
                // What SyncDao does: the claim bumps attempt_count, the failure parks the row back in
                // FAILED, and Room raises an invalidation for both writes.
                commands.value = listOf(
                    command(
                        id = commandId,
                        state = "FAILED",
                        attemptCount = commands.value.first().attemptCount + 1,
                    ),
                )
                FakeSyncResult.Failed("OFFLINE")
            },
            maxAttempts = 3,
            awaitBackoff = { backoffs += it },
        )

        val job = launch { drain.run() }
        runCurrent()

        assertEquals(listOf(COMMAND_ID, COMMAND_ID, COMMAND_ID), attempted)
        assertEquals(listOf(1_000L, 2_000L, 4_000L), backoffs)
        // The loop is parked on the flow rather than spinning, so cancelling it is enough to stop it.
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `backoff pauses the loop for the requested time`() = runTest {
        val commands = MutableStateFlow(listOf(command(id = COMMAND_ID)))
        var attempts = 0
        val drain = PendingSyncDrain(
            retryableCommands = commands,
            sync = {
                attempts += 1
                commands.value = listOf(
                    command(id = COMMAND_ID, state = "FAILED", attemptCount = attempts),
                )
                FakeSyncResult.Failed("OFFLINE")
            },
            maxAttempts = 2,
        )

        val job = launch { drain.run() }
        runCurrent()
        assertEquals(1, attempts)

        advanceTimeBy(999L)
        runCurrent()
        assertEquals(1, attempts)

        advanceTimeBy(1L)
        runCurrent()
        assertEquals(2, attempts)

        job.cancelAndJoin()
    }

    @Test
    fun `command past the attempt budget is left alone`() = runTest {
        val attempted = mutableListOf<String>()
        val backoffs = mutableListOf<Long>()
        val drain = PendingSyncDrain(
            retryableCommands = flowOf(
                listOf(command(id = COMMAND_ID, state = "FAILED", attemptCount = 3)),
            ),
            sync = { commandId ->
                attempted += commandId
                FakeSyncResult.Synced
            },
            maxAttempts = 3,
            awaitBackoff = { backoffs += it },
        )

        val job = launch { drain.run() }
        runCurrent()
        job.cancelAndJoin()

        assertEquals(emptyList<String>(), attempted)
        assertEquals(emptyList<Long>(), backoffs)
    }

    /** One command whose row moved under the adapter must not strand the rest of the queue. */
    @Test
    fun `adapter failure is reported and the rest of the snapshot still drains`() = runTest {
        val expected = IllegalStateException("Sync command changed before success was recorded")
        val attempted = mutableListOf<String>()
        val failures = mutableListOf<Throwable>()
        val drain = PendingSyncDrain(
            retryableCommands = flowOf(
                listOf(command(id = "sync-complete-a"), command(id = "sync-complete-b")),
            ),
            sync = { commandId ->
                attempted += commandId
                if (commandId == "sync-complete-a") throw expected
                FakeSyncResult.Synced
            },
            awaitBackoff = {},
            onDrainFailure = failures::add,
        )

        val job = launch { drain.run() }
        runCurrent()
        job.cancelAndJoin()

        assertEquals(listOf("sync-complete-a", "sync-complete-b"), attempted)
        assertEquals(1, failures.size)
        // Not assertSame: crossing the withContext boundary in the drain's critical section makes
        // coroutine stack-trace recovery hand back a copy carrying the recovered trace.
        assertEquals(expected.javaClass, failures.single().javaClass)
        assertEquals(expected.message, failures.single().message)
    }

    @Test
    fun `backoff grows exponentially and is capped`() {
        assertEquals(1_000L, pendingSyncBackoffMillis(1))
        assertEquals(2_000L, pendingSyncBackoffMillis(2))
        assertEquals(4_000L, pendingSyncBackoffMillis(3))
        assertEquals(8_000L, pendingSyncBackoffMillis(4))
        assertEquals(16_000L, pendingSyncBackoffMillis(5))
        assertEquals(32_000L, pendingSyncBackoffMillis(6))
        assertEquals(60_000L, pendingSyncBackoffMillis(7))
        assertEquals(60_000L, pendingSyncBackoffMillis(Int.MAX_VALUE))
    }

    private fun command(
        id: String,
        state: String = "PENDING",
        attemptCount: Int = 0,
    ) = PendingSyncEntity(
        id = id,
        aggregateType = "INSPECTION",
        aggregateId = id.removePrefix("sync-complete-"),
        operation = "COMPLETE",
        payloadVersion = 1,
        payloadJson = """{"inspectionId":"${id.removePrefix("sync-complete-")}"}""",
        state = state,
        attemptCount = attemptCount,
        lastErrorCode = if (state == "FAILED") "OFFLINE" else null,
        createdAtMillis = 1_000L,
        updatedAtMillis = 1_000L + attemptCount,
    )

    private companion object {
        const val COMMAND_ID = "sync-complete-laboratory-a2-safety-check"
    }
}

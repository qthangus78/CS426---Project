package com.topic11.cs426

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.data.RoomInspectionRepository
import com.topic11.cs426.data.seed.FieldFlowSampleDataSeeder
import com.topic11.cs426.data.sync.AttemptBasedFakeSyncScenario
import com.topic11.cs426.data.sync.FakeRemoteSyncAdapter
import com.topic11.cs426.data.sync.FakeSyncOutcome
import com.topic11.cs426.data.sync.FakeSyncScenario
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the wiring this fix is about: a real [FieldFlowDatabase], the real `SyncDao` flow, the real
 * adapter and [PendingSyncDrain] on a background job — the same objects the composition root builds.
 *
 * `FakeRemoteSyncAdapterTest` already proves a single sync call works. What was missing, and what let
 * the bug survive, is that nobody consumed the queue; this test asserts the queue actually drains and
 * that the inspection stops reporting the status the Dashboard counts as "Sync pending".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PendingSyncDrainEndToEndTest {
    private lateinit var database: FieldFlowDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FieldFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `drain clears the queue and the inspection stops reporting sync pending`() = runBlocking {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        assertEquals(InspectionStatus.SYNC_PENDING, inspectionStatus())
        assertEquals("PENDING", command()?.state)

        val drain = PendingSyncDrain(
            retryableCommands = database.syncDao().observeRetryableCommands(),
            sync = adapter(AttemptBasedFakeSyncScenario(outcomesByAttempt = emptyMap()))::sync,
        )

        withDrainRunning(drain) {
            awaitUntil("the queued command to reach SYNCED") { command()?.state == "SYNCED" }
        }

        assertEquals("SYNCED", database.inspectionDao().getInspection(INSPECTION_ID)?.syncStatus)
        assertEquals(InspectionStatus.COMPLETED, inspectionStatus())
        assertEquals(0, database.syncDao().observeRetryableCommands().first().size)
    }

    /**
     * A command that never succeeds goes back to `FAILED`, which re-emits from the DAO flow straight
     * away. The attempt budget is what stops that from becoming a retry loop that never sleeps.
     */
    @Test
    fun `permanently failing command stops being retried instead of hot looping`() = runBlocking {
        FieldFlowSampleDataSeeder(database).seedIfEmpty()
        val drain = PendingSyncDrain(
            retryableCommands = database.syncDao().observeRetryableCommands(),
            sync = adapter(FakeSyncScenario { FakeSyncOutcome.Failure(errorCode = "OFFLINE") })::sync,
            maxAttempts = MAX_ATTEMPTS,
            // Real backoff would make this test sleep for seconds. What is being asserted is that the
            // budget ends the retries, not how long each pause lasts.
            backoffMillisFor = { 10L },
        )

        withDrainRunning(drain) {
            awaitUntil("the attempt budget to be spent") { command()?.attemptCount == MAX_ATTEMPTS }

            // Long enough that an unbounded loop would have burned through many more attempts.
            delay(300L)
            assertEquals(MAX_ATTEMPTS, command()?.attemptCount)
        }

        assertEquals("FAILED", command()?.state)
        assertEquals("OFFLINE", command()?.lastErrorCode)
        assertEquals(InspectionStatus.SYNC_PENDING, inspectionStatus())
    }

    private fun adapter(scenario: FakeSyncScenario) = FakeRemoteSyncAdapter(
        syncDao = database.syncDao(),
        scenario = scenario,
        clock = System::currentTimeMillis,
    )

    private suspend fun withDrainRunning(
        drain: PendingSyncDrain,
        body: suspend () -> Unit,
    ) {
        val job = CoroutineScope(currentCoroutineContext()).launch { drain.run() }
        try {
            body()
        } finally {
            job.cancelAndJoin()
        }
    }

    private suspend fun awaitUntil(
        description: String,
        condition: suspend () -> Boolean,
    ) {
        try {
            withTimeout(TIMEOUT_MILLIS) {
                while (!condition()) {
                    delay(10L)
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            throw AssertionError("Timed out after ${TIMEOUT_MILLIS}ms waiting for $description", timeout)
        }
    }

    private suspend fun command() = database.syncDao().getCommand(COMMAND_ID)

    private suspend fun inspectionStatus() = RoomInspectionRepository(database)
        .getInspection(InspectionId(INSPECTION_ID))
        ?.status

    private companion object {
        const val COMMAND_ID = "sample-sync-lab-a2"
        const val INSPECTION_ID = "laboratory-a2-safety-check"
        const val MAX_ATTEMPTS = 3
        const val TIMEOUT_MILLIS = 5_000L
    }
}

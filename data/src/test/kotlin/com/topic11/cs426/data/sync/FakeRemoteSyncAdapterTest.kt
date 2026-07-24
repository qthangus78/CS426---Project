package com.topic11.cs426.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import java.util.ArrayDeque
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FakeRemoteSyncAdapterTest {
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
    fun `success records deterministic delay and durable synced state`() = runTest {
        database.syncDao().enqueueCommand(command())
        val observedDelays = mutableListOf<Long>()
        val adapter = adapter(
            scenario = AttemptBasedFakeSyncScenario(
                outcomesByAttempt = mapOf(1 to FakeSyncOutcome.Success(delayMillis = 250L)),
            ),
            times = ArrayDeque(listOf(2_100L, 2_350L)),
            delayOperation = observedDelays::add,
        )

        val result = adapter.sync(COMMAND_ID)

        assertEquals(FakeSyncResult.Synced, result)
        assertEquals(listOf(250L), observedDelays)
        assertCommand(state = "SYNCED", attempts = 1, errorCode = null)
    }

    @Test
    fun `configured failure can be retried with configured success`() = runTest {
        database.syncDao().enqueueCommand(command())
        val adapter = adapter(
            scenario = AttemptBasedFakeSyncScenario(
                outcomesByAttempt = mapOf(
                    1 to FakeSyncOutcome.Failure(errorCode = "OFFLINE"),
                    2 to FakeSyncOutcome.Success(),
                ),
            ),
            times = ArrayDeque(listOf(2_100L, 2_200L, 2_400L, 2_500L)),
        )

        assertEquals(FakeSyncResult.Failed("OFFLINE"), adapter.sync(COMMAND_ID))
        assertCommand(state = "FAILED", attempts = 1, errorCode = "OFFLINE")

        assertEquals(1, database.syncDao().retryFailed(COMMAND_ID, 2_300L))
        assertEquals(FakeSyncResult.Synced, adapter.sync(COMMAND_ID))
        assertCommand(state = "SYNCED", attempts = 2, errorCode = null)
    }

    @Test
    fun `completed command is not executed again`() = runTest {
        database.syncDao().enqueueCommand(command())
        database.syncDao().markSyncing(COMMAND_ID, 2_100L)
        database.syncDao().markSynced(COMMAND_ID, 2_200L)
        var scenarioCalls = 0
        val adapter = adapter(
            scenario = FakeSyncScenario {
                scenarioCalls += 1
                FakeSyncOutcome.Success()
            },
            times = ArrayDeque(listOf(2_300L)),
        )

        val result = adapter.sync(COMMAND_ID)

        assertEquals(FakeSyncResult.NotEligible, result)
        assertEquals(0, scenarioCalls)
        assertCommand(state = "SYNCED", attempts = 1, errorCode = null)
    }

    private fun adapter(
        scenario: FakeSyncScenario,
        times: ArrayDeque<Long>,
        delayOperation: suspend (Long) -> Unit = {},
    ) = FakeRemoteSyncAdapter(
        syncDao = database.syncDao(),
        scenario = scenario,
        clock = { times.removeFirst() },
        delayOperation = delayOperation,
    )

    private suspend fun assertCommand(
        state: String,
        attempts: Int,
        errorCode: String?,
    ) {
        val command = requireNotNull(database.syncDao().getCommand(COMMAND_ID))
        assertEquals(state, command.state)
        assertEquals(attempts, command.attemptCount)
        assertEquals(errorCode, command.lastErrorCode)
    }

    private fun command() = PendingSyncEntity(
        id = COMMAND_ID,
        aggregateType = "INSPECTION",
        aggregateId = "inspection-lab-i44",
        operation = "COMPLETE",
        payloadVersion = 1,
        payloadJson = """{"inspectionId":"inspection-lab-i44"}""",
        state = "PENDING",
        attemptCount = 0,
        lastErrorCode = null,
        createdAtMillis = 2_000L,
        updatedAtMillis = 2_000L,
    )

    private companion object {
        const val COMMAND_ID = "sync-inspection-complete"
    }
}

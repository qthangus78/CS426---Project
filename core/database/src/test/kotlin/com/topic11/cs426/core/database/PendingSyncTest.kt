package com.topic11.cs426.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import kotlinx.coroutines.flow.first
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
class PendingSyncTest {
    private lateinit var context: Context
    private var database: FieldFlowDatabase? = null

    @Before
    fun prepareDatabaseFile() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE_NAME)
        openDatabase()
    }

    @After
    fun removeDatabaseFile() {
        database?.close()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun `sync command follows guarded failure retry and success transitions`() = runTest {
        val syncDao = requireNotNull(database).syncDao()
        assertEquals(1L, syncDao.enqueueCommand(command()))
        assertEquals(0, syncDao.markSynced(COMMAND_ID, 2_100L))

        assertEquals(1, syncDao.markSyncing(COMMAND_ID, 2_100L))
        assertEquals(0, syncDao.markSyncing(COMMAND_ID, 2_200L))
        assertCommand(state = "SYNCING", attempts = 1, errorCode = null)

        assertEquals(1, syncDao.markFailed(COMMAND_ID, "NETWORK", 2_300L))
        assertCommand(state = "FAILED", attempts = 1, errorCode = "NETWORK")

        assertEquals(1, syncDao.retryFailed(COMMAND_ID, 2_400L))
        assertCommand(state = "PENDING", attempts = 1, errorCode = null)

        assertEquals(1, syncDao.markSyncing(COMMAND_ID, 2_500L))
        assertEquals(1, syncDao.markSynced(COMMAND_ID, 2_600L))
        assertCommand(state = "SYNCED", attempts = 2, errorCode = null)
    }

    @Test
    fun `enqueue is idempotent for the same command`() = runTest {
        val syncDao = requireNotNull(database).syncDao()

        assertEquals(1L, syncDao.enqueueCommand(command()))
        assertEquals(-1L, syncDao.enqueueCommand(command()))

        assertEquals(listOf(command()), syncDao.getCommands())
    }

    @Test
    fun `pending command survives database close and reopen`() = runTest {
        val expected = command()
        requireNotNull(database).syncDao().enqueueCommand(expected)

        reopenDatabase()
        val recovered = requireNotNull(database)
            .syncDao()
            .observeRetryableCommands()
            .first()

        assertEquals(listOf(expected), recovered)
    }

    private suspend fun assertCommand(
        state: String,
        attempts: Int,
        errorCode: String?,
    ) {
        val command = requireNotNull(
            requireNotNull(database).syncDao().getCommand(COMMAND_ID),
        )
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

    private fun openDatabase() {
        database = Room.databaseBuilder(
            context,
            FieldFlowDatabase::class.java,
            TEST_DATABASE_NAME,
        ).allowMainThreadQueries().build()
    }

    private fun reopenDatabase() {
        requireNotNull(database).close()
        openDatabase()
    }

    private companion object {
        const val TEST_DATABASE_NAME = "pending-sync-test.db"
        const val COMMAND_ID = "sync-inspection-complete"
    }
}

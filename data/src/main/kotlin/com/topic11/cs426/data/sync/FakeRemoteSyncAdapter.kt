package com.topic11.cs426.data.sync

import com.topic11.cs426.core.database.dao.SyncDao
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import kotlinx.coroutines.delay

class FakeRemoteSyncAdapter(
    private val syncDao: SyncDao,
    private val scenario: FakeSyncScenario,
    private val clock: () -> Long,
    private val delayOperation: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun sync(commandId: String): FakeSyncResult {
        val claimed = syncDao.markSyncing(commandId, clock())
        if (claimed == 0) {
            return FakeSyncResult.NotEligible
        }

        val command = checkNotNull(syncDao.getCommand(commandId)) {
            "Claimed sync command no longer exists: $commandId"
        }
        val outcome = scenario.outcomeFor(command)
        delayOperation(outcome.delayMillis)

        return when (outcome) {
            is FakeSyncOutcome.Success -> {
                check(syncDao.markSynced(commandId, clock()) == 1) {
                    "Sync command changed before success was recorded: $commandId"
                }
                FakeSyncResult.Synced
            }

            is FakeSyncOutcome.Failure -> {
                check(
                    syncDao.markFailed(
                        id = commandId,
                        errorCode = outcome.errorCode,
                        updatedAtMillis = clock(),
                    ) == 1,
                ) {
                    "Sync command changed before failure was recorded: $commandId"
                }
                FakeSyncResult.Failed(outcome.errorCode)
            }
        }
    }
}

fun interface FakeSyncScenario {
    fun outcomeFor(command: PendingSyncEntity): FakeSyncOutcome
}

class AttemptBasedFakeSyncScenario(
    outcomesByAttempt: Map<Int, FakeSyncOutcome>,
    private val defaultOutcome: FakeSyncOutcome = FakeSyncOutcome.Success(),
) : FakeSyncScenario {
    private val outcomesByAttempt = outcomesByAttempt.toMap()

    init {
        require(outcomesByAttempt.keys.all { it > 0 }) {
            "Fake sync attempt numbers must be positive."
        }
    }

    override fun outcomeFor(command: PendingSyncEntity): FakeSyncOutcome {
        return outcomesByAttempt[command.attemptCount] ?: defaultOutcome
    }
}

sealed interface FakeSyncOutcome {
    val delayMillis: Long

    data class Success(
        override val delayMillis: Long = 0L,
    ) : FakeSyncOutcome {
        init {
            require(delayMillis >= 0) { "Fake sync delay cannot be negative." }
        }
    }

    data class Failure(
        val errorCode: String,
        override val delayMillis: Long = 0L,
    ) : FakeSyncOutcome {
        init {
            require(errorCode.isNotBlank()) { "Fake sync error code cannot be blank." }
            require(delayMillis >= 0) { "Fake sync delay cannot be negative." }
        }
    }
}

sealed interface FakeSyncResult {
    data object Synced : FakeSyncResult

    data class Failed(val errorCode: String) : FakeSyncResult

    data object NotEligible : FakeSyncResult
}

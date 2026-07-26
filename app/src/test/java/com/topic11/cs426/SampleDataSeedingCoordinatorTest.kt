package com.topic11.cs426

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SampleDataSeedingCoordinatorTest {
    @Test
    fun `seeding starts in progress and reports success`() = runTest {
        val coordinator = SampleDataSeedingCoordinator(scope = backgroundScope, seedSampleData = {})

        assertEquals(SampleDataSeedingState.InProgress, coordinator.state.value)

        coordinator.start().join()

        assertEquals(SampleDataSeedingState.Succeeded, coordinator.state.value)
    }

    @Test
    fun `seeding failure is published as observable state`() = runTest {
        val expected = IllegalStateException("seed failure")
        val coordinator = SampleDataSeedingCoordinator(
            scope = backgroundScope,
            seedSampleData = { throw expected },
        )

        coordinator.start().join()

        assertEquals(SampleDataSeedingState.Failed(expected), coordinator.state.value)
    }

    @Test
    fun `retrying after a failure re-runs seeding and clears the failure`() = runTest {
        var attempts = 0
        val coordinator = SampleDataSeedingCoordinator(
            scope = backgroundScope,
            seedSampleData = {
                attempts++
                if (attempts == 1) throw IllegalStateException("seed failure")
            },
        )

        coordinator.start().join()
        assertEquals(
            SampleDataSeedingState.Failed::class.java,
            coordinator.state.value.javaClass,
        )

        coordinator.start().join()

        assertEquals(2, attempts)
        assertEquals(SampleDataSeedingState.Succeeded, coordinator.state.value)
    }

    @Test
    fun `retrying while seeding is in flight reuses the running job`() = runTest {
        val release = CompletableDeferred<Unit>()
        var attempts = 0
        val coordinator = SampleDataSeedingCoordinator(
            scope = backgroundScope,
            seedSampleData = {
                attempts++
                release.await()
            },
        )

        val running = coordinator.start()
        runCurrent()
        assertEquals(1, attempts)

        assertSame(running, coordinator.start())
        runCurrent()
        assertEquals(1, attempts)

        release.complete(Unit)
        running.join()
        assertEquals(SampleDataSeedingState.Succeeded, coordinator.state.value)
    }

    /**
     * Cancellation is how the graph shuts down, not a seeding defect — reporting it would put an
     * error banner on screen while the app is going away.
     */
    @Test
    fun `cancelled seeding is not reported as a failure`() = runTest {
        val coordinator = SampleDataSeedingCoordinator(
            scope = backgroundScope,
            seedSampleData = { awaitCancellation() },
        )

        val running = coordinator.start()
        runCurrent()
        running.cancelAndJoin()

        assertEquals(SampleDataSeedingState.InProgress, coordinator.state.value)
    }
}

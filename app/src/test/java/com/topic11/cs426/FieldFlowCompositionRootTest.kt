package com.topic11.cs426

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class FieldFlowCompositionRootTest {
    @Test
    fun `startup seeding is scheduled without blocking the caller`() {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val completed = CountDownLatch(1)

        try {
            val job = launchFieldFlowStartupSeeding(
                scope = scope,
                seedSampleData = {
                    started.countDown()
                    release.await(2, TimeUnit.SECONDS)
                    completed.countDown()
                },
                onFailure = { throwable ->
                    throw AssertionError("Unexpected sample data seeding failure", throwable)
                },
            )

            assertTrue(started.await(1, TimeUnit.SECONDS))
            assertEquals(1L, completed.count)

            release.countDown()
            runBlocking { job.join() }

            assertEquals(0L, completed.count)
        } finally {
            scope.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `startup seeding records non cancellation failures`() = runBlocking {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val expected = IllegalStateException("seed failure")
        val failure = AtomicReference<Throwable?>()

        try {
            val job = launchFieldFlowStartupSeeding(
                scope = scope,
                seedSampleData = { throw expected },
                onFailure = failure::set,
            )

            job.join()

            assertEquals(expected, failure.get())
        } finally {
            scope.cancel()
            dispatcher.close()
            executor.shutdownNow()
        }
    }
}

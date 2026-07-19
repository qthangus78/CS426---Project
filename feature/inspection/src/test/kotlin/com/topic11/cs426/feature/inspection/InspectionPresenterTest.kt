package com.topic11.cs426.feature.inspection

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionPresenterTest {
    @Test
    fun `present emits selected inspection`() = runTest {
        val screen = InspectionScreen(InspectionTestFixtures.computerLab.id.value)
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen, screen)
        val presenter = InspectionPresenter(
            screen = screen,
            observeInspection = ObserveInspectionUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertNull(loading.inspection)

            val content = awaitItem()
            assertFalse(content.isLoading)
            assertEquals(InspectionTestFixtures.computerLab, content.inspection)
        }
    }

    @Test
    fun `present handles unknown inspection safely`() = runTest {
        val screen = InspectionScreen("unknown-inspection")
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen, screen)
        val presenter = InspectionPresenter(
            screen = screen,
            observeInspection = ObserveInspectionUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitItem()

            assertFalse(content.isLoading)
            assertNull(content.inspection)
        }
    }

    @Test
    fun `back event pops the inspection screen`() = runTest {
        val screen = InspectionScreen(InspectionTestFixtures.projector.id.value)
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen, screen)
        val presenter = InspectionPresenter(
            screen = screen,
            observeInspection = ObserveInspectionUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitItem()

            content.eventSink(InspectionEvent.BackSelected)

            assertEquals(screen, navigator.awaitPop().poppedScreen)
        }
    }
}

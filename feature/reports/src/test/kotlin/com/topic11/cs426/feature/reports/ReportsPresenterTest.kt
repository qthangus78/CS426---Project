package com.topic11.cs426.feature.reports

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsPresenterTest {
    @Test
    fun `present emits honest placeholder state`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, ReportsScreen)
        val presenter = ReportsPresenter(navigator)

        presenter.test {
            val state = awaitItem()

            assertEquals("Reports", state.title)
            assertEquals("Not implemented yet.", state.message)
            assertTrue(state.details.contains("future Domain ports"))
            assertEquals(
                listOf(
                    "inspection report presentation",
                    "completed-inspection eligibility",
                    "PDF and JSON exporters behind Domain ports",
                    "export and sharing status",
                ),
                state.futureResponsibilities,
            )
        }
    }

    @Test
    fun `back event pops reports screen`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, ReportsScreen)
        val presenter = ReportsPresenter(navigator)

        presenter.test {
            val state = awaitItem()

            state.eventSink(ReportsEvent.BackSelected)

            assertEquals(ReportsScreen, navigator.awaitPop().poppedScreen)
        }
    }
}

package com.topic11.cs426.feature.reports

import com.slack.circuit.runtime.Navigator
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

            assertEquals("Reports", state.topBarTitle)
            assertEquals("Inspection reports", state.title)
            assertEquals("Report generation is a future milestone.", state.message)
            assertTrue(state.details.contains("Domain report ports"))
            assertTrue(state.details.contains("Data adapters"))
            assertEquals(
                listOf(
                    ReportCapabilityUi(
                        title = "Completed inspection summaries",
                        description = "Present report-ready inspection results after the workflow is implemented.",
                    ),
                    ReportCapabilityUi(
                        title = "Report eligibility",
                        description = "Show which completed inspections can be exported after validation exists.",
                    ),
                    ReportCapabilityUi(
                        title = "PDF export through a Domain port",
                        description = "Keep document generation behind a replaceable boundary.",
                    ),
                    ReportCapabilityUi(
                        title = "JSON export through a Domain port",
                        description = "Support structured export without coupling UI to file details.",
                    ),
                    ReportCapabilityUi(
                        title = "Export and sharing status",
                        description = "Display future export progress honestly when a real implementation exists.",
                    ),
                ),
                state.futureCapabilities,
            )
        }
    }

    @Test
    fun `presenter depends only on navigator`() {
        val constructorTypes = ReportsPresenter::class.java
            .declaredConstructors
            .single()
            .parameterTypes
            .toList()

        assertEquals(listOf(Navigator::class.java), constructorTypes)
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

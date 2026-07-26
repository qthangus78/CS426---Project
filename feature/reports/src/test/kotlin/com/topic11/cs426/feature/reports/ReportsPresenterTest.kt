package com.topic11.cs426.feature.reports

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
            assertEquals("No completed reports are available yet.", state.message)
            assertEquals(
                "Complete inspections to make report summaries available here.",
                state.details,
            )
            assertEquals(
                listOf(
                    ReportCapabilityUi(
                        title = "Completed inspection summaries",
                        description = "Present report-ready inspection results from completed local inspections.",
                    ),
                    ReportCapabilityUi(
                        title = "Report eligibility",
                        description = "Show which completed inspections are ready for export.",
                    ),
                    ReportCapabilityUi(
                        title = "PDF export",
                        description = "Create a portable inspection report for sharing.",
                    ),
                    ReportCapabilityUi(
                        title = "JSON export",
                        description = "Export structured inspection data for record keeping.",
                    ),
                    ReportCapabilityUi(
                        title = "Export and sharing status",
                        description = "Show export progress and sharing results.",
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

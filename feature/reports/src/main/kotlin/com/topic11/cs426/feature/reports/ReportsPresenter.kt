package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter

internal class ReportsPresenter(
    private val navigator: Navigator,
) : Presenter<ReportsState> {
    @Composable
    override fun present(): ReportsState {
        val eventSink = remember(navigator) {
            { event: ReportsEvent ->
                when (event) {
                    ReportsEvent.BackSelected -> navigator.pop()
                }
                Unit
            }
        }
        return ReportsState(eventSink = eventSink)
    }
}

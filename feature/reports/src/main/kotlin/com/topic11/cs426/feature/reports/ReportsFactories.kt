package com.topic11.cs426.feature.reports

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.ReportDetailScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import com.topic11.cs426.domain.usecase.ExportInspectionReportUseCase
import com.topic11.cs426.domain.usecase.GenerateInspectionReportUseCase
import com.topic11.cs426.domain.usecase.ObserveReportCandidatesUseCase
import com.topic11.cs426.domain.usecase.ObserveReportHistoryUseCase

class ReportsPresenterFactory(
    private val observeReportCandidates: ObserveReportCandidatesUseCase,
    private val observeReportHistory: ObserveReportHistoryUseCase,
    private val generateInspectionReport: GenerateInspectionReportUseCase,
    private val exportInspectionReport: ExportInspectionReportUseCase,
    private val reportActionHandler: ReportActionHandler,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            ReportsScreen -> ReportsPresenter(
                observeReportCandidates = observeReportCandidates,
                observeReportHistory = observeReportHistory,
                reportActionHandler = reportActionHandler,
                navigator = navigator,
            )
            is ReportDetailScreen -> ReportDetailPresenter(
                screen = screen,
                generateInspectionReport = generateInspectionReport,
                exportInspectionReport = exportInspectionReport,
                reportActionHandler = reportActionHandler,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class ReportsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            ReportsScreen -> ui<ReportsState> { state, modifier ->
                ReportsUi(state = state, modifier = modifier)
            }
            is ReportDetailScreen -> ui<ReportDetailState> { state, modifier ->
                ReportDetailUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}

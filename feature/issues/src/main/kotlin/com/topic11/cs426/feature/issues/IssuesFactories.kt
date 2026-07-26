package com.topic11.cs426.feature.issues

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.IssueDetailScreen
import com.topic11.cs426.core.navigation.IssuesScreen
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveIssueUseCase
import com.topic11.cs426.domain.usecase.ObserveIssuesUseCase
import com.topic11.cs426.domain.usecase.UpdateIssueStatusUseCase

class IssuesPresenterFactory(
    private val observeIssues: ObserveIssuesUseCase,
    private val observeIssue: ObserveIssueUseCase,
    private val observeAssets: ObserveAssetsUseCase,
    private val observeInspectionSummaries: ObserveInspectionSummariesUseCase,
    private val updateIssueStatus: UpdateIssueStatusUseCase,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            IssuesScreen -> IssuesPresenter(
                observeIssues = observeIssues,
                observeAssets = observeAssets,
                observeInspectionSummaries = observeInspectionSummaries,
                navigator = navigator,
            )
            is IssueDetailScreen -> IssueDetailPresenter(
                screen = screen,
                observeIssue = observeIssue,
                observeAssets = observeAssets,
                observeInspectionSummaries = observeInspectionSummaries,
                updateIssueStatus = updateIssueStatus,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class IssuesUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            IssuesScreen -> ui<IssuesState> { state, modifier ->
                IssuesUi(state = state, modifier = modifier)
            }
            is IssueDetailScreen -> ui<IssueDetailState> { state, modifier ->
                IssueDetailUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}

package com.topic11.cs426.feature.templates

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.TemplateDetailScreen
import com.topic11.cs426.core.navigation.TemplateEditorScreen
import com.topic11.cs426.core.navigation.TemplatesScreen
import com.topic11.cs426.domain.usecase.CreateTemplateUseCase
import com.topic11.cs426.domain.usecase.GetTemplateUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplateUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import com.topic11.cs426.domain.usecase.UpdateTemplateMetadataUseCase

class TemplatesPresenterFactory(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val observeTemplate: ObserveTemplateUseCase,
    private val getTemplate: GetTemplateUseCase,
    private val createTemplate: CreateTemplateUseCase,
    private val updateTemplateMetadata: UpdateTemplateMetadataUseCase,
    private val observeAssets: ObserveAssetsUseCase,
    private val startInspection: StartInspectionUseCase,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            TemplatesScreen -> TemplatesPresenter(
                observeTemplates = observeTemplates,
                navigator = navigator,
            )
            is TemplateDetailScreen -> TemplateDetailPresenter(
                screen = screen,
                observeTemplate = observeTemplate,
                observeAssets = observeAssets,
                startInspection = startInspection,
                navigator = navigator,
            )
            is TemplateEditorScreen -> TemplateEditorPresenter(
                screen = screen,
                getTemplate = getTemplate,
                createTemplate = createTemplate,
                updateTemplateMetadata = updateTemplateMetadata,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class TemplatesUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            TemplatesScreen -> ui<TemplatesState> { state, modifier ->
                TemplatesUi(state = state, modifier = modifier)
            }
            is TemplateDetailScreen -> ui<TemplateDetailState> { state, modifier ->
                TemplateDetailUi(state = state, modifier = modifier)
            }
            is TemplateEditorScreen -> ui<TemplateEditorState> { state, modifier ->
                TemplateEditorUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}

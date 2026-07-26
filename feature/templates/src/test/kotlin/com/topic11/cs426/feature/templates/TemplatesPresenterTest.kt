package com.topic11.cs426.feature.templates

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.navigation.TemplateDetailScreen
import com.topic11.cs426.core.navigation.TemplateEditorScreen
import com.topic11.cs426.core.navigation.TemplatesScreen
import com.topic11.cs426.core.testing.FakeAssetRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.usecase.CreateTemplateUseCase
import com.topic11.cs426.domain.usecase.GetTemplateUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplateUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import com.topic11.cs426.domain.usecase.UpdateTemplateMetadataUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplatesPresenterTest {
    @Test
    fun `list presents templates and opens detail`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, TemplatesScreen)
        val repository = FakeTemplateRepository(
            mapOf(InspectionTestFixtures.templateId to InspectionTestFixtures.sampleTemplate),
        )
        val presenter = TemplatesPresenter(
            observeTemplates = ObserveTemplatesUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            assertEquals(TemplatesState.Loading, awaitItem())
            val content = awaitItem() as TemplatesState.Content

            assertEquals(listOf(InspectionTestFixtures.templateId), content.templates.map { it.id })
            content.eventSink(TemplatesEvent.TemplateSelected(InspectionTestFixtures.templateId))

            assertEquals(
                TemplateDetailScreen(InspectionTestFixtures.templateId.value),
                navigator.awaitNextScreen(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `list presents empty state`() = runTest {
        val presenter = TemplatesPresenter(
            observeTemplates = ObserveTemplatesUseCase(FakeTemplateRepository()),
            navigator = FakeNavigator(DashboardScreen, TemplatesScreen),
        )

        presenter.test {
            assertEquals(TemplatesState.Loading, awaitItem())
            assertTrue(awaitItem() is TemplatesState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editor creates template with initial checklist`() = runTest {
        val screen = TemplateEditorScreen(templateId = null)
        val navigator = FakeNavigator(DashboardScreen, TemplatesScreen, screen)
        val repository = FakeTemplateRepository()
        val presenter = TemplateEditorPresenter(
            screen = screen,
            getTemplate = GetTemplateUseCase(repository),
            createTemplate = CreateTemplateUseCase(
                templateRepository = repository,
                idFactory = { TemplateId("template-created") },
            ),
            updateTemplateMetadata = UpdateTemplateMetadataUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val editing = awaitEditor()
            editing.eventSink(TemplateEditorEvent.NameChanged("Room readiness"))
            var current = awaitItem() as TemplateEditorState.Editing
            current.eventSink(TemplateEditorEvent.ItemTitleChanged("Projector starts"))
            current = awaitItem() as TemplateEditorState.Editing
            current.eventSink(TemplateEditorEvent.SaveSelected)
            advanceUntilIdle()

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            val saved = requireNotNull(repository.getTemplate(TemplateId("template-created")))
            assertEquals("Room readiness", saved.name)
            assertEquals("Projector starts", saved.sections.single().items.single().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editor shows validation errors without saving`() = runTest {
        val screen = TemplateEditorScreen(templateId = null)
        val navigator = FakeNavigator(DashboardScreen, TemplatesScreen, screen)
        val repository = FakeTemplateRepository()
        val presenter = TemplateEditorPresenter(
            screen = screen,
            getTemplate = GetTemplateUseCase(repository),
            createTemplate = CreateTemplateUseCase(
                templateRepository = repository,
                idFactory = { TemplateId("template-created") },
            ),
            updateTemplateMetadata = UpdateTemplateMetadataUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val editing = awaitEditor()

            editing.eventSink(TemplateEditorEvent.SaveSelected)
            advanceUntilIdle()
            val validation = awaitEditorWithValidation()

            assertEquals(
                listOf(
                    "Template name is required.",
                    "Checklist item title is required.",
                ),
                validation.validationMessages,
            )
            assertEquals(null, repository.getTemplate(TemplateId("template-created")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editor updates template metadata without changing checklist`() = runTest {
        val original = InspectionTestFixtures.sampleTemplate
        val screen = TemplateEditorScreen(original.id.value)
        val navigator = FakeNavigator(DashboardScreen, TemplatesScreen, TemplateDetailScreen(original.id.value), screen)
        val repository = FakeTemplateRepository(mapOf(original.id to original))
        val presenter = TemplateEditorPresenter(
            screen = screen,
            getTemplate = GetTemplateUseCase(repository),
            createTemplate = CreateTemplateUseCase(repository),
            updateTemplateMetadata = UpdateTemplateMetadataUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val editing = awaitEditor()
            editing.eventSink(TemplateEditorEvent.NameChanged("Updated checklist"))
            val updated = awaitItem() as TemplateEditorState.Editing
            updated.eventSink(TemplateEditorEvent.SaveSelected)
            advanceUntilIdle()

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            val saved = requireNotNull(repository.getTemplate(original.id))
            assertEquals("Updated checklist", saved.name)
            assertEquals(original.sections, saved.sections)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail presents sections and checklist semantics`() = runTest {
        val templateRepository = FakeTemplateRepository(
            mapOf(InspectionTestFixtures.templateId to InspectionTestFixtures.sampleTemplate),
        )
        val screen = TemplateDetailScreen(InspectionTestFixtures.templateId.value)
        val presenter = TemplateDetailPresenter(
            screen = screen,
            observeTemplate = ObserveTemplateUseCase(templateRepository),
            observeAssets = ObserveAssetsUseCase(FakeAssetRepository()),
            startInspection = StartInspectionUseCase(
                inspectionRepository = RecordingInspectionRepository(),
                templateRepository = templateRepository,
            ),
            navigator = FakeNavigator(DashboardScreen, TemplatesScreen, screen),
        )

        presenter.test {
            val content = awaitDetailContent()

            assertEquals("Lab Safety Checklist", content.template.name)
            assertEquals(2, content.template.sections.size)
            assertTrue(content.template.sections.first().items.first().required)
            assertTrue(content.template.sections.first().items.first().critical)
            assertEquals("Weight 5", content.template.sections.first().items.first().weightLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail missing state supports back navigation`() = runTest {
        val screen = TemplateDetailScreen("missing-template")
        val navigator = FakeNavigator(DashboardScreen, TemplatesScreen, screen)
        val presenter = TemplateDetailPresenter(
            screen = screen,
            observeTemplate = ObserveTemplateUseCase(FakeTemplateRepository()),
            observeAssets = ObserveAssetsUseCase(FakeAssetRepository()),
            startInspection = StartInspectionUseCase(
                inspectionRepository = RecordingInspectionRepository(),
                templateRepository = FakeTemplateRepository(),
            ),
            navigator = navigator,
        )

        presenter.test {
            var state = awaitItem()
            while (state !is TemplateDetailState.Missing) {
                state = awaitItem()
            }

            state.eventSink(TemplateDetailEvent.BackSelected)

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `template detail starts inspection with selected asset`() = runTest {
        val templateRepository = FakeTemplateRepository(
            mapOf(InspectionTestFixtures.templateId to InspectionTestFixtures.sampleTemplate),
        )
        val assetRepository = FakeAssetRepository()
        val inspectionRepository = RecordingInspectionRepository()
        val screen = TemplateDetailScreen(InspectionTestFixtures.templateId.value)
        val navigator = FakeNavigator(DashboardScreen, TemplatesScreen, screen)
        val presenter = TemplateDetailPresenter(
            screen = screen,
            observeTemplate = ObserveTemplateUseCase(templateRepository),
            observeAssets = ObserveAssetsUseCase(assetRepository),
            startInspection = StartInspectionUseCase(inspectionRepository, templateRepository),
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitDetailContent()

            content.eventSink(TemplateDetailEvent.StartInspectionSelected)
            val selecting = awaitDetailContent()
            assertTrue(selecting.startInspection.isVisible)
            selecting.eventSink(TemplateDetailEvent.StartInspectionConfirmed)
            advanceUntilIdle()

            assertEquals(InspectionScreen("inspection-1"), navigator.awaitNextScreen())
            assertEquals(1, inspectionRepository.createInspectionCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<TemplateEditorState>.awaitEditor(): TemplateEditorState.Editing {
        var state = awaitItem()
        while (state !is TemplateEditorState.Editing) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<TemplateEditorState>.awaitEditorWithValidation(): TemplateEditorState.Editing {
        var state = awaitItem()
        while (state !is TemplateEditorState.Editing || state.validationMessages.isEmpty()) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<TemplateDetailState>.awaitDetailContent(): TemplateDetailState.Content {
        var state = awaitItem()
        while (state !is TemplateDetailState.Content) {
            state = awaitItem()
        }
        return state
    }
}

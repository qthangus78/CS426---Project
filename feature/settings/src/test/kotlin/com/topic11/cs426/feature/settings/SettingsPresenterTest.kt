package com.topic11.cs426.feature.settings

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.SettingsScreen
import com.topic11.cs426.domain.model.ThemeMode
import com.topic11.cs426.domain.repository.AppearancePreferenceRepository
import com.topic11.cs426.domain.usecase.ObserveThemeModeUseCase
import com.topic11.cs426.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPresenterTest {
    @Test
    fun `settings presents theme mode and saves selection`() = runTest {
        val repository = RecordingAppearancePreferenceRepository()
        val presenter = settingsPresenter(
            repository = repository,
            navigator = FakeNavigator(DashboardScreen, SettingsScreen),
        )

        presenter.test {
            assertEquals(SettingsState.Loading, awaitItem())
            val content = awaitItem() as SettingsState.Content
            assertEquals(ThemeMode.SYSTEM, content.selectedThemeMode)

            content.eventSink(SettingsEvent.ThemeModeSelected(ThemeMode.DARK))
            advanceUntilIdle()

            val updated = awaitItem() as SettingsState.Content
            assertEquals(ThemeMode.DARK, updated.selectedThemeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `settings maps save failure to product message`() = runTest {
        val repository = RecordingAppearancePreferenceRepository(failWrites = true)
        val presenter = settingsPresenter(
            repository = repository,
            navigator = FakeNavigator(DashboardScreen, SettingsScreen),
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as SettingsState.Content

            content.eventSink(SettingsEvent.ThemeModeSelected(ThemeMode.LIGHT))
            advanceUntilIdle()

            var state = awaitItem()
            while (state !is SettingsState.Content || state.errorMessage == null) {
                state = awaitItem()
            }
            assertEquals("Appearance setting could not be saved.", state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `settings back event pops screen`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, SettingsScreen)
        val presenter = settingsPresenter(
            repository = RecordingAppearancePreferenceRepository(),
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as SettingsState.Content

            content.eventSink(SettingsEvent.BackSelected)

            assertEquals(SettingsScreen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `settings presents load error`() = runTest {
        val presenter = settingsPresenter(
            repository = FailingAppearancePreferenceRepository(),
            navigator = FakeNavigator(DashboardScreen, SettingsScreen),
        )

        presenter.test {
            assertEquals(SettingsState.Loading, awaitItem())
            assertTrue(awaitItem() is SettingsState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun settingsPresenter(
        repository: AppearancePreferenceRepository,
        navigator: FakeNavigator,
    ) = SettingsPresenter(
        observeThemeMode = ObserveThemeModeUseCase(repository),
        setThemeMode = SetThemeModeUseCase(repository),
        navigator = navigator,
    )
}

private class RecordingAppearancePreferenceRepository(
    private val failWrites: Boolean = false,
) : AppearancePreferenceRepository {
    private val mode = MutableStateFlow(ThemeMode.SYSTEM)

    override fun observeThemeMode(): Flow<ThemeMode> = mode

    override suspend fun setThemeMode(mode: ThemeMode) {
        if (failWrites) error("write failed")
        this.mode.value = mode
    }
}

private class FailingAppearancePreferenceRepository : AppearancePreferenceRepository {
    override fun observeThemeMode(): Flow<ThemeMode> =
        kotlinx.coroutines.flow.flow { error("read failed") }

    override suspend fun setThemeMode(mode: ThemeMode) = Unit
}

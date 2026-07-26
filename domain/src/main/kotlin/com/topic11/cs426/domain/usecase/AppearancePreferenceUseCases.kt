package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.ThemeMode
import com.topic11.cs426.domain.repository.AppearancePreferenceRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(
    private val appearancePreferenceRepository: AppearancePreferenceRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = appearancePreferenceRepository.observeThemeMode()
}

class SetThemeModeUseCase(
    private val appearancePreferenceRepository: AppearancePreferenceRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) {
        appearancePreferenceRepository.setThemeMode(mode)
    }
}

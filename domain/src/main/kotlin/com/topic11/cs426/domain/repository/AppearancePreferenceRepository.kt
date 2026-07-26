package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppearancePreferenceRepository {
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}

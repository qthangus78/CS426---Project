package com.topic11.cs426

import android.content.Context
import android.content.SharedPreferences
import com.topic11.cs426.domain.model.ThemeMode
import com.topic11.cs426.domain.repository.AppearancePreferenceRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class AndroidAppearancePreferenceRepository(
    private val preferences: SharedPreferences,
) : AppearancePreferenceRepository {
    override fun observeThemeMode(): Flow<ThemeMode> =
        callbackFlow {
            trySend(preferences.readThemeMode())
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { updated, key ->
                if (key == THEME_MODE_KEY) {
                    trySend(updated.readThemeMode())
                }
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferences.edit()
            .putString(THEME_MODE_KEY, mode.name)
            .apply()
    }

    private fun SharedPreferences.readThemeMode(): ThemeMode {
        val stored = getString(THEME_MODE_KEY, null)
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    companion object {
        private const val PREFERENCES_NAME = "fieldflow_preferences"
        private const val THEME_MODE_KEY = "theme_mode"

        fun create(context: Context): AndroidAppearancePreferenceRepository =
            AndroidAppearancePreferenceRepository(
                preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            )
    }
}

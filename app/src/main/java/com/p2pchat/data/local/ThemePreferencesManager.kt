package com.p2pchat.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global app preference manager for Dark/Light theme mode.
 */
@Singleton
class ThemePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "p2pchat_preferences"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_DISPLAY_NAME = "key_display_name"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _displayName = MutableStateFlow(prefs.getString(KEY_DISPLAY_NAME, "P2Chat User") ?: "P2Chat User")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
        _displayName.value = name
    }
}

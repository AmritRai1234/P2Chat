package com.p2pchat.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p2pchat.data.local.ThemePreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SettingsUiState(
    val displayName: String = "P2Chat User",
    val isDarkMode: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePrefs: ThemePreferencesManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        themePrefs.displayName,
        themePrefs.isDarkMode
    ) { name, isDark ->
        SettingsUiState(displayName = name, isDarkMode = isDark)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateDisplayName(name: String) {
        themePrefs.setDisplayName(name)
    }

    fun toggleDarkMode() {
        themePrefs.setDarkMode(!uiState.value.isDarkMode)
    }
}

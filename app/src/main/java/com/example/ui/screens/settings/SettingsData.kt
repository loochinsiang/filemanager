package com.example.ui.screens.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class SettingsLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

data class SettingsProfileState(
    val isLoading: Boolean,
    val isLoggedIn: Boolean,
    val accountName: String,
    val accountEmail: String,
    val accountImageUrl: String?,
)

data class SettingsContentState(
    val profileHeader: SettingsProfileState,
    val quickActions: List<SettingsQuickAction>,
    val integrations: List<SettingsIntegrationAction>,
    val groups: List<SettingsGroup>,
    val internalGroup: SettingsGroup?,
    val showPermissionBanner: Boolean,
    val showUpdateBanner: Boolean,
    val latestVersion: String,
    val isSearchActive: Boolean,
    val hasSearchResults: Boolean,
    val onProfileHeaderClick: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onUpdateClick: () -> Unit,
)

data class SettingsQuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val accentColor: Color,
)

data class SettingsGroup(
    val title: String,
    val items: List<SettingsItem>,
)

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val showUpdateIndicator: Boolean = false,
    val accentColor: Color = Color.Unspecified,
    val keywords: List<String> = emptyList(),
    val onClick: () -> Unit,
)

data class SettingsIntegrationAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val accentColor: Color,
)

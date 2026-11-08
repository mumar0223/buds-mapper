package com.budsmapper.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.budsmapper.app.data.AppProfileStore
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.ui.screens.AppGestureMappingScreen
import com.budsmapper.app.ui.screens.AppProfilesScreen
import com.budsmapper.app.ui.screens.HomeScreen
import com.budsmapper.app.ui.screens.SettingsScreen
import com.budsmapper.app.ui.screens.TestDiagnosticsScreen
import com.budsmapper.app.ui.theme.LocalThemePalette
import com.budsmapper.app.ui.theme.ThemeMode
import com.budsmapper.app.ui.theme.ThemeStore

sealed interface Screen {
    data object Home : Screen
    data object AppProfiles : Screen
    data class GestureMapping(val packageName: String, val appLabel: String) : Screen
    data object TestDiagnostics : Screen
    data object Settings : Screen
}

@Composable
fun BudsMapperApp(
    eventStore: LastEventStore,
    profileStore: AppProfileStore = AppProfileStore.getInstance(LocalContext.current),
    themeStore: ThemeStore = ThemeStore.getInstance(LocalContext.current),
    onThemeModeChanged: (isDark: Boolean) -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val currentMode by themeStore.themeMode.collectAsState()
    val currentAccent by themeStore.accentTheme.collectAsState()
    val palette = remember(currentMode, currentAccent) {
        themeStore.getPalette(currentMode, currentAccent)
    }

    // Notify activity to dynamically update system status bar icons (white in dark mode, dark in light mode)
    LaunchedEffect(currentMode) {
        onThemeModeChanged(currentMode == ThemeMode.DARK)
    }

    // Android Hardware Back Button Handler:
    // Sub-pages navigate back to the previous screen; on Home, it exits the app naturally!
    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = when (currentScreen) {
            is Screen.GestureMapping -> Screen.AppProfiles
            is Screen.AppProfiles -> Screen.Home
            is Screen.TestDiagnostics -> Screen.Home
            is Screen.Settings -> Screen.Home
            is Screen.Home -> Screen.Home
        }
    }

    CompositionLocalProvider(LocalThemePalette provides palette) {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    eventStore = eventStore,
                    profileStore = profileStore,
                    onNavigateToProfiles = { currentScreen = Screen.AppProfiles },
                    onNavigateToTest = { currentScreen = Screen.TestDiagnostics },
                    onNavigateToSettings = { currentScreen = Screen.Settings }
                )
            }
            is Screen.AppProfiles -> {
                AppProfilesScreen(
                    profileStore = profileStore,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onNavigateToMapping = { pkg, label ->
                        currentScreen = Screen.GestureMapping(pkg, label)
                    }
                )
            }
            is Screen.GestureMapping -> {
                AppGestureMappingScreen(
                    packageName = screen.packageName,
                    appLabel = screen.appLabel,
                    profileStore = profileStore,
                    onNavigateBack = { currentScreen = Screen.AppProfiles }
                )
            }
            is Screen.TestDiagnostics -> {
                TestDiagnosticsScreen(
                    eventStore = eventStore,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    themeStore = themeStore,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}

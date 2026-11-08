package com.budsmapper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.ui.BudsMapperApp
import com.budsmapper.app.ui.theme.ThemeMode
import com.budsmapper.app.ui.theme.ThemeStore

class MainActivity : ComponentActivity() {

    private lateinit var eventStore: LastEventStore
    private lateinit var themeStore: ThemeStore

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        eventStore = LastEventStore.getInstance(applicationContext)
        themeStore = ThemeStore.getInstance(applicationContext)

        val isDarkInitial = themeStore.themeMode.value == ThemeMode.DARK
        applySystemBars(isDarkInitial)

        setContent {
            BudsMapperApp(
                eventStore = eventStore,
                themeStore = themeStore,
                onThemeModeChanged = { isDark ->
                    applySystemBars(isDark)
                }
            )
        }
    }

    private fun applySystemBars(isDark: Boolean) {
        val navColor = android.graphics.Color.TRANSPARENT
        val statusColor = android.graphics.Color.TRANSPARENT

        if (isDark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(statusColor),
                navigationBarStyle = SystemBarStyle.dark(navColor)
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(statusColor, statusColor),
                navigationBarStyle = SystemBarStyle.light(navColor, navColor)
            )
        }

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // isAppearanceLightStatusBars = false -> WHITE icons (for Dark Mode)
        // isAppearanceLightStatusBars = true -> DARK icons (for Light Mode)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
    }

    override fun onResume() {
        super.onResume()
        eventStore.reload()
    }
}

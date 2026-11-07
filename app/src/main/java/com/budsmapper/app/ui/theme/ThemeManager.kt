package com.budsmapper.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    DARK,
    LIGHT
}

enum class AccentTheme(val id: String, val displayName: String) {
    FUSION("FUSION", "⚡ Fusion"),
    BLUE("BLUE", "💧 Cyber Blue"),
    RED("RED", "🔥 Hyper Red"),
    MINT("MINT", "🍃 Cyber Mint");

    companion object {
        fun fromId(id: String): AccentTheme = entries.find { it.id == id } ?: FUSION
    }
}

data class ThemePalette(
    val isDark: Boolean,
    val backgroundColor: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val cardBorderActive: Color,
    val primaryAccent: Color,
    val statusAccent: Color,
    val actionAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val auraColor: Color,
    val auraAlpha: Float,
    val buttonOutlineBackground: Color
)

class ThemeStore private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accentTheme = MutableStateFlow(
        AccentTheme.fromId(prefs.getString(KEY_ACCENT_THEME, AccentTheme.FUSION.id) ?: AccentTheme.FUSION.id)
    )
    val accentTheme: StateFlow<AccentTheme> = _accentTheme.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setAccentTheme(accent: AccentTheme) {
        _accentTheme.value = accent
        prefs.edit().putString(KEY_ACCENT_THEME, accent.id).apply()
    }

    fun getPalette(mode: ThemeMode = _themeMode.value, accent: AccentTheme = _accentTheme.value): ThemePalette {
        val isDark = mode == ThemeMode.DARK
        return if (isDark) {
            when (accent) {
                AccentTheme.FUSION -> ThemePalette(
                    isDark = true,
                    backgroundColor = Color(0xFF0C1019),
                    cardBackground = Color(0xFF141926),
                    cardBorder = Color(0xFF252E46),
                    cardBorderActive = Color(0xFF5E81F4).copy(alpha = 0.5f),
                    primaryAccent = Color(0xFF5E81F4), // Cyber Ice Blue
                    statusAccent = Color(0xFF00E5A3),  // Neon Cyber Mint
                    actionAccent = Color(0xFFFF3B5C),  // Hyper Crimson
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFF94A3B8),
                    textMuted = Color(0xFF64748B),
                    auraColor = Color(0xFF5E81F4),
                    auraAlpha = 0.22f,
                    buttonOutlineBackground = Color(0xFF5E81F4).copy(alpha = 0.08f)
                )
                AccentTheme.BLUE -> ThemePalette(
                    isDark = true,
                    backgroundColor = Color(0xFF0C1019),
                    cardBackground = Color(0xFF141926),
                    cardBorder = Color(0xFF252E46),
                    cardBorderActive = Color(0xFF5E81F4).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF5E81F4),
                    statusAccent = Color(0xFF5E81F4),
                    actionAccent = Color(0xFF6C8EEF),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFF94A3B8),
                    textMuted = Color(0xFF64748B),
                    auraColor = Color(0xFF4D74FF),
                    auraAlpha = 0.24f,
                    buttonOutlineBackground = Color(0xFF5E81F4).copy(alpha = 0.08f)
                )
                AccentTheme.RED -> ThemePalette(
                    isDark = true,
                    backgroundColor = Color(0xFF0F0E17),
                    cardBackground = Color(0xFF181422),
                    cardBorder = Color(0xFF332538),
                    cardBorderActive = Color(0xFFFF3B5C).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFFFF3B5C),
                    statusAccent = Color(0xFFFF3B5C),
                    actionAccent = Color(0xFFFF2A55),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFA197AA),
                    textMuted = Color(0xFF756A7F),
                    auraColor = Color(0xFFFF2A55),
                    auraAlpha = 0.20f,
                    buttonOutlineBackground = Color(0xFFFF3B5C).copy(alpha = 0.08f)
                )
                AccentTheme.MINT -> ThemePalette(
                    isDark = true,
                    backgroundColor = Color(0xFF0A1215),
                    cardBackground = Color(0xFF111E20),
                    cardBorder = Color(0xFF1E383A),
                    cardBorderActive = Color(0xFF00E5A3).copy(alpha = 0.6f),
                    primaryAccent = Color(0xFF00E5A3),
                    statusAccent = Color(0xFF00E5A3),
                    actionAccent = Color(0xFF00D28E),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFF92A9A8),
                    textMuted = Color(0xFF5A7877),
                    auraColor = Color(0xFF00E5A3),
                    auraAlpha = 0.18f,
                    buttonOutlineBackground = Color(0xFF00E5A3).copy(alpha = 0.08f)
                )
            }
        } else {
            // Light Mode (Samsung Clock inspired crisp aesthetic with pastel aura)
            when (accent) {
                AccentTheme.FUSION -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFF8F9FD),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFE2E8F0),
                    cardBorderActive = Color(0xFF4C6EF5).copy(alpha = 0.5f),
                    primaryAccent = Color(0xFF4C6EF5),
                    statusAccent = Color(0xFF10B981),
                    actionAccent = Color(0xFFEF4444),
                    textPrimary = Color(0xFF0F172A),
                    textSecondary = Color(0xFF475569),
                    textMuted = Color(0xFF94A3B8),
                    auraColor = Color(0xFFC7D2FE), // Lavender pastel
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFF1F5F9)
                )
                AccentTheme.BLUE -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFF8F9FD),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFE2E8F0),
                    cardBorderActive = Color(0xFF3B82F6).copy(alpha = 0.5f),
                    primaryAccent = Color(0xFF3B82F6),
                    statusAccent = Color(0xFF3B82F6),
                    actionAccent = Color(0xFF2563EB),
                    textPrimary = Color(0xFF0F172A),
                    textSecondary = Color(0xFF475569),
                    textMuted = Color(0xFF94A3B8),
                    auraColor = Color(0xFFBFDBFE), // Ice pastel
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFEFF6FF)
                )
                AccentTheme.RED -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFFFF8F9),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFFCE7F3),
                    cardBorderActive = Color(0xFFE11D48).copy(alpha = 0.5f),
                    primaryAccent = Color(0xFFE11D48),
                    statusAccent = Color(0xFFE11D48),
                    actionAccent = Color(0xFFBE123C),
                    textPrimary = Color(0xFF1E1015),
                    textSecondary = Color(0xFF6F525E),
                    textMuted = Color(0xFF9E8590),
                    auraColor = Color(0xFFFECDD3), // Rose pastel
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFFFF1F2)
                )
                AccentTheme.MINT -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFF6FDF9),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFD1FAE5),
                    cardBorderActive = Color(0xFF059669).copy(alpha = 0.5f),
                    primaryAccent = Color(0xFF059669),
                    statusAccent = Color(0xFF059669),
                    actionAccent = Color(0xFF047857),
                    textPrimary = Color(0xFF062118),
                    textSecondary = Color(0xFF3F6557),
                    textMuted = Color(0xFF75978A),
                    auraColor = Color(0xFFA7F3D0), // Mint pastel
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFECFDF5)
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "buds_theme_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ACCENT_THEME = "key_accent_theme"

        @Volatile
        private var instance: ThemeStore? = null

        fun getInstance(context: Context): ThemeStore {
            return instance ?: synchronized(this) {
                instance ?: ThemeStore(context).also { instance = it }
            }
        }
    }
}

val LocalThemePalette = staticCompositionLocalOf<ThemePalette> {
    error("LocalThemePalette not provided")
}

object AppTheme {
    val palette: ThemePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalThemePalette.current
}


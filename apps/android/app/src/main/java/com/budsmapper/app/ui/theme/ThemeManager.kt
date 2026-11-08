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
    VIOLET("VIOLET", "Cyber Violet"),
    BLUE("BLUE", "Cyber Ice Blue"),
    RED("RED", "Hyper Crimson Red"),
    MINT("MINT", "Neon Cyber Mint"),
    FUSION("FUSION", "Fusion Synergy");

    companion object {
        fun fromId(id: String): AccentTheme = entries.find { it.id == id } ?: VIOLET
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
        AccentTheme.fromId(prefs.getString(KEY_ACCENT_THEME, AccentTheme.VIOLET.id) ?: AccentTheme.VIOLET.id)
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
                AccentTheme.VIOLET -> ThemePalette(
                    isDark = true,
                    backgroundColor = Color(0xFF0C0A14),
                    cardBackground = Color(0x99171224), // Frosted glass deep violet
                    cardBorder = Color(0x3847325D),
                    cardBorderActive = Color(0xFFA855F7).copy(alpha = 0.65f),
                    primaryAccent = Color(0xFFA855F7), // Neon Violet from icon
                    statusAccent = Color(0xFFA855F7),
                    actionAccent = Color(0xFFC084FC),
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFFA89BB8),
                    textMuted = Color(0xFF756A88),
                    auraColor = Color(0xFF8B5CF6), // Glowing violet breathing aura
                    auraAlpha = 0.24f,
                    buttonOutlineBackground = Color(0xFFA855F7).copy(alpha = 0.08f)
                )
                AccentTheme.BLUE -> ThemePalette(
                    isDark = true,
                    backgroundColor = Color(0xFF0B0E17),
                    cardBackground = Color(0x99131826),
                    cardBorder = Color(0x383F5278),
                    cardBorderActive = Color(0xFF5E81F4).copy(alpha = 0.65f),
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
                    backgroundColor = Color(0xFF0F0D15),
                    cardBackground = Color(0x99181322),
                    cardBorder = Color(0x384E3754),
                    cardBorderActive = Color(0xFFFF3B5C).copy(alpha = 0.65f),
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
                    backgroundColor = Color(0xFF091114),
                    cardBackground = Color(0x99101C1E),
                    cardBorder = Color(0x382B4F52),
                    cardBorderActive = Color(0xFF00E5A3).copy(alpha = 0.65f),
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
                AccentTheme.FUSION -> ThemePalette(
                    isDark = true,
                    backgroundColor = Color(0xFF0B0E17),
                    cardBackground = Color(0x99131826), // Frosted glass dark slate
                    cardBorder = Color(0x383F5278),
                    cardBorderActive = Color(0xFFA855F7).copy(alpha = 0.55f),
                    primaryAccent = Color(0xFFA855F7), // Neon Violet (Brand)
                    statusAccent = Color(0xFF00E5A3),  // Neon Mint (Status)
                    actionAccent = Color(0xFFFF3B5C),  // Hyper Crimson (Actions)
                    textPrimary = Color(0xFFFFFFFF),
                    textSecondary = Color(0xFF94A3B8),
                    textMuted = Color(0xFF64748B),
                    auraColor = Color(0xFF8B5CF6),
                    auraAlpha = 0.22f,
                    buttonOutlineBackground = Color(0xFFA855F7).copy(alpha = 0.08f)
                )
            }
        } else {
            // Light Mode (Frosted glass porcelain aesthetic with soft pastel aura)
            when (accent) {
                AccentTheme.VIOLET -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFF9F7FD),
                    cardBackground = Color(0x99FFFFFF), // 60% frosted glass
                    cardBorder = Color(0x55E9D5FF),
                    cardBorderActive = Color(0xFF9333EA).copy(alpha = 0.50f),
                    primaryAccent = Color(0xFF9333EA),
                    statusAccent = Color(0xFF9333EA),
                    actionAccent = Color(0xFF7E22CE),
                    textPrimary = Color(0xFF1E102E),
                    textSecondary = Color(0xFF5B456E),
                    textMuted = Color(0xFF8C799E),
                    auraColor = Color(0xFFDDD6FE),
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFFAF5FF)
                )
                AccentTheme.BLUE -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFF7F8FC),
                    cardBackground = Color(0x99FFFFFF), // 60% frosted glass
                    cardBorder = Color(0x55CBD5E1),
                    cardBorderActive = Color(0xFF3B82F6).copy(alpha = 0.50f),
                    primaryAccent = Color(0xFF3B82F6),
                    statusAccent = Color(0xFF3B82F6),
                    actionAccent = Color(0xFF2563EB),
                    textPrimary = Color(0xFF0F172A),
                    textSecondary = Color(0xFF475569),
                    textMuted = Color(0xFF94A3B8),
                    auraColor = Color(0xFFBFDBFE),
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFEFF6FF)
                )
                AccentTheme.RED -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFFFF7F8),
                    cardBackground = Color(0x99FFFFFF), // 60% frosted glass
                    cardBorder = Color(0x55FBCFE8),
                    cardBorderActive = Color(0xFFE11D48).copy(alpha = 0.50f),
                    primaryAccent = Color(0xFFE11D48),
                    statusAccent = Color(0xFFE11D48),
                    actionAccent = Color(0xFFBE123C),
                    textPrimary = Color(0xFF1E1015),
                    textSecondary = Color(0xFF6F525E),
                    textMuted = Color(0xFF9E8590),
                    auraColor = Color(0xFFFECDD3),
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFFFF1F2)
                )
                AccentTheme.MINT -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFF5FCF8),
                    cardBackground = Color(0x99FFFFFF), // 60% frosted glass
                    cardBorder = Color(0x55A7F3D0),
                    cardBorderActive = Color(0xFF059669).copy(alpha = 0.50f),
                    primaryAccent = Color(0xFF059669),
                    statusAccent = Color(0xFF059669),
                    actionAccent = Color(0xFF047857),
                    textPrimary = Color(0xFF062118),
                    textSecondary = Color(0xFF3F6557),
                    textMuted = Color(0xFF75978A),
                    auraColor = Color(0xFFA7F3D0),
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFECFDF5)
                )
                AccentTheme.FUSION -> ThemePalette(
                    isDark = false,
                    backgroundColor = Color(0xFFF7F8FC),
                    cardBackground = Color(0x99FFFFFF), // 60% frosted glass
                    cardBorder = Color(0x55CBD5E1),
                    cardBorderActive = Color(0xFF9333EA).copy(alpha = 0.50f),
                    primaryAccent = Color(0xFF9333EA), // Violet Brand
                    statusAccent = Color(0xFF10B981),  // Mint Status
                    actionAccent = Color(0xFFEF4444),  // Crimson Action
                    textPrimary = Color(0xFF0F172A),
                    textSecondary = Color(0xFF475569),
                    textMuted = Color(0xFF94A3B8),
                    auraColor = Color(0xFFDDD6FE),
                    auraAlpha = 0.35f,
                    buttonOutlineBackground = Color(0xFFF1F5F9)
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

package com.budsmapper.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budsmapper.app.ui.components.AppRootScaffold
import com.budsmapper.app.ui.components.CheckmarkIcon
import com.budsmapper.app.ui.components.DropletIcon
import com.budsmapper.app.ui.components.FlameIcon
import com.budsmapper.app.ui.components.GameSpaceCard
import com.budsmapper.app.ui.components.LeafIcon
import com.budsmapper.app.ui.components.MoonIcon
import com.budsmapper.app.ui.components.SunIcon
import com.budsmapper.app.ui.components.ZapIcon
import com.budsmapper.app.ui.theme.AccentTheme
import com.budsmapper.app.ui.theme.AppTheme
import com.budsmapper.app.ui.theme.ThemeMode
import com.budsmapper.app.ui.theme.ThemeStore

@Composable
fun SettingsScreen(
    themeStore: ThemeStore = ThemeStore.getInstance(LocalContext.current),
    onNavigateBack: () -> Unit
) {
    val palette = AppTheme.palette
    val scrollState = rememberScrollState()

    val currentMode by themeStore.themeMode.collectAsState()
    val currentAccent by themeStore.accentTheme.collectAsState()

    AppRootScaffold(
        title = "Settings",
        subtitle = "Appearance & Preferences",
        onBackClick = onNavigateBack
    ) { topPad, bottomPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = topPad, bottom = bottomPad)
        ) {
            // Card 1: Theme Mode (Dark vs Light - 50/50 Equal Height & Width Grid)
            GameSpaceCard {
                Text(
                    text = "Theme Appearance",
                    color = palette.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Switch between dark slate and bright porcelain themes",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeModeSelector(
                        title = "Dark Mode",
                        description = "Deep slate with dynamic aura",
                        isSelected = currentMode == ThemeMode.DARK,
                        icon = { MoonIcon(color = if (currentMode == ThemeMode.DARK) palette.primaryAccent else palette.textMuted) },
                        onClick = { themeStore.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    ThemeModeSelector(
                        title = "Light Mode",
                        description = "Crisp white with soft aura",
                        isSelected = currentMode == ThemeMode.LIGHT,
                        icon = { SunIcon(color = if (currentMode == ThemeMode.LIGHT) palette.primaryAccent else palette.textMuted) },
                        onClick = { themeStore.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 2: Accent Color Palette (Fusion, Blue, Red, Mint - Redundant badge removed)
            GameSpaceCard {
                Column {
                    Text(
                        text = "Color Accent",
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Ambient aura, buttons & indicator styling",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentRow(
                        accent = AccentTheme.FUSION,
                        label = "Fusion Synergy (Default)",
                        description = "Ice Blue (Brand) + Neon Mint (Status) + Crimson (Actions)",
                        swatchColor = Color(0xFF5E81F4),
                        icon = { ZapIcon(color = Color.White, size = 15.dp) },
                        isSelected = currentAccent == AccentTheme.FUSION,
                        onClick = { themeStore.setAccentTheme(AccentTheme.FUSION) }
                    )
                    AccentRow(
                        accent = AccentTheme.BLUE,
                        label = "Cyber Ice Blue",
                        description = "Icy periwinkle neon theme",
                        swatchColor = Color(0xFF5E81F4),
                        icon = { DropletIcon(color = Color.White, size = 15.dp) },
                        isSelected = currentAccent == AccentTheme.BLUE,
                        onClick = { themeStore.setAccentTheme(AccentTheme.BLUE) }
                    )
                    AccentRow(
                        accent = AccentTheme.RED,
                        label = "Hyper Crimson Red",
                        description = "Aggressive red gaming theme",
                        swatchColor = Color(0xFFFF3B5C),
                        icon = { FlameIcon(color = Color.White, size = 15.dp) },
                        isSelected = currentAccent == AccentTheme.RED,
                        onClick = { themeStore.setAccentTheme(AccentTheme.RED) }
                    )
                    AccentRow(
                        accent = AccentTheme.MINT,
                        label = "Neon Cyber Mint",
                        description = "Futuristic emerald teal theme",
                        swatchColor = Color(0xFF00E5A3),
                        icon = { LeafIcon(color = Color.White, size = 15.dp) },
                        isSelected = currentAccent == AccentTheme.MINT,
                        onClick = { themeStore.setAccentTheme(AccentTheme.MINT) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 3: About Buds Mapper
            GameSpaceCard {
                Text(
                    text = "Buds Mapper Engine",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Version 1.2.0 · Zero-Timer Dual Track Engine",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Configured exclusively for Samsung Galaxy Buds Core and physical devices running Android 14 (One UI 6.1). Features native sub-audible PCM priority hold and hardware gesture injection.",
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    title: String,
    description: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val borderColor = if (isSelected) palette.primaryAccent else palette.cardBorder
    val bgColor = if (isSelected) palette.primaryAccent.copy(alpha = 0.12f) else palette.cardBackground

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isSelected) palette.primaryAccent else palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = palette.textMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun AccentRow(
    accent: AccentTheme,
    label: String,
    description: String,
    swatchColor: Color,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = AppTheme.palette
    val borderColor = if (isSelected) palette.primaryAccent else palette.cardBorder
    val bgColor = if (isSelected) palette.primaryAccent.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (isSelected) palette.primaryAccent else palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = palette.textMuted,
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent),
                contentAlignment = Alignment.Center
            ) {
                CheckmarkIcon(color = Color.White, size = 12.dp)
            }
        }
    }
}

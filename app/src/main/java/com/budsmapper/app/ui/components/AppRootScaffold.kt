package com.budsmapper.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budsmapper.app.ui.theme.AppTheme

/**
 * Unified Root Scaffold for all screens in Buds Mapper.
 *
 * Provides:
 * 1. Samsung Clock-inspired smooth animated breathing aura (Dark & Light).
 * 2. Hardware dynamic status bar & navigation bar insets from Android.
 * 3. Top "Cloudy" borderless fade vignette mask under the status bar.
 * 4. Standard Samsung One UI back chevron (<) and typography.
 */
@Composable
fun AppRootScaffold(
    title: String? = null,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable (topPadding: androidx.compose.ui.unit.Dp, bottomPadding: androidx.compose.ui.unit.Dp) -> Unit
) {
    val palette = AppTheme.palette

    // Exact dynamic Android hardware insets
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Samsung Clock-inspired Ambient Breathing & Floating Aura Animation
    val infiniteTransition = rememberInfiniteTransition(label = "ambientAuraTransition")

    // Smooth breathing scale: 0.88x to 1.14x
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    // Gentle vertical floating: -20dp to +20dp
    val auraOffsetY by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraOffsetY"
    )

    // Subtle breathing opacity
    val auraAlphaMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundColor)
    ) {
        // 1. Samsung Clock-inspired Animated Ambient Aura Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val auraRadius = (size.width * 0.90f * auraScale).coerceAtLeast(10f)
            val centerY = size.height * 0.28f + (auraOffsetY * density)
            val center = Offset(size.width * 0.5f, centerY)

            val baseAlpha = (palette.auraAlpha * auraAlphaMultiplier).coerceIn(0.01f, 0.99f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.auraColor.copy(alpha = baseAlpha),
                        palette.auraColor.copy(alpha = baseAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = center
            )
        }

        // 2. Main Content Viewport
        val contentTopPadding = statusBarHeight + 14.dp
        val contentBottomPadding = navBarHeight + 20.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Optional Header (with One UI back chevron and title)
            if (title != null) {
                Spacer(modifier = Modifier.height(statusBarHeight + 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onBackClick),
                            contentAlignment = Alignment.Center
                        ) {
                            ChevronLeftIcon(
                                color = palette.textPrimary,
                                size = 22.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = palette.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                color = palette.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (actions != null) {
                        actions()
                    }
                }
            }

            // Screen content
            Box(modifier = Modifier.fillMaxSize()) {
                val effectiveTop = if (title != null) 8.dp else contentTopPadding
                content(effectiveTop, contentBottomPadding)
            }
        }
    }
}


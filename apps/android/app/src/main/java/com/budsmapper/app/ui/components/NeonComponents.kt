package com.budsmapper.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budsmapper.app.R
import com.budsmapper.app.ui.theme.AppTheme

// ==========================================
// LUCIDE-STYLE MINIMALIST VECTOR ICONS
// ==========================================

/**
 * Pixel-perfect Samsung One UI style Chevron (<) vector icon
 */
@Composable
fun ChevronLeftIcon(
    color: Color = AppTheme.palette.textPrimary,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.4.dp.toPx()
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.65f, this@Canvas.size.height * 0.18f)
            lineTo(this@Canvas.size.width * 0.32f, this@Canvas.size.height * 0.50f)
            lineTo(this@Canvas.size.width * 0.65f, this@Canvas.size.height * 0.82f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Lucide-style Search Icon
 */
@Composable
fun SearchIcon(
    color: Color = AppTheme.palette.textMuted,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val radius = this@Canvas.size.width * 0.35f
        val center = Offset(this@Canvas.size.width * 0.42f, this@Canvas.size.height * 0.42f)
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = color,
            start = Offset(this@Canvas.size.width * 0.66f, this@Canvas.size.height * 0.66f),
            end = Offset(this@Canvas.size.width * 0.90f, this@Canvas.size.height * 0.90f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Lucide-style Flask / Beaker Icon (for Test & Monitor)
 */
@Composable
fun FlaskIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height

        val path = Path().apply {
            moveTo(w * 0.38f, h * 0.15f)
            lineTo(w * 0.62f, h * 0.15f)
            moveTo(w * 0.42f, h * 0.15f)
            lineTo(w * 0.42f, h * 0.40f)
            lineTo(w * 0.18f, h * 0.82f)
            quadraticTo(w * 0.15f, h * 0.88f, w * 0.24f, h * 0.88f)
            lineTo(w * 0.76f, h * 0.88f)
            quadraticTo(w * 0.85f, h * 0.88f, w * 0.82f, h * 0.82f)
            lineTo(w * 0.58f, h * 0.40f)
            lineTo(w * 0.58f, h * 0.15f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Liquid level line
        drawLine(
            color = color,
            start = Offset(w * 0.30f, h * 0.65f),
            end = Offset(w * 0.70f, h * 0.65f),
            strokeWidth = strokeWidth * 0.8f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Lucide-style Sliders / Settings Icon
 */
@Composable
fun SlidersIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height

        // Line 1
        drawLine(color, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), strokeWidth, StrokeCap.Round)
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.40f, h * 0.28f), style = Stroke(strokeWidth))

        // Line 2
        drawLine(color, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), strokeWidth, StrokeCap.Round)
        drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.65f, h * 0.72f), style = Stroke(strokeWidth))
    }
}

/**
 * Lucide-style Moon Icon (Dark Mode)
 */
@Composable
fun MoonIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_lucide_moon),
        contentDescription = "Moon",
        tint = color,
        modifier = modifier.size(size)
    )
}

/**
 * Lucide-style Sparkle / Star Icon (Cyber Violet Theme)
 */
@Composable
fun SparkleIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 15.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 1.8.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        val path = Path().apply {
            moveTo(cx, h * 0.12f)
            quadraticTo(cx, cy, w * 0.88f, cy)
            quadraticTo(cx, cy, cx, h * 0.88f)
            quadraticTo(cx, cy, w * 0.12f, cy)
            quadraticTo(cx, cy, cx, h * 0.12f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Lucide-style Sun Icon (Light Mode)
 */
@Composable
fun SunIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height
        val center = Offset(w * 0.5f, h * 0.5f)

        // Center Sun disc
        drawCircle(color, radius = w * 0.22f, center = center, style = Stroke(strokeWidth))

        // 8 Rays
        val rayInner = w * 0.34f
        val rayOuter = w * 0.46f

        val angles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
        for (a in angles) {
            val rad = Math.toRadians(a)
            val start = Offset(
                center.x + (rayInner * Math.cos(rad)).toFloat(),
                center.y + (rayInner * Math.sin(rad)).toFloat()
            )
            val end = Offset(
                center.x + (rayOuter * Math.cos(rad)).toFloat(),
                center.y + (rayOuter * Math.sin(rad)).toFloat()
            )
            drawLine(color, start, end, strokeWidth, StrokeCap.Round)
        }
    }
}

/**
 * Lucide-style Zap / Sparkle Icon (Fusion Theme)
 */
@Composable
fun ZapIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height

        val path = Path().apply {
            moveTo(w * 0.55f, h * 0.12f)
            lineTo(w * 0.25f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.88f)
            lineTo(w * 0.75f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.48f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Lucide-style Droplet Icon (Cyber Blue Theme)
 */
@Composable
fun DropletIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height

        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.15f)
            quadraticTo(w * 0.82f, h * 0.55f, w * 0.75f, h * 0.72f)
            quadraticTo(w * 0.65f, h * 0.88f, w * 0.50f, h * 0.88f)
            quadraticTo(w * 0.35f, h * 0.88f, w * 0.25f, h * 0.72f)
            quadraticTo(w * 0.18f, h * 0.55f, w * 0.50f, h * 0.15f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Lucide-style Flame Icon (Hyper Red Theme)
 */
@Composable
fun FlameIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_lucide_flame),
        contentDescription = "Flame",
        tint = color,
        modifier = modifier.size(size)
    )
}

/**
 * Lucide-style Leaf Icon (Cyber Mint Theme)
 */
@Composable
fun LeafIcon(
    color: Color = AppTheme.palette.primaryAccent,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height

        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.85f)
            quadraticTo(w * 0.15f, h * 0.40f, w * 0.75f, h * 0.15f)
            quadraticTo(w * 0.85f, h * 0.60f, w * 0.45f, h * 0.85f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Center vein
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.83f),
            end = Offset(w * 0.60f, h * 0.42f),
            strokeWidth = strokeWidth * 0.8f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Lucide-style Checkmark Icon
 */
@Composable
fun CheckmarkIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.2.dp.toPx()
        val w = this@Canvas.size.width
        val h = this@Canvas.size.height
        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.45f, h * 0.78f)
            lineTo(w * 0.85f, h * 0.25f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ==========================================
// FROSTED GLASS CARDS & UI COMPONENTS
// ==========================================

/**
 * Frosted Glass Card with translucent blur backdrop and 20dp corners
 */
@Composable
fun GameSpaceCard(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AppTheme.palette
    val borderColor = if (isActive) palette.cardBorderActive else palette.cardBorder

    val cardModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    } else {
        modifier.clip(RoundedCornerShape(20.dp))
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        color = palette.cardBackground,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

/**
 * Outlined Neon Button (No solid heavy fill - sleek cyberpunk styling)
 */
@Composable
fun NeonOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AppTheme.palette.primaryAccent,
    trailingText: String? = "→"
) {
    val palette = AppTheme.palette
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = palette.buttonOutlineBackground,
        border = BorderStroke(1.5.dp, accentColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = if (palette.isDark) accentColor else palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (trailingText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    color = accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Status / Action Neon Badge
 */
@Composable
fun NeonBadge(
    text: String,
    color: Color = AppTheme.palette.statusAccent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Polished Search Bar with M3 Rounded Shape and Search Icon
 */
@Composable
fun PolishedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = palette.textMuted,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            SearchIcon(color = palette.textMuted)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = palette.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.cardBackground,
            unfocusedContainerColor = palette.cardBackground,
            focusedBorderColor = palette.primaryAccent,
            unfocusedBorderColor = palette.cardBorder,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            cursorColor = palette.primaryAccent
        )
    )
}

/**
 * Compact Modern Switch (Scaled down by ~20% so it is sleek and not oversized)
 */
@Composable
fun NeonSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AppTheme.palette.statusAccent
) {
    val palette = AppTheme.palette
    Box(modifier = modifier.scale(0.80f)) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = palette.textMuted,
                uncheckedTrackColor = palette.cardBorder.copy(alpha = 0.6f),
                uncheckedBorderColor = palette.cardBorder
            )
        )
    }
}

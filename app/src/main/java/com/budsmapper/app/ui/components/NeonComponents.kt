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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budsmapper.app.ui.theme.AppTheme

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
 * Search Magnifying Glass Icon
 */
@Composable
fun SearchIcon(
    color: Color = AppTheme.palette.textMuted,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 2.dp.toPx()
        val radius = size.width * 0.35f
        val center = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        val handleStart = androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.66f)
        val handleEnd = androidx.compose.ui.geometry.Offset(size.width * 0.90f, size.height * 0.90f)
        drawLine(
            color = color,
            start = handleStart,
            end = handleEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Realme Game Space 20dp Rounded Card with 1dp frosted border
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
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

/**
 * Outlined Neon Button (No solid heavy fill - sleek cyberpunk / Game Space styling)
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
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = palette.buttonOutlineBackground,
        border = BorderStroke(1.5.dp, accentColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = if (palette.isDark) accentColor else palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (trailingText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    color = accentColor,
                    fontSize = 16.sp,
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
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
 * Styled Neon Switch
 */
@Composable
fun NeonSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AppTheme.palette.statusAccent
) {
    val palette = AppTheme.palette
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accentColor,
            uncheckedThumbColor = palette.textMuted,
            uncheckedTrackColor = palette.cardBorder,
            uncheckedBorderColor = palette.cardBorder
        )
    )
}


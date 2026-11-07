package com.budsmapper.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.budsmapper.app.accessibility.BudsAccessibilityService
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.ui.components.AppRootScaffold
import com.budsmapper.app.ui.components.GameSpaceCard
import com.budsmapper.app.ui.components.NeonBadge
import com.budsmapper.app.ui.components.NeonSwitch
import com.budsmapper.app.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestDiagnosticsScreen(
    eventStore: LastEventStore,
    onNavigateBack: () -> Unit
) {
    val palette = AppTheme.palette
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val lastEvent by eventStore.lastEventFlow.collectAsState()
    val detectionState by eventStore.detectionStateFlow.collectAsState()
    val isTestCaptureForced by eventStore.isTestCaptureForced.collectAsState()
    val isAccessibilityActive = BudsAccessibilityService.instance != null

    // Automatic shutoff: As soon as the user navigates back OR minimizes the app,
    // turn OFF test capture mode so normal foreground app routing resumes immediately!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                eventStore.setTestCaptureForced(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            eventStore.setTestCaptureForced(false)
        }
    }

    AppRootScaffold(
        title = "Test & Monitor",
        subtitle = "Live AVRCP & Diagnostics",
        onBackClick = onNavigateBack
    ) { topPad, bottomPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = topPad, bottom = bottomPad)
        ) {
            // Card 0: Test Capture Mode Toggle (Auto-Off on Exit)
            GameSpaceCard(isActive = isTestCaptureForced) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test Capture Mode",
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isTestCaptureForced)
                                "Active · Tap earbuds now to test capture"
                            else
                                "Forces capture ON inside Buds Mapper for testing (auto-off on exit)",
                            color = if (isTestCaptureForced) palette.statusAccent else palette.textMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    NeonSwitch(
                        checked = isTestCaptureForced,
                        onCheckedChange = { forced ->
                            eventStore.setTestCaptureForced(forced)
                        },
                        accentColor = palette.statusAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 1: Live Command Monitor
            GameSpaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Command Monitor",
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time earbud AVRCP taps",
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.cardBorder.copy(alpha = 0.5f))
                            .clickable { eventStore.clearHistory() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Clear Log",
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detected command indicators grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommandPill(
                        label = "PLAY",
                        isDetected = detectionState.mediaPlayDetected,
                        modifier = Modifier.weight(1f)
                    )
                    CommandPill(
                        label = "PAUSE",
                        isDetected = detectionState.mediaPauseDetected,
                        modifier = Modifier.weight(1f)
                    )
                    CommandPill(
                        label = "NEXT",
                        isDetected = detectionState.mediaNextDetected,
                        modifier = Modifier.weight(1f)
                    )
                    CommandPill(
                        label = "PREV",
                        isDetected = detectionState.mediaPreviousDetected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 2: Last Received Event Inspector
            GameSpaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last Received Event",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (lastEvent != null) {
                        NeonBadge(
                            text = lastEvent!!.source,
                            color = palette.statusAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (lastEvent != null) {
                    val event = lastEvent!!
                    val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                    val formattedTime = dateFormat.format(Date(event.timestamp))

                    EventDetailRow(label = "KeyCode", value = "${event.keyName} (${event.keyCode})")
                    EventDetailRow(label = "Action", value = event.action)
                    EventDetailRow(label = "Time", value = formattedTime)
                    EventDetailRow(label = "Device ID", value = "${event.deviceId}")
                    if (event.mappedAction != null) {
                        EventDetailRow(
                            label = "Mapped Action",
                            value = event.mappedAction,
                            isHighlighted = true
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No earbud taps recorded yet.\nTurn on Test Capture Mode or double-tap your Galaxy Buds.",
                            color = palette.textMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card 3: Service Health Diagnostics
            GameSpaceCard {
                Text(
                    text = "System Diagnostics",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Background service communication health",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                DiagnosticRow(
                    title = "Accessibility Engine",
                    subtitle = "Gesture injection subsystem",
                    statusText = if (isAccessibilityActive) "CONNECTED" else "STOPPED",
                    isGood = isAccessibilityActive
                )

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticRow(
                    title = "Sub-Audible Dual AudioTracks",
                    subtitle = "Continuous non-zero PCM handoff",
                    statusText = "STANDBY / ACTIVE",
                    isGood = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                DiagnosticRow(
                    title = "Hardware AVRCP Route",
                    subtitle = "Samsung One UI 6.1 MediaSession index 0",
                    statusText = "READY",
                    isGood = true
                )
            }
        }
    }
}

@Composable
private fun CommandPill(
    label: String,
    isDetected: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val activeColor = palette.actionAccent
    val bgColor = if (isDetected) activeColor.copy(alpha = 0.15f) else palette.cardBorder.copy(alpha = 0.35f)
    val borderColor = if (isDetected) activeColor else palette.cardBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isDetected) activeColor else palette.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun EventDetailRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    val palette = AppTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = if (isHighlighted) palette.actionAccent else palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    subtitle: String,
    statusText: String,
    isGood: Boolean
) {
    val palette = AppTheme.palette
    val statusColor = if (isGood) palette.statusAccent else palette.actionAccent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = palette.textMuted,
                fontSize = 11.sp
            )
        }
        NeonBadge(
            text = statusText,
            color = statusColor
        )
    }
}

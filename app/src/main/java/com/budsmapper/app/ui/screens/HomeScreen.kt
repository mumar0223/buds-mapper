package com.budsmapper.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.budsmapper.app.data.CommandDetectionState
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.data.RecordedKeyEvent
import com.budsmapper.app.ui.theme.DarkBackground
import com.budsmapper.app.ui.theme.DarkBorder
import com.budsmapper.app.ui.theme.DarkSurface
import com.budsmapper.app.ui.theme.DarkSurfaceVariant
import com.budsmapper.app.ui.theme.PrimaryBlue
import com.budsmapper.app.ui.theme.StatusInactive
import com.budsmapper.app.ui.theme.StatusInactiveContainer
import com.budsmapper.app.ui.theme.StatusSuccess
import com.budsmapper.app.ui.theme.StatusSuccessContainer
import com.budsmapper.app.ui.theme.TextPrimary
import com.budsmapper.app.ui.theme.TextSecondary
import com.budsmapper.app.ui.theme.TextTertiary
import com.budsmapper.app.util.AccessibilityUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    eventStore: LastEventStore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isServiceEnabled by remember {
        mutableStateOf(AccessibilityUtils.isServiceEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = AccessibilityUtils.isServiceEnabled(context)
                eventStore.reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val lastEvent by eventStore.lastEventFlow.collectAsState()
    val detectionState by eventStore.detectionStateFlow.collectAsState()
    val isMediaSessionCaptureEnabled by eventStore.mediaSessionCaptureEnabled.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // HEADER
            HeaderSection(
                isServiceEnabled = isServiceEnabled,
                isSessionActive = isMediaSessionCaptureEnabled
            )

            // MEDIASESSION CAPTURE MODE CARD (MILESTONE 2)
            MediaSessionCaptureCard(
                isCaptureEnabled = isMediaSessionCaptureEnabled,
                onToggleCapture = { enabled -> eventStore.setMediaSessionCaptureEnabled(enabled) }
            )

            // SERVICE CARD (ACCESSIBILITY SERVICE)
            ServiceCard(
                isServiceEnabled = isServiceEnabled,
                onEnableClick = { AccessibilityUtils.openAccessibilitySettings(context) }
            )

            // EARBUD COMMAND MONITOR
            CommandMonitorSection(detectionState = detectionState)

            // LAST RECEIVED EVENT
            LastEventSection(lastEvent = lastEvent)

            // DIAGNOSTIC SECTION
            DiagnosticStatusSection(
                isServiceEnabled = isServiceEnabled,
                isSessionActive = isMediaSessionCaptureEnabled,
                lastEvent = lastEvent
            )

            // FOOTER
            FooterSection()
        }
    }
}

@Composable
private fun HeaderSection(isServiceEnabled: Boolean, isSessionActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ᛒ",
                    color = PrimaryBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = "Buds Mapper",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "AVRCP & MediaSession Bridge",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        StatusBadge(
            label = if (isSessionActive) "SESSION ACTIVE" else if (isServiceEnabled) "ACCESSIBILITY ON" else "OFFLINE",
            isActive = isSessionActive || isServiceEnabled
        )
    }
}

@Composable
private fun MediaSessionCaptureCard(
    isCaptureEnabled: Boolean,
    onToggleCapture: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCaptureEnabled) PrimaryBlue.copy(alpha = 0.6f) else DarkBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MediaSession Capture Mode",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isCaptureEnabled) "Active • Listening for Bluetooth AVRCP" else "Inactive • Released to system",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCaptureEnabled) StatusSuccess else TextSecondary
                    )
                }

                Switch(
                    checked = isCaptureEnabled,
                    onCheckedChange = onToggleCapture,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryBlue,
                        uncheckedThumbColor = StatusInactive,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Text(
                text = "Activates a native Android MediaSession to claim media-key priority for wireless earbud gestures. When active, earbud taps (single/double/triple) are intercepted and consumed here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "⚠️ Notice: While capture mode is ON, earbud gestures may temporarily stop controlling YouTube or Spotify while we verify priority routing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    isServiceEnabled: Boolean,
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accessibility Service",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isServiceEnabled) StatusSuccessContainer else StatusInactiveContainer
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isServiceEnabled) "Connected" else "Disabled",
                        color = if (isServiceEnabled) StatusSuccess else StatusInactive,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Keeps the diagnostic service alive in the background and intercepts physical phone hardware buttons (Volume Up/Down).",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Button(
                onClick = onEnableClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceEnabled) DarkSurfaceVariant else PrimaryBlue,
                    contentColor = if (isServiceEnabled) TextPrimary else Color.White
                )
            ) {
                Text(
                    text = if (isServiceEnabled) "Open Accessibility Settings" else "Enable Service in Settings",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CommandMonitorSection(detectionState: CommandDetectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Earbud Command Monitor",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Text(
                text = "Target AVRCP commands verified via Bluetooth HCI snoop:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            CommandRow(
                commandName = "MEDIA_PLAY",
                detected = detectionState.mediaPlayDetected,
                timestamp = detectionState.mediaPlayTimestamp
            )
            CommandRow(
                commandName = "MEDIA_PAUSE",
                detected = detectionState.mediaPauseDetected,
                timestamp = detectionState.mediaPauseTimestamp
            )
            CommandRow(
                commandName = "MEDIA_NEXT",
                detected = detectionState.mediaNextDetected,
                timestamp = detectionState.mediaNextTimestamp
            )
            CommandRow(
                commandName = "MEDIA_PREVIOUS",
                detected = detectionState.mediaPreviousDetected,
                timestamp = detectionState.mediaPreviousTimestamp
            )
        }
    }
}

@Composable
private fun CommandRow(
    commandName: String,
    detected: Boolean,
    timestamp: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = commandName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            if (detected && timestamp > 0) {
                Text(
                    text = "Detected at ${formatTimestamp(timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (detected) StatusSuccessContainer else StatusInactiveContainer
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (detected) "Detected" else "Not detected",
                color = if (detected) StatusSuccess else StatusInactive,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LastEventSection(lastEvent: RecordedKeyEvent?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last Received Event",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                if (lastEvent != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (lastEvent.source == "MediaSession") PrimaryBlue.copy(alpha = 0.2f)
                                else DarkSurfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = lastEvent.source,
                            color = if (lastEvent.source == "MediaSession") PrimaryBlue else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (lastEvent == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events recorded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventDataRow(label = "Command / Key", value = "${lastEvent.keyName} (${lastEvent.keyCode})")
                    EventDataRow(label = "Action", value = lastEvent.action)
                    EventDataRow(label = "Timestamp", value = formatTimestamp(lastEvent.timestamp))
                    EventDataRow(label = "Device ID", value = "${lastEvent.deviceId}")
                    EventDataRow(label = "Received Via", value = lastEvent.source)
                }
            }
        }
    }
}

@Composable
private fun EventDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DiagnosticStatusSection(
    isServiceEnabled: Boolean,
    isSessionActive: Boolean,
    lastEvent: RecordedKeyEvent?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Diagnostic Status",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            val statusMessage = when {
                isSessionActive -> "MediaSession Capture Mode is ACTIVE. Buds Mapper has registered a native MediaSession to claim media-key priority. Tap your Galaxy Buds Core (single/double/triple tap) to test AVRCP reception."
                !isServiceEnabled -> "Background AccessibilityService is disabled. Enable it in Settings to keep background services alive."
                lastEvent != null && lastEvent.source == "AccessibilityService" -> "AccessibilityService is active and capturing physical device buttons (e.g. Volume keys). Enable MediaSession Capture Mode above to test Bluetooth earbud AVRCP commands."
                else -> "Ready. Enable MediaSession Capture Mode to intercept Bluetooth earbud gestures."
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "• Physical hardware buttons route via AccessibilityService.\n• Bluetooth AVRCP commands route via MediaSession.\n• Dual-pipeline diagnostic harness active.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Buds Mapper v1.1.0 — Milestone 2 MediaSession",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}

@Composable
private fun StatusBadge(label: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) StatusSuccessContainer else StatusInactiveContainer)
            .border(
                1.dp,
                if (isActive) StatusSuccess.copy(alpha = 0.5f) else StatusInactive.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) StatusSuccess else StatusInactive,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis == 0L) return "Never"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}

package com.budsmapper.app.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
            HeaderSection(isServiceEnabled = isServiceEnabled)

            // SERVICE CARD
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
                hasEvents = lastEvent != null
            )

            // FOOTER
            FooterSection()
        }
    }
}

@Composable
private fun HeaderSection(isServiceEnabled: Boolean) {
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ᛒ",
                    color = PrimaryBlue,
                    fontSize = 18.sp,
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
                    text = "AVRCP Diagnostic Bridge",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        StatusBadge(
            label = if (isServiceEnabled) "SERVICE ACTIVE" else "SERVICE INACTIVE",
            isActive = isServiceEnabled
        )
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
                        text = if (isServiceEnabled) "Enabled" else "Disabled",
                        color = if (isServiceEnabled) StatusSuccess else StatusInactive,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Android requires this service with key-event filtering enabled so incoming Bluetooth earbud AVRCP commands can be captured without consuming normal media playback.",
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
                    text = "First detected: ${formatTimestamp(timestamp)}",
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
            Text(
                text = "Last Received Event",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

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
                        text = "No key events recorded yet",
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
    hasEvents: Boolean
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
                !isServiceEnabled -> "Background AccessibilityService is disabled. Enable the service in Android Settings to begin capturing hardware KeyEvents."
                !hasEvents -> "AccessibilityService is active and listening. No Bluetooth media commands have been detected yet. Connect Galaxy Buds Core, trigger a gesture in a media app (e.g. YouTube), and check here."
                else -> "KeyEvents successfully intercepted! Check the monitor above to confirm whether Samsung Galaxy Buds Core AVRCP maps into standard KeyEvents."
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "• Event delivery is tested live on physical hardware.\n• Service operates independently of this UI.\n• Events are forwarded without consumption (return false).",
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
            text = "Buds Mapper v1.0.0",
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


package com.budsmapper.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import com.budsmapper.app.data.AppProfileStore
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
    profileStore: AppProfileStore,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isServiceEnabled by remember {
        mutableStateOf(AccessibilityUtils.isServiceEnabled(context))
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            eventStore.setStatusNotificationEnabled(true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = AccessibilityUtils.isServiceEnabled(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
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
    val isStatusNotificationEnabled by eventStore.statusNotificationEnabled.collectAsState()
    val foregroundPackage by eventStore.currentForegroundPackage.collectAsState()
    val foregroundLabel by eventStore.currentForegroundLabel.collectAsState()
    val enabledPackages by profileStore.enabledPackages.collectAsState()

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

            // APPLICATION PROFILES CARD
            ApplicationProfilesCard(
                activeCount = enabledPackages.size,
                onManageProfilesClick = onNavigateToProfiles
            )

            // STATUS NOTIFICATION CARD (SPOTIFY STYLE COMPACT)
            StatusNotificationCard(
                isNotificationEnabled = isStatusNotificationEnabled,
                hasPermission = hasNotificationPermission,
                currentApp = foregroundLabel,
                onToggleNotification = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        eventStore.setStatusNotificationEnabled(enabled)
                    }
                }
            )

            // ACTIVE FOREGROUND APP CARD
            ForegroundAppCard(
                foregroundLabel = foregroundLabel,
                foregroundPackage = foregroundPackage
            )

            // SMART GESTURE ENGINE (AUTO-ENGAGE)
            MediaSessionCaptureCard(
                isCaptureEnabled = isMediaSessionCaptureEnabled,
                currentApp = foregroundLabel
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
                    text = "Silent Background Add-on",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        StatusBadge(
            label = if (isSessionActive) "CAPTURE ON" else if (isServiceEnabled) "SERVICE READY" else "OFFLINE",
            isActive = isSessionActive || isServiceEnabled
        )
    }
}

@Composable
private fun ApplicationProfilesCard(
    activeCount: Int,
    onManageProfilesClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onManageProfilesClick() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📱", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Application Profiles",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (activeCount > 0) "$activeCount profile${if (activeCount == 1) "" else "s"} active" else "No profiles enabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activeCount > 0) StatusSuccess else TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeCount > 0) StatusSuccessContainer else StatusInactiveContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (activeCount > 0) "ACTIVE" else "OFF",
                        color = if (activeCount > 0) StatusSuccess else StatusInactive,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Choose which installed applications have custom earbud gesture profiles and configure mappings for each.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Button(
                onClick = onManageProfilesClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Manage Application Profiles →",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatusNotificationCard(
    isNotificationEnabled: Boolean,
    hasPermission: Boolean,
    currentApp: String,
    onToggleNotification: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isNotificationEnabled && hasPermission) PrimaryBlue.copy(alpha = 0.8f) else DarkBorder
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
                        text = "Compact Status Notification",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isNotificationEnabled && hasPermission) "Active • Stays visible across Clear All" else "Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNotificationEnabled && hasPermission) StatusSuccess else TextSecondary
                    )
                }

                Switch(
                    checked = isNotificationEnabled && hasPermission,
                    onCheckedChange = onToggleNotification,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryBlue,
                        uncheckedThumbColor = StatusInactive,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Text(
                text = "Spotify-style background artwork card in the compact screenshot notification height (~64dp). Dynamically displays the foreground application you are currently using.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // Visual mini-preview of the compact notification card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
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
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "ᛒ", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "Buds Mapper", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Currently using $currentApp", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }

                    Text(text = "64dp COMPACT", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ForegroundAppCard(
    foregroundLabel: String,
    foregroundPackage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Foreground App",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = foregroundLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = foregroundPackage,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ACCESSIBILITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MediaSessionCaptureCard(
    isCaptureEnabled: Boolean,
    currentApp: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCaptureEnabled) PrimaryBlue.copy(alpha = 0.8f) else DarkBorder
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
                        text = "Smart Gesture Engine",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isCaptureEnabled) "Auto-Active in $currentApp" else "Standby · Ready for enabled apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCaptureEnabled) StatusSuccess else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCaptureEnabled) StatusSuccessContainer else StatusInactiveContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCaptureEnabled) "ACTIVE" else "STANDBY",
                        color = if (isCaptureEnabled) StatusSuccess else StatusInactive,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Automatically engages when an enabled app (e.g. YouTube) is in the foreground to remap earbud gestures to screen actions. Releases instantly for Spotify and unmapped apps with zero interference.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
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
                text = "Bound by the Android OS framework. Operates silently in the background like a native add-on and keeps services alive across 'Clear All'.",
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
                text = "Verified Bluetooth AVRCP commands from Galaxy Buds Core:",
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
                    if (lastEvent.mappedAction != null) {
                        EventDataRow(label = "Assigned Action", value = lastEvent.mappedAction)
                    }
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
                isSessionActive -> "MediaSession Priority Engine is ACTIVE. Buds Mapper holds priority on MediaSessionStack. Earbud taps route directly to Buds Mapper."
                !isServiceEnabled -> "Background AccessibilityService is disabled. Enable it in Settings to keep background services alive."
                lastEvent != null && lastEvent.source == "AccessibilityService" -> "AccessibilityService is active and capturing physical device buttons (e.g. Volume keys)."
                else -> "Ready. Enable MediaSession Priority Engine to intercept Bluetooth earbud gestures."
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "• Operates silently in the background as a native Android add-on.\n• Notification stays active across 'Clear All'.\n• 0.0% CPU overhead at idle.",
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
            text = "Buds Mapper v1.3.0 — Milestone 3 Native Add-on",
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

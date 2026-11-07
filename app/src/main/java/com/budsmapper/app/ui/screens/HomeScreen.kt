package com.budsmapper.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.budsmapper.app.accessibility.BudsAccessibilityService
import com.budsmapper.app.data.AppProfileStore
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.ui.components.AppRootScaffold
import com.budsmapper.app.ui.components.GameSpaceCard
import com.budsmapper.app.ui.components.NeonBadge
import com.budsmapper.app.ui.components.NeonOutlinedButton
import com.budsmapper.app.ui.components.NeonSwitch
import com.budsmapper.app.ui.theme.AppTheme
import com.budsmapper.app.util.AccessibilityUtils

@Composable
fun HomeScreen(
    eventStore: LastEventStore,
    profileStore: AppProfileStore,
    onNavigateToProfiles: () -> Unit,
    onNavigateToTest: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = AppTheme.palette
    val scrollState = rememberScrollState()

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
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isMasterEnabled by eventStore.isMasterEnabled.collectAsState()
    val enabledProfiles by profileStore.enabledPackages.collectAsState()
    val statusNotificationEnabled by eventStore.statusNotificationEnabled.collectAsState()
    val currentForegroundLabel by eventStore.currentForegroundLabel.collectAsState()
    val currentForegroundPackage by eventStore.currentForegroundPackage.collectAsState()

    AppRootScaffold(
        title = "Buds Mapper",
        subtitle = "Galaxy Buds Gesture Remapper",
        actions = {
            // Master Engine ON/OFF Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isMasterEnabled) palette.statusAccent.copy(alpha = 0.12f)
                        else palette.cardBorder.copy(alpha = 0.5f)
                    )
                    .border(
                        1.dp,
                        if (isMasterEnabled) palette.statusAccent else palette.cardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { eventStore.setMasterEnabled(!isMasterEnabled) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isMasterEnabled) palette.statusAccent else palette.textMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMasterEnabled) "ACTIVE" else "PAUSED",
                    color = if (isMasterEnabled) palette.statusAccent else palette.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    ) { topPad, bottomPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = topPad, bottom = bottomPad)
        ) {
            // Accessibility Permission Banner if not enabled
            if (!isServiceEnabled) {
                GameSpaceCard(isActive = true) {
                    Text(
                        text = "⚠️ Accessibility Service Required",
                        color = palette.actionAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Buds Mapper needs accessibility permission to simulate swipe gestures on screen when you tap your earbuds.",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    NeonOutlinedButton(
                        text = "Enable in Android Settings",
                        onClick = { AccessibilityUtils.openAccessibilitySettings(context) },
                        accentColor = palette.actionAccent
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Card 1: Application Profiles (Hero Feature)
            GameSpaceCard(isActive = isMasterEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Application Profiles",
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${enabledProfiles.size} custom profiles enabled",
                            color = if (isMasterEnabled) palette.statusAccent else palette.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    NeonBadge(
                        text = if (isMasterEnabled && enabledProfiles.isNotEmpty()) "MONITORING" else "STANDBY",
                        color = if (isMasterEnabled && enabledProfiles.isNotEmpty()) palette.statusAccent else palette.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Configure custom earbud tap gestures (Swipe Up, Scroll Down, etc.) for YouTube, Instagram, and your favorite apps.",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Outlined Neon Button (No heavy solid fill!)
                NeonOutlinedButton(
                    text = "Manage Application Profiles",
                    onClick = onNavigateToProfiles,
                    accentColor = palette.primaryAccent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Compact Status Notification
            GameSpaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compact Status Notification",
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (statusNotificationEnabled) "Active · Displays foreground app" else "Disabled",
                            color = if (statusNotificationEnabled) palette.statusAccent else palette.textMuted,
                            fontSize = 12.sp
                        )
                    }

                    NeonSwitch(
                        checked = statusNotificationEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (hasNotificationPermission) {
                                    eventStore.setStatusNotificationEnabled(true)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    eventStore.setStatusNotificationEnabled(true)
                                }
                            } else {
                                eventStore.setStatusNotificationEnabled(false)
                            }
                        },
                        accentColor = palette.statusAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3: Current Foreground App Detection
            GameSpaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Foreground App",
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentForegroundLabel,
                            color = palette.primaryAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentForegroundPackage,
                            color = palette.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    val isForegroundProfileEnabled = enabledProfiles.contains(currentForegroundPackage)
                    NeonBadge(
                        text = if (isForegroundProfileEnabled) "MAPPED" else "PASSTHROUGH",
                        color = if (isForegroundProfileEnabled) palette.actionAccent else palette.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-Column Grid: 🧪 Test & Diagnostics | ⚙️ Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Test & Monitor
                GameSpaceCard(
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTest
                ) {
                    Text(
                        text = "🧪",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Test & Monitor",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Live AVRCP & logs",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Open Monitor →",
                        color = palette.primaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Column 2: Settings
                GameSpaceCard(
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSettings
                ) {
                    Text(
                        text = "⚙️",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Settings",
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Themes & tuning",
                        color = palette.textSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Open Settings →",
                        color = palette.primaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

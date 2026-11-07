package com.budsmapper.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budsmapper.app.R
import com.budsmapper.app.data.AppProfileStore
import com.budsmapper.app.data.InstalledApp
import com.budsmapper.app.data.InstalledAppsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val DarkBackground = Color(0xFF0F1117)
private val CardBackground = Color(0xFF161A24)
private val CardBorderColor = Color(0xFF262C3D)
private val PrimaryAccent = Color(0xFF4C8DFF)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9CA3AF)
private val TextGreen = Color(0xFF34D399)

@Composable
fun AppProfilesScreen(
    profileStore: AppProfileStore,
    onNavigateBack: () -> Unit,
    onNavigateToMapping: (packageName: String, appLabel: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val enabledPackages by profileStore.enabledPackages.collectAsState()

    // Load launchable applications in background coroutine
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = InstalledAppsManager.getLaunchableApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
                isLoading = false
            }
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            val q = searchQuery.trim().lowercase()
            installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                        .border(1.dp, CardBorderColor, CircleShape)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Application Profiles",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${enabledPackages.size} active · ${installedApps.size} installed",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                placeholder = {
                    Text("Search installed applications...", color = TextMuted, fontSize = 14.sp)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = CardBorderColor,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No applications found" else "No matching apps for \"$searchQuery\"",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isEnabled = enabledPackages.contains(app.packageName)
                        AppProfileItemCard(
                            app = app,
                            isEnabled = isEnabled,
                            onToggleEnabled = { enabled ->
                                profileStore.setProfileEnabled(app.packageName, enabled)
                            },
                            onClickCard = {
                                if (isEnabled) {
                                    onNavigateToMapping(app.packageName, app.label)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppProfileItemCard(
    app: InstalledApp,
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onClickCard: () -> Unit
) {
    val appBitmap = remember(app.packageName) { app.toImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(
                1.dp,
                if (isEnabled) PrimaryAccent.copy(alpha = 0.4f) else CardBorderColor,
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = isEnabled) {
                onClickCard()
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF202534)),
                contentAlignment = Alignment.Center
            ) {
                if (appBitmap != null) {
                    Image(
                        bitmap = appBitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_buds_mapper),
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App Label + Status
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isEnabled) 1.0f else 0.6f)
            ) {
                Text(
                    text = app.label,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isEnabled) "Profile enabled · Tap to configure" else "Profile disabled",
                    color = if (isEnabled) TextGreen else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Independent Switch (Tapping switch DOES NOT trigger card click)
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggleEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryAccent,
                    uncheckedThumbColor = Color(0xFF6B7280),
                    uncheckedTrackColor = Color(0xFF262C3D)
                )
            )
        }
    }
}

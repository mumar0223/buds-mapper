package com.budsmapper.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budsmapper.app.R
import com.budsmapper.app.data.AppProfileStore
import com.budsmapper.app.data.InstalledApp
import com.budsmapper.app.data.InstalledAppsManager
import com.budsmapper.app.ui.components.AppRootScaffold
import com.budsmapper.app.ui.components.NeonSwitch
import com.budsmapper.app.ui.components.PolishedSearchBar
import com.budsmapper.app.ui.theme.AppTheme

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
    val palette = AppTheme.palette
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(InstalledAppsManager.getCachedApps()) }
    var isLoading by remember { mutableStateOf(installedApps.isEmpty()) }
    var searchQuery by remember { mutableStateOf("") }

    val enabledPackages by profileStore.enabledPackages.collectAsState()

    LaunchedEffect(Unit) {
        val apps = InstalledAppsManager.getOrRefreshApps(context, forceRefresh = true)
        installedApps = apps
        isLoading = false
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

    AppRootScaffold(
        title = "Application Profiles",
        subtitle = "${enabledPackages.size} active · ${installedApps.size} installed",
        onBackClick = onNavigateBack
    ) { topPad, bottomPad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = topPad, bottom = bottomPad)
        ) {
            // Polished Search Bar with proper M3 shape and search icon
            PolishedSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search installed applications..."
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.primaryAccent)
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
                        color = palette.textMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val palette = AppTheme.palette
    val appBitmap = app.iconBitmap

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .border(
                1.dp,
                if (isEnabled) palette.cardBorderActive else palette.cardBorder,
                RoundedCornerShape(18.dp)
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.cardBorder.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (appBitmap != null) {
                    Image(
                        bitmap = appBitmap,
                        contentDescription = app.label,
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_buds_logo),
                        contentDescription = app.label,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App Label & Package
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isEnabled) 1.0f else 0.6f)
            ) {
                Text(
                    text = app.label,
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnabled) "Tap card to remap gestures →" else app.packageName,
                    color = if (isEnabled) palette.primaryAccent else palette.textMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Neon Switch Toggle
            NeonSwitch(
                checked = isEnabled,
                onCheckedChange = onToggleEnabled,
                accentColor = palette.statusAccent
            )
        }
    }
}

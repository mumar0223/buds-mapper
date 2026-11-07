package com.budsmapper.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budsmapper.app.R
import com.budsmapper.app.data.AppProfileStore
import com.budsmapper.app.data.BudsMediaCommand
import com.budsmapper.app.data.InstalledAppsManager
import com.budsmapper.app.data.MappedAction
import com.budsmapper.app.ui.components.AppRootScaffold
import com.budsmapper.app.ui.components.GameSpaceCard
import com.budsmapper.app.ui.components.NeonBadge
import com.budsmapper.app.ui.components.NeonOutlinedButton
import com.budsmapper.app.ui.components.NeonSwitch
import com.budsmapper.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CommandGestureInfo(
    val command: BudsMediaCommand,
    val gestureLabel: String
)

private val SUPPORTED_GESTURES = listOf(
    CommandGestureInfo(BudsMediaCommand.MEDIA_PLAY_PAUSE, "Single tap"),
    CommandGestureInfo(BudsMediaCommand.MEDIA_NEXT, "Double tap"),
    CommandGestureInfo(BudsMediaCommand.MEDIA_PREVIOUS, "Triple tap"),
    CommandGestureInfo(BudsMediaCommand.MEDIA_PLAY, "Play command"),
    CommandGestureInfo(BudsMediaCommand.MEDIA_PAUSE, "Pause command")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGestureMappingScreen(
    packageName: String,
    appLabel: String,
    profileStore: AppProfileStore,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val palette = AppTheme.palette
    val enabledPackages by profileStore.enabledPackages.collectAsState()
    val isEnabled = enabledPackages.contains(packageName)
    val mappingVersion by profileStore.mappingVersion.collectAsState()

    val cachedIcon = remember(packageName) {
        InstalledAppsManager.getCachedApps().find { it.packageName == packageName }?.iconBitmap
    }
    var appIconBitmap by remember(packageName) { mutableStateOf(cachedIcon) }
    var selectedCommandForEdit by remember { mutableStateOf<CommandGestureInfo?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(packageName) {
        if (appIconBitmap == null) {
            withContext(Dispatchers.IO) {
                val bmp = loadAppIcon(context, packageName)
                withContext(Dispatchers.Main) {
                    appIconBitmap = bmp
                }
            }
        }
    }

    AppRootScaffold(
        title = appLabel,
        subtitle = packageName,
        onBackClick = onNavigateBack,
        actions = {
            NeonSwitch(
                checked = isEnabled,
                onCheckedChange = { profileStore.setProfileEnabled(packageName, it) },
                accentColor = palette.statusAccent
            )
        }
    ) { topPad, bottomPad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = topPad, bottom = bottomPad),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Header Card
            GameSpaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.cardBorder.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap!!,
                                contentDescription = appLabel,
                                modifier = Modifier.size(38.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_buds_mapper),
                                contentDescription = null,
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnabled) "Gesture Mapping Active" else "Profile Inactive",
                            color = if (isEnabled) palette.statusAccent else palette.textMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = if (isEnabled) "Taps on your Galaxy Buds Core trigger mapped actions below."
                            else "Turn on the toggle switch at the top to enable gestures for this app.",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Text(
                text = "GESTURE ASSIGNMENTS",
                color = palette.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Gesture Cards List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isEnabled) 1.0f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SUPPORTED_GESTURES.forEach { gestureInfo ->
                    val currentAction = remember(mappingVersion, gestureInfo.command) {
                        profileStore.getMapping(packageName, gestureInfo.command)
                    }

                    GestureCard(
                        gestureLabel = gestureInfo.gestureLabel,
                        command = gestureInfo.command,
                        action = currentAction,
                        isAppEnabled = isEnabled,
                        onEditClick = {
                            if (isEnabled) {
                                selectedCommandForEdit = gestureInfo
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet to choose action
    selectedCommandForEdit?.let { gestureInfo ->
        val currentAction = profileStore.getMapping(packageName, gestureInfo.command)

        ModalBottomSheet(
            onDismissRequest = { selectedCommandForEdit = null },
            sheetState = sheetState,
            containerColor = if (palette.isDark) Color(0xFF131826) else Color(0xFFFFFFFF),
            contentColor = palette.textPrimary
        ) {
            ActionSelectionSheetContent(
                gestureLabel = gestureInfo.gestureLabel,
                command = gestureInfo.command,
                currentAction = currentAction,
                onActionSelected = { newAction ->
                    profileStore.setMapping(packageName, gestureInfo.command, newAction)
                    coroutineScope.launch {
                        sheetState.hide()
                        selectedCommandForEdit = null
                    }
                },
                onCancel = {
                    coroutineScope.launch {
                        sheetState.hide()
                        selectedCommandForEdit = null
                    }
                }
            )
        }
    }
}

@Composable
private fun GestureCard(
    gestureLabel: String,
    command: BudsMediaCommand,
    action: MappedAction,
    isAppEnabled: Boolean,
    onEditClick: () -> Unit
) {
    val palette = AppTheme.palette
    val isConfigured = action != MappedAction.DEFAULT

    GameSpaceCard(
        isActive = isConfigured,
        onClick = if (isAppEnabled) onEditClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = gestureLabel,
                    color = palette.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                NeonBadge(
                    text = command.key,
                    color = palette.primaryAccent
                )
            }

            NeonBadge(
                text = if (isConfigured) "CONFIGURED" else "DEFAULT",
                color = if (isConfigured) palette.actionAccent else palette.textMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Assigned Action Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.cardBorder.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = "Assigned action",
                    color = palette.textMuted,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.displayName,
                    color = if (action == MappedAction.NONE) palette.textMuted else palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.description,
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Edit mapping →",
                color = palette.primaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ActionSelectionSheetContent(
    gestureLabel: String,
    command: BudsMediaCommand,
    currentAction: MappedAction,
    onActionSelected: (MappedAction) -> Unit,
    onCancel: () -> Unit
) {
    val palette = AppTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "Map $gestureLabel",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Bluetooth Command: ${command.key} (${command.description})",
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MappedAction.entries.forEach { action ->
                val isSelected = action == currentAction
                val borderColor = if (isSelected) palette.primaryAccent else palette.cardBorder
                val bgColor = if (isSelected) palette.primaryAccent.copy(alpha = 0.10f) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgColor)
                        .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { onActionSelected(action) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onActionSelected(action) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = palette.primaryAccent,
                            unselectedColor = palette.textMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.displayName,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = action.description,
                            color = palette.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        NeonOutlinedButton(
            text = "Cancel",
            onClick = onCancel,
            accentColor = palette.textMuted,
            trailingText = null
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun loadAppIcon(context: Context, packageName: String): ImageBitmap? {
    return try {
        val d: Drawable = context.packageManager.getApplicationIcon(packageName)
        if (d is BitmapDrawable && d.bitmap != null) {
            d.bitmap.asImageBitmap()
        } else {
            val width = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
            val height = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            d.setBounds(0, 0, width, height)
            d.draw(canvas)
            bmp.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}

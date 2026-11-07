package com.budsmapper.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DarkBackground = Color(0xFF0F1117)
private val CardBackground = Color(0xFF161A24)
private val CardBorderColor = Color(0xFF262C3D)
private val PrimaryAccent = Color(0xFF4C8DFF)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9CA3AF)
private val TextGreen = Color(0xFF34D399)
private val BadgeBg = Color(0xFF1F2937)

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

    // Fallback: Resolve app icon on background thread only if not in cache
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
            // Top Navigation & App Header Card
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

                // App Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF202534)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap!!,
                            contentDescription = appLabel,
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLabel,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = packageName,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Profile Switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { profileStore.setProfileEnabled(packageName, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryAccent,
                        uncheckedThumbColor = Color(0xFF6B7280),
                        uncheckedTrackColor = Color(0xFF262C3D)
                    )
                )
            }

            // Subheader Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "$appLabel Gesture Mapping",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnabled) {
                        "Configure what your Galaxy Buds Core taps trigger inside $appLabel."
                    } else {
                        "Profile is currently disabled. Toggle the switch above to activate custom earbud gestures."
                    },
                    color = if (isEnabled) TextMuted else Color(0xFFF59E0B),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Text(
                text = "GESTURE ASSIGNMENTS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Gesture Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isEnabled) 1.0f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SUPPORTED_GESTURES.forEach { gestureInfo ->
                    // Re-read mapping whenever mappingVersion changes
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal Bottom Sheet to choose action
    selectedCommandForEdit?.let { gestureInfo ->
        val currentAction = profileStore.getMapping(packageName, gestureInfo.command)

        ModalBottomSheet(
            onDismissRequest = { selectedCommandForEdit = null },
            sheetState = sheetState,
            containerColor = CardBackground,
            contentColor = TextWhite
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAppEnabled) { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Gesture Name & Command Badge
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
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BadgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = command.key,
                            color = PrimaryAccent,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (action != MappedAction.DEFAULT) Color(0xFF064E3B) else BadgeBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (action != MappedAction.DEFAULT) "Configured" else "Default",
                        color = if (action != MappedAction.DEFAULT) TextGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Assigned Action Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B202D))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Assigned action",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.displayName,
                    color = if (action == MappedAction.NONE) TextMuted else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.description,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Action row: Edit Mapping Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit mapping →",
                    color = PrimaryAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "Map $gestureLabel",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Bluetooth Command: ${command.key} (${command.description})",
                color = TextMuted,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF1E283E) else Color(0xFF1A1E29))
                        .border(
                            1.dp,
                            if (isSelected) PrimaryAccent else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onActionSelected(action) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onActionSelected(action) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PrimaryAccent,
                            unselectedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.displayName,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = action.description,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF262C3D),
                contentColor = TextWhite
            )
        ) {
            Text(text = "Cancel")
        }

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

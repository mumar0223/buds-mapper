package com.budsmapper.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.budsmapper.app.MainActivity
import com.budsmapper.app.R
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.media.BudsMediaSessionManager

class StatusNotificationManager(
    private val context: Context,
    private val mediaSessionManager: BudsMediaSessionManager
) {

    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val eventStore = LastEventStore.getInstance(appContext)

    @Volatile
    private var lastAppLabel: String = "Buds Mapper"

    @Volatile
    private var lastPackageName: String = appContext.packageName

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_TOGGLE_CAPTURE -> {
                    val enabled = !eventStore.isMediaSessionCaptureEnabled()
                    Log.i(TAG, "Notification action: Toggling capture to $enabled")
                    eventStore.setMediaSessionCaptureEnabled(enabled)
                    updateNotification(lastAppLabel, lastPackageName)
                }
                ACTION_CYCLE_MODE -> {
                    eventStore.cycleMode()
                    Log.i(TAG, "Notification action: Cycled mode to ${eventStore.getCurrentMode()}")
                    updateNotification(lastAppLabel, lastPackageName)
                }
                ACTION_NOTIFICATION_DISMISSED -> {
                    if (eventStore.isStatusNotificationEnabled()) {
                        Log.d(TAG, "Media notification dismissed; re-pinning command center")
                        updateNotification(lastAppLabel, lastPackageName)
                    }
                }
            }
        }
    }

    init {
        createNotificationChannel()
        registerActionReceiver()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Displays the Spotify-styled Buds Mapper Earbuds Command Center."
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun registerActionReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_TOGGLE_CAPTURE)
            addAction(ACTION_CYCLE_MODE)
            addAction(ACTION_NOTIFICATION_DISMISSED)
        }
        ContextCompat.registerReceiver(
            appContext,
            actionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun updateNotification(appLabel: String, packageName: String) {
        lastAppLabel = appLabel
        lastPackageName = packageName

        val isCaptureActive = eventStore.isMediaSessionCaptureEnabled()
        val currentStoreMode = eventStore.getCurrentMode()

        // 1. Resolve Dynamic Mode and Gesture Cheat Sheet based on active application
        val effectiveMode = when {
            currentStoreMode != "Auto Mode" -> currentStoreMode
            packageName.contains("youtube") || packageName.contains("tiktok") || packageName.contains("instagram") -> "Shorts Mode"
            packageName.contains("spotify") || packageName.contains("music") || packageName.contains("audio") -> "Music Mode"
            else -> "Scroll Mode"
        }

        val displayTitle = "$appLabel • $effectiveMode"
        val displaySubtitle = when (effectiveMode) {
            "Shorts Mode" -> "Double Tap: Next Video • Triple Tap: Prev"
            "Music Mode" -> "Single Tap: Play/Pause • Double: Next Track"
            else -> "Double Tap: Scroll Down • Triple: Scroll Up"
        }
        val statusBadge = if (isCaptureActive) "ᛒ Earbuds Active" else "⏸ Remapping Paused"

        // 2. Generate Spotify-style full-bleed dynamic artwork matching the foreground app
        val artwork = generateDynamicArtwork(packageName)

        // 3. Sync MediaSession metadata so SystemUI renders the full-bleed artwork across the tile
        mediaSessionManager.updateMetadata(
            title = displayTitle,
            artist = displaySubtitle,
            album = statusBadge,
            artwork = artwork
        )

        // 4. Content intent (opens app dashboard)
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 5. Interactive action buttons
        val toggleIntent = Intent(ACTION_TOGGLE_CAPTURE).apply { setPackage(appContext.packageName) }
        val togglePendingIntent = PendingIntent.getBroadcast(
            appContext, 10, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleIcon = if (isCaptureActive) R.drawable.ic_pause else R.drawable.ic_play
        val toggleTitle = if (isCaptureActive) "Pause" else "Resume"
        val toggleAction = Notification.Action.Builder(
            Icon.createWithResource(appContext, toggleIcon),
            toggleTitle,
            togglePendingIntent
        ).build()

        val modeIntent = Intent(ACTION_CYCLE_MODE).apply { setPackage(appContext.packageName) }
        val modePendingIntent = PendingIntent.getBroadcast(
            appContext, 11, modeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val modeAction = Notification.Action.Builder(
            Icon.createWithResource(appContext, R.drawable.ic_switch_mode),
            "Mode",
            modePendingIntent
        ).build()

        val settingsAction = Notification.Action.Builder(
            Icon.createWithResource(appContext, R.drawable.ic_buds_mapper),
            "Settings",
            contentPendingIntent
        ).build()

        val deleteIntent = Intent(ACTION_NOTIFICATION_DISMISSED).apply {
            setPackage(appContext.packageName)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            appContext,
            1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 6. Build the genuine MediaStyle notification linked to MediaSession
        val sessionToken = mediaSessionManager.getSessionToken()
        val mediaStyle = Notification.MediaStyle()
        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken)
        }
        mediaStyle.setShowActionsInCompactView(0, 1) // Toggle & Mode in compact view

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_buds_mapper)
            .setContentTitle(displayTitle)
            .setContentText(displaySubtitle)
            .setSubText(statusBadge)
            .setLargeIcon(artwork)
            .setStyle(mediaStyle)
            .addAction(toggleAction)
            .addAction(modeAction)
            .addAction(settingsAction)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            Log.d(TAG, "Spotify-style Media Command Center updated for: $displayTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post media status notification", e)
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.i(TAG, "Media status notification cancelled")
    }

    fun release() {
        try {
            appContext.unregisterReceiver(actionReceiver)
        } catch (_: IllegalArgumentException) {}
        cancelNotification()
    }

    /**
     * Generates a 16:9 dynamic Spotify-style full-bleed ambient artwork bitmap
     * themed to the currently active application.
     */
    private fun generateDynamicArtwork(packageName: String): Bitmap {
        val width = 720
        val height = 405
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isYouTube = packageName.contains("youtube")
        val isSpotify = packageName.contains("spotify") || packageName.contains("music")
        val isSocial = packageName.contains("instagram") || packageName.contains("tiktok")

        val (c1, c2, c3, orbColor1, orbColor2) = when {
            isYouTube -> listOf(
                Color.parseColor("#2C060B"),
                Color.parseColor("#150205"),
                Color.parseColor("#08080C"),
                Color.parseColor("#80FF0033"),
                Color.parseColor("#60FF3366")
            )
            isSpotify -> listOf(
                Color.parseColor("#0B2A18"),
                Color.parseColor("#06160D"),
                Color.parseColor("#080C0E"),
                Color.parseColor("#801DB954"),
                Color.parseColor("#6010D876")
            )
            isSocial -> listOf(
                Color.parseColor("#280826"),
                Color.parseColor("#14041C"),
                Color.parseColor("#090812"),
                Color.parseColor("#80E1306C"),
                Color.parseColor("#60FD1D1D")
            )
            else -> listOf(
                Color.parseColor("#101C38"),
                Color.parseColor("#0B1326"),
                Color.parseColor("#080B14"),
                Color.parseColor("#804C8DFF"),
                Color.parseColor("#6000F2FE")
            )
        }

        // Base ambient gradient
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(c1, c2, c3),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), basePaint)

        // Glow Orb 1 (Top Left)
        val orb1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.2f, height * 0.35f, 180f,
                orbColor1, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.2f, height * 0.35f, 180f, orb1)

        // Glow Orb 2 (Right Center)
        val orb2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.8f, height * 0.65f, 220f,
                orbColor2, Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.8f, height * 0.65f, 220f, orb2)

        // Dark gradient scrim across bottom for maximum text contrast
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, height * 0.3f, 0f, height.toFloat(),
                Color.TRANSPARENT, Color.parseColor("#E6080A10"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height * 0.3f, width.toFloat(), height.toFloat(), scrimPaint)

        return bitmap
    }

    companion object {
        private const val TAG = "StatusNotifMgr"
        private const val CHANNEL_ID = "buds_mapper_command_center_v1"
        private const val CHANNEL_NAME = "Buds Mapper Command Center"
        private const val NOTIFICATION_ID = 1001

        private const val ACTION_TOGGLE_CAPTURE = "com.budsmapper.app.ACTION_TOGGLE_CAPTURE"
        private const val ACTION_CYCLE_MODE = "com.budsmapper.app.ACTION_CYCLE_MODE"
        private const val ACTION_NOTIFICATION_DISMISSED = "com.budsmapper.app.ACTION_NOTIFICATION_DISMISSED"
    }
}

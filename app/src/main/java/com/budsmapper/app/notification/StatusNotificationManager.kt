package com.budsmapper.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.budsmapper.app.MainActivity
import com.budsmapper.app.R
import com.budsmapper.app.data.LastEventStore

/**
 * StatusNotificationManager
 *
 * Implements a clean, compact, native Android standard notification.
 * Uses Samsung One UI's native notification card styling without custom RemoteViews.
 *
 * - Compact ~64dp standard height
 * - Title: "[App] is active"
 * - Subtitle: "Gestures ready · Background service running"
 * - Completely silent (IMPORTANCE_LOW), non-intrusive, updating in-place
 */
class StatusNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val eventStore = LastEventStore.getInstance(appContext)

    @Volatile
    private var lastAppLabel: String = "Buds Mapper"

    @Volatile
    private var lastPackageName: String = appContext.packageName

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_DISMISSED) {
                if (eventStore.isStatusNotificationEnabled()) {
                    Log.d(TAG, "Notification dismissed by system; re-pinning native status card")
                    updateNotification(lastAppLabel, lastPackageName)
                }
            }
        }
    }

    init {
        createNotificationChannel()
        registerDismissReceiver()
    }

    private fun createNotificationChannel() {
        // Remove legacy channels to avoid cached SystemUI states
        try {
            notificationManager.deleteNotificationChannel("buds_mapper_active_status")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v2")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v3")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v4")
            notificationManager.deleteNotificationChannel("buds_mapper_command_center_v1")
            notificationManager.deleteNotificationChannel("buds_mapper_status_channel_v1")
        } catch (_: Exception) {}

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Displays Buds Mapper active status in a compact native card."
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun registerDismissReceiver() {
        val filter = IntentFilter(ACTION_NOTIFICATION_DISMISSED)
        ContextCompat.registerReceiver(
            appContext,
            dismissReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun updateNotification(appLabel: String, packageName: String) {
        lastAppLabel = appLabel
        lastPackageName = packageName

        val isCaptureActive = eventStore.isMediaSessionCaptureEnabled()

        val contentTitle = "$appLabel is active"
        val contentText = if (isCaptureActive) {
            "Gestures ready · Background service running"
        } else {
            "Gestures paused · Background service running"
        }

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(ACTION_NOTIFICATION_DISMISSED).apply {
            setPackage(appContext.packageName)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            appContext,
            1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Standard Android Notification Template (100% native One UI styling)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_buds_mapper)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Native status notification updated: $contentTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post status notification", e)
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.i(TAG, "Native status notification cancelled")
    }

    fun release() {
        try {
            appContext.unregisterReceiver(dismissReceiver)
        } catch (_: IllegalArgumentException) {}
        cancelNotification()
    }

    companion object {
        private const val TAG = "StatusNotifMgr"
        private const val CHANNEL_ID = "buds_mapper_native_v1"
        private const val CHANNEL_NAME = "Buds Mapper Status"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_NOTIFICATION_DISMISSED = "com.budsmapper.app.ACTION_NOTIFICATION_DISMISSED"
    }
}

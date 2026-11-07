package com.budsmapper.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?
) {
    fun toImageBitmap(): ImageBitmap? {
        val d = icon ?: return null
        return try {
            if (d is BitmapDrawable && d.bitmap != null) {
                return d.bitmap.asImageBitmap()
            }
            val width = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
            val height = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            d.setBounds(0, 0, width, height)
            d.draw(canvas)
            bmp.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

object InstalledAppsManager {

    private const val TAG = "InstalledAppsMgr"

    /**
     * Discovers user-facing launchable applications on the device.
     * Deduplicates multiple launcher activities belonging to the same package.
     */
    fun getLaunchableApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying launcher activities", e)
            emptyList()
        }

        val seenPackages = HashSet<String>()
        val appsList = ArrayList<InstalledApp>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            // Ignore internal android package
            if (pkg == "android") continue

            val label = try {
                resolveInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg
            }

            val icon = try {
                resolveInfo.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            appsList.add(InstalledApp(packageName = pkg, label = label, icon = icon))
        }

        // Sort alphabetically by application name
        return appsList.sortedBy { it.label.lowercase() }
    }
}

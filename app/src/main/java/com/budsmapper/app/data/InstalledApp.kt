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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val iconBitmap: ImageBitmap? = null
)

object InstalledAppsManager {

    private const val TAG = "InstalledAppsMgr"

    // In-memory cache of discovered apps with pre-decoded icons
    @Volatile
    private var cachedApps: List<InstalledApp> = emptyList()

    fun getCachedApps(): List<InstalledApp> = cachedApps

    /**
     * Retrieves cached applications immediately if available, or discovers them if cache is empty.
     * When forceRefresh is true, re-queries PackageManager to sync newly installed/uninstalled apps.
     */
    suspend fun getOrRefreshApps(context: Context, forceRefresh: Boolean = false): List<InstalledApp> {
        if (!forceRefresh && cachedApps.isNotEmpty()) {
            return cachedApps
        }

        return withContext(Dispatchers.IO) {
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

                val drawableIcon = try {
                    resolveInfo.loadIcon(pm)
                } catch (_: Exception) {
                    null
                }

                // Pre-decode drawable to ImageBitmap once on IO thread
                val bitmapIcon = drawableToImageBitmap(drawableIcon)

                appsList.add(InstalledApp(packageName = pkg, label = label, iconBitmap = bitmapIcon))
            }

            val sortedList = appsList.sortedBy { it.label.lowercase() }
            cachedApps = sortedList
            Log.i(TAG, "Discovered and cached ${sortedList.size} applications with pre-decoded icons")
            sortedList
        }
    }

    private fun drawableToImageBitmap(d: Drawable?): ImageBitmap? {
        if (d == null) return null
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

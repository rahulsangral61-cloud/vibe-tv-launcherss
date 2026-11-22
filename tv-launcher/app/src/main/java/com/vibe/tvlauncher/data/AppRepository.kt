package com.vibe.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val packageName: String,
    val launchIntent: Intent,
    val icon: ImageBitmap,
    val banner: ImageBitmap?,
    val accent: Color,
)

/** Reads every launchable app (TV apps first, then regular ones) and prepares artwork for the UI. */
class AppRepository(private val context: Context) {

    suspend fun load(): List<AppEntry> = withContext(Dispatchers.Default) {
        val pm = context.packageManager

        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val phoneIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val tvApps = pm.queryIntentActivities(tvIntent, 0).map { it to Intent.CATEGORY_LEANBACK_LAUNCHER }
        val phoneApps = pm.queryIntentActivities(phoneIntent, 0).map { it to Intent.CATEGORY_LAUNCHER }

        (tvApps + phoneApps)
            .filter { (info, _) -> info.activityInfo.packageName != context.packageName }
            .distinctBy { (info, _) -> info.activityInfo.packageName }
            .mapNotNull { (info, category) -> runCatching { toEntry(pm, info, category) }.getOrNull() }
            .sortedBy { it.label.lowercase() }
    }

    private fun toEntry(pm: PackageManager, info: ResolveInfo, category: String): AppEntry {
        val activity = info.activityInfo
        val label = info.loadLabel(pm).toString()

        val iconBitmap = info.loadIcon(pm).toBitmap(ICON_SIZE, ICON_SIZE)
        val bannerDrawable: Drawable? = activity.loadBanner(pm) ?: activity.applicationInfo.loadBanner(pm)
        val bannerBitmap = bannerDrawable?.toBitmap(BANNER_W, BANNER_H)

        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(category)
            .setClassName(activity.packageName, activity.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        return AppEntry(
            label = label,
            packageName = activity.packageName,
            launchIntent = launch,
            icon = iconBitmap.asImageBitmap(),
            banner = bannerBitmap?.asImageBitmap(),
            accent = accentFrom(bannerBitmap ?: iconBitmap),
        )
    }

    /** Picks a vivid colour from the artwork; used to tint the whole home screen. */
    private fun accentFrom(source: Bitmap): Color {
        val small = Bitmap.createScaledBitmap(source, 48, 48, true)
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)

        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0L
        for (p in px) {
            val a = p ushr 24
            if (a < 200) continue
            val rr = (p shr 16) and 0xFF
            val gg = (p shr 8) and 0xFF
            val bb = p and 0xFF
            val lum = (rr * 299 + gg * 587 + bb * 114) / 1000
            if (lum < 25 || lum > 235) continue // ignore near-black / near-white pixels
            r += rr
            g += gg
            b += bb
            n++
        }
        if (n == 0L) return FALLBACK_ACCENT

        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV((r / n).toInt(), (g / n).toInt(), (b / n).toInt(), hsv)
        hsv[1] = hsv[1].coerceAtLeast(0.45f)
        hsv[2] = hsv[2].coerceIn(0.5f, 0.85f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private companion object {
        const val ICON_SIZE = 192
        const val BANNER_W = 416
        const val BANNER_H = 234
        val FALLBACK_ACCENT = Color(0xFF3A3A3C)
    }
}

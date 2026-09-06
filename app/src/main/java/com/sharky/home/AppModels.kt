package com.sharky.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class TvApp(val label: String, val packageName: String, val icon: Drawable?, val system: Boolean)

object AppRepository {
    fun installed(context: Context): List<TvApp> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0).mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) null else TvApp(
                info.loadLabel(pm).toString(), pkg, info.loadIcon(pm),
                (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    fun launch(context: Context, packageName: String): Boolean = try {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } ?: false
    } catch (_: Exception) { false }

    fun systemSettings(context: Context) = try {
        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { }
}

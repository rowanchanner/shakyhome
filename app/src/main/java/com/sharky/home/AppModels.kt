package com.sharky.home

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import java.util.Locale

data class TvApp(val label: String, val packageName: String, val icon: Drawable?)

object AppRepository {
    fun installed(context: Context): List<TvApp> {
        val pm = context.packageManager
        return listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            .flatMap { pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(it), 0) }
            .filter { it.activityInfo.exported && it.activityInfo.enabled && it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }.mapNotNull { info -> runCatching {
                TvApp(info.loadLabel(pm).toString(), info.activityInfo.packageName,
                    info.activityInfo.loadBanner(pm) ?: info.loadIcon(pm))
            }.getOrNull() }.sortedBy { it.label.lowercase(Locale.ROOT) }
    }
    fun launch(context: Context, pkg: String): Boolean = runCatching {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    }.getOrDefault(false)
    fun open(context: Context, intent: Intent): Boolean = runCatching { context.startActivity(intent); true }.getOrDefault(false)
}

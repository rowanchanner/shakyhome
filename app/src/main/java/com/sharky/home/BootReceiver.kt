package com.sharky.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** A best-effort hook only. Fire OS may block background launches; Home on Fire is the supported redirect route. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.getSharedPreferences("sharky", Context.MODE_PRIVATE).getBoolean("boot_attempt", false)) return
        try { context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)) } catch (_: Exception) { }
    }
}

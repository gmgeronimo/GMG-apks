package com.gmg.edgelight

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings as SysSettings

object Permissions {

    fun notificationAccess(ctx: Context): Boolean {
        val enabled = SysSettings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(ctx, NotifListener::class.java)
        return enabled.split(":").any { ComponentName.unflattenFromString(it) == me }
    }

    fun overlayService(ctx: Context): Boolean {
        val enabled = SysSettings.Secure.getString(ctx.contentResolver, SysSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = ComponentName(ctx, EdgeLightService::class.java)
        return enabled.split(":").any { ComponentName.unflattenFromString(it) == me }
    }

    fun openNotificationAccess(ctx: Context) =
        ctx.startActivity(Intent(SysSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun openAccessibility(ctx: Context) =
        ctx.startActivity(Intent(SysSettings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

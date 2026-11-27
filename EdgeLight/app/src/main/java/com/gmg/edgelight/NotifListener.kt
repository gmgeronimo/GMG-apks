package com.gmg.edgelight

import android.app.Notification
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Recebe as notificações e decide quando acender a luz. */
class NotifListener : NotificationListenerService() {

    private var lastFlash = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!shouldLight(sbn)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastFlash < COOLDOWN_MS) return
        lastFlash = now
        EdgeLightService.instance?.flash()
    }

    private fun shouldLight(sbn: StatusBarNotification): Boolean {
        val settings = SettingsStore.load(this)
        if (!settings.enabled) return false
        if (sbn.packageName == packageName) return false
        // Música, downloads, chamadas em andamento etc.
        if (sbn.isOngoing) return false
        // Resumo de grupo: a notificação "de verdade" já chega separada
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return false
        // Notificações silenciosas
        val ranking = Ranking()
        if (currentRanking?.getRanking(sbn.key, ranking) == true && ranking.isAmbient) return false
        // Com a tela apagada a luz não apareceria
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    companion object {
        private const val COOLDOWN_MS = 1500L
    }
}

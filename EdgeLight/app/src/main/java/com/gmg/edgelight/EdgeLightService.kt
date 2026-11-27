package com.gmg.edgelight

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Serviço de acessibilidade usado só para desenhar por cima de tudo,
 * inclusive da tela de bloqueio. Não lê nada da tela.
 */
class EdgeLightService : AccessibilityService() {

    private var current: EdgeLightView? = null
    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        removeOverlay()
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    /** Mostra a luz uma vez. Se já estiver rodando, recomeça. */
    fun flash(settings: Settings = SettingsStore.load(this)) {
        removeOverlay()
        val view = EdgeLightView(this, settings) { removeOverlay() }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                setFitInsetsTypes(0)
                // Tamanho real da tela, incluindo barras de status e navegação
                val bounds = wm.maximumWindowMetrics.bounds
                width = bounds.width()
                height = bounds.height()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        runCatching { wm.addView(view, lp) }.onSuccess {
            current = view
            view.start()
        }
    }

    private fun removeOverlay() {
        current?.let { v ->
            v.cancel()
            runCatching { wm.removeView(v) }
        }
        current = null
    }

    companion object {
        /** Instância ativa; nula quando o serviço está desligado nas configurações. */
        @Volatile var instance: EdgeLightService? = null
            private set
    }
}

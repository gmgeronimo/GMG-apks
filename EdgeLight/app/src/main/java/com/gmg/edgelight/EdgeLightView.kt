package com.gmg.edgelight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Build
import android.view.DisplayCutout
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import android.view.animation.LinearInterpolator

/** View do overlay: ocupa a tela toda e roda a animação uma vez. */
class EdgeLightView(
    context: Context,
    private val settings: Settings,
    private val onFinished: () -> Unit,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val renderer = LightRenderer(density).apply { configure(settings) }
    private var corner = if (settings.cornersFromScreen) DEFAULT_CORNER_DP * density else 0f
    private var hole: CameraHole? = null
    private var elapsed = 0L

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = renderer.totalMs
        interpolator = LinearInterpolator()
        addUpdateListener {
            elapsed = (it.animatedFraction * duration).toLong()
            invalidate()
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = onFinished()
        })
    }

    init {
        // O brilho (BlurMaskFilter) só funciona com aceleração de hardware a partir do Android 9
        if (settings.glow && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    fun start() = animator.start()

    fun cancel() {
        animator.removeAllListeners()
        animator.cancel()
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (settings.cornersFromScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
                ?.takeIf { it > 0 }?.let { corner = it.toFloat() }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            hole = insets.displayCutout?.let { findCameraHole(it, resources.displayMetrics.widthPixels) }
        }
        renderer.setGeometry(width, height, corner, hole)
        return super.onApplyWindowInsets(insets)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        renderer.setGeometry(w, h, corner, hole)
    }

    override fun onDraw(canvas: Canvas) = renderer.draw(canvas, elapsed)

    companion object {
        const val DEFAULT_CORNER_DP = 32f
    }
}

/**
 * Procura um furo de câmera redondo no recorte da tela.
 * Entalhes largos (estilo "notch") são ignorados: o anel só faz sentido em volta de um furo.
 */
fun findCameraHole(cutout: DisplayCutout, screenWidth: Int): CameraHole? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    val rect = RectF()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && cutout.cutoutPath != null) {
        cutout.cutoutPath!!.computeBounds(rect, true)
    } else {
        val r = cutout.boundingRects.minByOrNull { it.top } ?: return null
        rect.set(r)
    }
    if (rect.isEmpty || rect.width() > screenWidth * 0.25f) return null
    val radius = rect.width() / 2f
    if (rect.height() < rect.width() * 0.6f) return null       // muito achatado: é um entalhe, não um furo
    // Alguns aparelhos informam um retângulo mais alto que o furo, colado no topo da tela
    return CameraHole(rect.centerX(), rect.bottom - radius, radius)
}

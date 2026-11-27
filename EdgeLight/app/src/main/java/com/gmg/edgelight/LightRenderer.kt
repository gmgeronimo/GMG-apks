package com.gmg.edgelight

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Furo da câmera frontal, em pixels da tela. */
data class CameraHole(val cx: Float, val cy: Float, val radius: Float)

/**
 * Desenha a luz da borda e o anel da câmera para um instante da animação.
 * Usado tanto pelo overlay de verdade quanto pela prévia dentro do app.
 */
class LightRenderer(private val density: Float) {

    private var s = Settings()
    private var width = 0
    private var height = 0
    private var cornerPx = 0f
    private var camera: CameraHole? = null

    private val edgePath = Path()
    private var edgeMeasure = PathMeasure()
    private var perimeter = 0f
    private val piece = Path()
    private val ringOval = RectF()

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val glowEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val glowRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    private val edgeThickness get() = s.edge.thicknessDp * density
    private val ringThickness get() = s.ring.thicknessDp * density

    val runMs: Long get() = (s.edge.lapSeconds * 1000).toLong() * s.edge.laps
    val totalMs: Long get() = runMs + FADE_OUT_MS

    fun configure(settings: Settings) {
        s = settings
        glowEdge.maskFilter = BlurMaskFilter(max(edgeThickness * 3f, 4f), BlurMaskFilter.Blur.NORMAL)
        glowRing.maskFilter = BlurMaskFilter(max(ringThickness * 2.5f, 3f), BlurMaskFilter.Blur.NORMAL)
        rebuild()
    }

    fun setGeometry(w: Int, h: Int, cornerRadiusPx: Float, hole: CameraHole?) {
        width = w; height = h; cornerPx = cornerRadiusPx; camera = hole
        rebuild()
    }

    /** Contorno arredondado, começando no topo-centro e seguindo em sentido horário. */
    private fun rebuild() {
        if (width == 0 || height == 0) return
        val inset = edgeThickness / 2f
        val l = inset; val t = inset; val r = width - inset; val b = height - inset
        val rad = min(cornerPx - inset, min((r - l) / 2f, (b - t) / 2f)).coerceAtLeast(0f)
        val d = rad * 2
        edgePath.reset()
        edgePath.moveTo((l + r) / 2f, t)
        edgePath.lineTo(r - rad, t)
        if (rad > 0f) edgePath.arcTo(RectF(r - d, t, r, t + d), -90f, 90f)
        edgePath.lineTo(r, b - rad)
        if (rad > 0f) edgePath.arcTo(RectF(r - d, b - d, r, b), 0f, 90f)
        edgePath.lineTo(l + rad, b)
        if (rad > 0f) edgePath.arcTo(RectF(l, b - d, l + d, b), 90f, 90f)
        edgePath.lineTo(l, t + rad)
        if (rad > 0f) edgePath.arcTo(RectF(l, t, l + d, t + d), 180f, 90f)
        edgePath.close()
        edgeMeasure = PathMeasure(edgePath, false)
        perimeter = edgeMeasure.length

        camera?.let {
            val rr = it.radius + 2f * density + ringThickness / 2f
            ringOval.set(it.cx - rr, it.cy - rr, it.cx + rr, it.cy + rr)
        }
    }

    /** Progresso de 0 a 1 da animação, já com o ritmo escolhido. */
    private fun eased(elapsed: Long): Float {
        val u = (elapsed.toFloat() / runMs).coerceIn(0f, 1f)
        return when (s.pace) {
            Pace.CONSTANT -> u
            Pace.ACCELERATE -> u * u
            Pace.DECELERATE -> 1f - (1f - u) * (1f - u)
        }
    }

    private fun fade(elapsed: Long): Float = when {
        elapsed < FADE_IN_MS -> elapsed / FADE_IN_MS.toFloat()
        elapsed > runMs -> 1f - (elapsed - runMs) / FADE_OUT_MS.toFloat()
        else -> 1f
    }.coerceIn(0f, 1f)

    fun draw(canvas: Canvas, elapsed: Long) {
        if (perimeter <= 0f || elapsed > totalMs) return
        val fade = fade(elapsed)
        val e = eased(elapsed)
        drawEdge(canvas, e, fade)
        if (s.ring.enabled && camera != null) drawRing(canvas, e, fade)
    }

    private fun trailAlpha(i: Int) = if (s.fadingTrail) (1f - i / STEPS.toFloat()).pow(1.6f) else 1f
    private fun trailWidth(base: Float, i: Int) = if (s.fadingTrail) base * (1f - 0.5f * i / STEPS) else base

    private fun drawEdge(canvas: Canvas, eased: Float, fade: Float) {
        val sign = if (s.edge.clockwise) 1f else -1f
        val head = sign * eased * s.edge.laps * perimeter
        val trail = perimeter * s.edge.trailPercent / 100f * 0.5f
        for (i in 0 until STEPS) {
            val a = head - sign * trail * (i + 1) / STEPS
            val b = head - sign * trail * i / STEPS
            val color = s.edge.color.at(i / (STEPS - 1f))
            val alpha = (255 * fade * trailAlpha(i)).toInt()
            val w = trailWidth(edgeThickness, i)
            if (s.glow) {
                glowEdge.color = color; glowEdge.alpha = (alpha * 0.8f).toInt(); glowEdge.strokeWidth = w * 2.5f
                drawPiece(canvas, min(a, b), max(a, b), glowEdge)
            }
            line.color = color; line.alpha = alpha; line.strokeWidth = w
            drawPiece(canvas, min(a, b), max(a, b), line)
        }
    }

    /** Desenha o trecho [a, b] do contorno, dando a volta quando passa do início. */
    private fun drawPiece(canvas: Canvas, a0: Float, b0: Float, paint: Paint) {
        val shift = kotlin.math.floor(a0 / perimeter) * perimeter
        val a = a0 - shift
        val b = b0 - shift
        if (b <= perimeter) {
            segment(canvas, a, b, paint)
        } else {
            segment(canvas, a, perimeter, paint)
            segment(canvas, 0f, b - perimeter, paint)
        }
    }

    private fun segment(canvas: Canvas, a: Float, b: Float, paint: Paint) {
        if (b <= a) return
        piece.reset()
        if (edgeMeasure.getSegment(a, b, piece, true)) canvas.drawPath(piece, paint)
    }

    private fun drawRing(canvas: Canvas, eased: Float, fade: Float) {
        val sign = if (s.ring.clockwise) 1f else -1f
        val turns = eased * runMs / (s.ring.lapSeconds * 1000f)
        val head = -90f + sign * turns * 360f
        val trail = 360f * s.ring.trailPercent / 100f
        for (i in 0 until STEPS) {
            val a = head - sign * trail * (i + 1) / STEPS
            val b = head - sign * trail * i / STEPS
            val start = min(a, b)
            val sweep = kotlin.math.abs(b - a)
            val color = s.ring.color.at(i / (STEPS - 1f))
            val alpha = (255 * fade * trailAlpha(i)).toInt()
            val w = trailWidth(ringThickness, i)
            if (s.glow) {
                glowRing.color = color; glowRing.alpha = (alpha * 0.8f).toInt(); glowRing.strokeWidth = w * 2.2f
                canvas.drawArc(ringOval, start, sweep, false, glowRing)
            }
            line.color = color; line.alpha = alpha; line.strokeWidth = w
            canvas.drawArc(ringOval, start, sweep, false, line)
        }
    }

    companion object {
        const val STEPS = 24
        const val FADE_IN_MS = 200L
        const val FADE_OUT_MS = 250L
    }
}

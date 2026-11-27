package com.gmg.edgelight

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Pace { CONSTANT, ACCELERATE, DECELERATE }
enum class AppTheme { NOTURNO, PAPEL, SISTEMA }

/** Cor de um traço: sólida ou degradê de 4 cores (da ponta até o fim do rastro). */
data class LineColor(
    val gradient: Boolean = false,
    val solid: Int = 0xFF29B6FF.toInt(),
    val stops: List<Int> = Presets.GRADIENTS[0],
) {
    /** Cor no ponto t do traço (0 = ponta, 1 = fim do rastro). */
    fun at(t: Float): Int {
        if (!gradient) return solid
        val f = t.coerceIn(0f, 1f) * (stops.size - 1)
        val i = f.toInt().coerceAtMost(stops.size - 2)
        return lerpColor(stops[i], stops[i + 1], f - i)
    }
}

/** Ajustes da luz da borda. */
data class EdgeConfig(
    val clockwise: Boolean = true,
    val color: LineColor = LineColor(),
    val thicknessDp: Float = 3f,     // 1–10
    val trailPercent: Int = 22,      // 5–60
    val lapSeconds: Float = 1.8f,    // 0,6–5
    val laps: Int = 2,               // 1–6
)

/** Ajustes do anel em volta da câmera frontal. */
data class RingConfig(
    val enabled: Boolean = true,
    val clockwise: Boolean = true,
    val color: LineColor = LineColor(solid = 0xFFFFFFFF.toInt(), stops = Presets.GRADIENTS[3]),
    val thicknessDp: Float = 2f,     // 1–4
    val trailPercent: Int = 40,      // 10–90
    val lapSeconds: Float = 0.9f,    // 0,3–3
)

data class Settings(
    val enabled: Boolean = true,
    val edge: EdgeConfig = EdgeConfig(),
    val ring: RingConfig = RingConfig(),
    val pace: Pace = Pace.CONSTANT,
    val glow: Boolean = true,
    val fadingTrail: Boolean = true,
    val cornersFromScreen: Boolean = true,
    val theme: AppTheme = AppTheme.NOTURNO,
    val followSystem: Boolean = false,
)

object Presets {
    val SOLIDS = listOf(0xFF29B6FF, 0xFFFF2E7E, 0xFFFFFFFF, 0xFF7CFF6B, 0xFFFFB020, 0xFFB388FF).map { it.toInt() }
    val GRADIENTS = listOf(
        listOf(0xFF29B6FF, 0xFFB388FF, 0xFFFF2E7E, 0xFFFFB020),
        listOf(0xFF7CFF6B, 0xFF29B6FF, 0xFF3D5AFE, 0xFFB388FF),
        listOf(0xFFFFE14D, 0xFFFFB020, 0xFFFF6A3D, 0xFFFF2E7E),
        listOf(0xFFFFFFFF, 0xFFA6F0FF, 0xFF29B6FF, 0xFF1D2BD8),
    ).map { l -> l.map { it.toInt() } }
}

fun lerpColor(a: Int, b: Int, t: Float): Int {
    fun ch(c: Int, s: Int) = (c shr s) and 0xFF
    fun mix(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).toInt().coerceIn(0, 255)
    return (mix(24) shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}

/** Guarda os ajustes no aparelho, em JSON. */
object SettingsStore {
    private const val FILE = "edge_light"
    private const val KEY = "settings_v1"

    fun load(ctx: Context): Settings {
        val raw = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return Settings()
        return runCatching { fromJson(JSONObject(raw)) }.getOrDefault(Settings())
    }

    fun save(ctx: Context, s: Settings) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY, toJson(s).toString()).apply()
    }

    private fun colorJson(c: LineColor) = JSONObject()
        .put("gradient", c.gradient).put("solid", c.solid)
        .put("stops", JSONArray(c.stops))

    private fun colorFrom(o: JSONObject?, def: LineColor): LineColor {
        if (o == null) return def
        val arr = o.optJSONArray("stops")
        val stops = if (arr != null && arr.length() == 4) List(4) { arr.getInt(it) } else def.stops
        return LineColor(o.optBoolean("gradient", def.gradient), o.optInt("solid", def.solid), stops)
    }

    private fun toJson(s: Settings) = JSONObject()
        .put("enabled", s.enabled)
        .put("edge", JSONObject()
            .put("cw", s.edge.clockwise).put("color", colorJson(s.edge.color))
            .put("th", s.edge.thicknessDp.toDouble()).put("trail", s.edge.trailPercent)
            .put("lap", s.edge.lapSeconds.toDouble()).put("laps", s.edge.laps))
        .put("ring", JSONObject()
            .put("on", s.ring.enabled).put("cw", s.ring.clockwise).put("color", colorJson(s.ring.color))
            .put("th", s.ring.thicknessDp.toDouble()).put("trail", s.ring.trailPercent)
            .put("lap", s.ring.lapSeconds.toDouble()))
        .put("pace", s.pace.name).put("glow", s.glow).put("fading", s.fadingTrail)
        .put("corners", s.cornersFromScreen).put("theme", s.theme.name).put("follow", s.followSystem)

    private fun fromJson(o: JSONObject): Settings {
        val d = Settings()
        val e = o.optJSONObject("edge")
        val r = o.optJSONObject("ring")
        return Settings(
            enabled = o.optBoolean("enabled", d.enabled),
            edge = if (e == null) d.edge else EdgeConfig(
                clockwise = e.optBoolean("cw", true),
                color = colorFrom(e.optJSONObject("color"), d.edge.color),
                thicknessDp = e.optDouble("th", 3.0).toFloat(),
                trailPercent = e.optInt("trail", 22),
                lapSeconds = e.optDouble("lap", 1.8).toFloat(),
                laps = e.optInt("laps", 2),
            ),
            ring = if (r == null) d.ring else RingConfig(
                enabled = r.optBoolean("on", true),
                clockwise = r.optBoolean("cw", true),
                color = colorFrom(r.optJSONObject("color"), d.ring.color),
                thicknessDp = r.optDouble("th", 2.0).toFloat(),
                trailPercent = r.optInt("trail", 40),
                lapSeconds = r.optDouble("lap", 0.9).toFloat(),
            ),
            pace = runCatching { Pace.valueOf(o.optString("pace")) }.getOrDefault(d.pace),
            glow = o.optBoolean("glow", true),
            fadingTrail = o.optBoolean("fading", true),
            cornersFromScreen = o.optBoolean("corners", true),
            theme = runCatching { AppTheme.valueOf(o.optString("theme")) }.getOrDefault(d.theme),
            followSystem = o.optBoolean("follow", false),
        )
    }
}

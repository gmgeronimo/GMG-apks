package com.gmg.edgelight.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmg.edgelight.CameraHole
import com.gmg.edgelight.LightRenderer
import com.gmg.edgelight.LineColor
import com.gmg.edgelight.Presets
import com.gmg.edgelight.Settings

/** Fundo de cartão do tema: cor, borda opcional e a "sombra de papel" (Papel). */
fun Modifier.card(shape: Shape, bg: Color, border: Color? = null, shadow: Color? = null, shadowDy: Dp = 3.dp): Modifier {
    var m = this
    if (shadow != null) m = m.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(top = shadowDy.toPx()) { drawOutline(outline, shadow) }
    }
    m = m.clip(shape).background(bg)
    if (border != null) m = m.border(1.5.dp, border, shape)
    return m
}

@Composable
fun SectionHeader(text: String) {
    val st = LocalAppStyle.current
    Text(text, color = st.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp))
}

/** Grupo de linhas no estilo "lista segmentada" do Material 3 Expressive. */
@Composable
fun Group(vararg rows: @Composable ColumnScope.() -> Unit) {
    val st = LocalAppStyle.current
    Column(verticalArrangement = Arrangement.spacedBy(st.groupGap)) {
        rows.forEachIndexed { i, row ->
            val top = if (i == 0) st.radiusBig else st.radiusInner
            val bottom = if (i == rows.lastIndex) st.radiusBig else st.radiusInner
            val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
            Column(
                Modifier.fillMaxWidth().card(shape, st.row, st.cardBorder, st.cardShadow)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = row,
            )
        }
    }
}

@Composable
fun LabelRow(text: String, value: String? = null, sub: String? = null) {
    val st = LocalAppStyle.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = st.fg)
            if (sub != null) Text(sub, fontSize = 14.sp, color = st.muted)
        }
        if (value != null) Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = st.primary, maxLines = 1)
    }
}

/** Botões conectados: o escolhido vira pílula e ganha um ✓ (grupo de botões do M3 Expressive). */
@Composable
fun ButtonGroup(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val st = LocalAppStyle.current
    val bounce = spring<Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val start by animateDpAsState(if (on || i == 0) 24.dp else st.buttonInner, bounce, label = "s")
            val end by animateDpAsState(if (on || i == options.lastIndex) 24.dp else st.buttonInner, bounce, label = "e")
            val w by animateFloatAsState(if (on) 1.25f else 1f, label = "w")
            val shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
            val fg = if (on) st.selFg else st.onSecCont
            Box(
                Modifier.weight(w).height(48.dp)
                    .card(shape, if (on) st.selBg else st.secCont, shadow = if (on) st.selShadow else null, shadowDy = 2.dp)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnimatedVisibility(on) { Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = fg) }
                    Text(label, color = fg, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SliderRow(
    label: String, valueText: String, value: Float,
    range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit,
) {
    val st = LocalAppStyle.current
    Column {
        LabelRow(label, valueText)
        Slider(
            value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = st.primary, activeTrackColor = st.primary, inactiveTrackColor = st.secCont,
                activeTickColor = st.onPrimary.copy(alpha = 0.6f), inactiveTickColor = st.onSecCont.copy(alpha = 0.5f),
            ),
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, sub: String? = null, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val st = LocalAppStyle.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) st.fg else st.muted)
            if (sub != null) Text(sub, fontSize = 14.sp, color = st.muted)
        }
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            thumbContent = if (checked) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = st.primary, checkedThumbColor = st.onPrimary, checkedIconColor = st.primary,
                uncheckedTrackColor = st.rowHighest, uncheckedThumbColor = st.outline, uncheckedBorderColor = st.outline,
            ),
        )
    }
}

/** Escolha de cor de um traço: sólida (6 prontas + personalizada) ou degradê de 4 cores. */
@Composable
fun ColorEditor(color: LineColor, onChange: (LineColor) -> Unit) {
    val st = LocalAppStyle.current
    var editing by remember { mutableStateOf<Int?>(null) } // -1 = cor sólida; 0..3 = cores do degradê

    ButtonGroup(listOf("Sólida", "Degradê"), if (color.gradient) 1 else 0) { onChange(color.copy(gradient = it == 1)) }

    if (!color.gradient) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Presets.SOLIDS.forEach { c -> Swatch(Brush.linearGradient(listOf(Color(c), Color(c))), color.solid == c, "Cor") { onChange(color.copy(solid = c)) } }
            val custom = color.solid !in Presets.SOLIDS
            Swatch(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)),
                custom, "Cor personalizada") { editing = -1 }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Presets.GRADIENTS.forEachIndexed { i, g ->
                Swatch(Brush.linearGradient(g.map { Color(it) }), color.stops == g, "Degradê ${i + 1}", size = 52.dp) {
                    onChange(color.copy(stops = g))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("ponta", "2", "3", "rastro").forEachIndexed { i, name ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(st.buttonInner)).background(st.row2)
                        .clickable { editing = i }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(color.stops[i])).border(1.dp, st.outlineVar, CircleShape))
                    Text(name, fontSize = 13.sp, color = st.muted)
                }
            }
        }
    }

    editing?.let { idx ->
        val initial = if (idx < 0) color.solid else color.stops[idx]
        ColorPickerDialog(initial, onDismiss = { editing = null }) { picked ->
            onChange(if (idx < 0) color.copy(solid = picked) else color.copy(stops = color.stops.toMutableList().also { it[idx] = picked }))
            editing = null
        }
    }
}

@Composable
private fun Swatch(brush: Brush, selected: Boolean, label: String, size: Dp = 40.dp, onClick: () -> Unit) {
    val st = LocalAppStyle.current
    val corner by animateDpAsState(if (selected) 14.dp else size / 2, spring(dampingRatio = 0.6f), label = "c")
    val shape = RoundedCornerShape(corner)
    Box(
        Modifier.size(size + 8.dp)
            .then(if (selected) Modifier.border(2.5.dp, st.fg, RoundedCornerShape(corner + 4.dp)) else Modifier)
            .padding(4.dp).clip(shape).background(brush)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
    )
}

@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    val current = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolher cor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(current)))
                SliderRow("Matiz", "${h.toInt()}°", h, 0f..360f) { h = it }
                SliderRow("Saturação", "${(s * 100).toInt()}%", s, 0f..1f) { s = it }
                SliderRow("Brilho", "${(v * 100).toInt()}%", v, 0f..1f) { v = it }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text("Usar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Celular em miniatura com a luz rodando em loop, usando o mesmo desenho do overlay. */
@Composable
fun LightPreview(settings: Settings, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val renderer = remember { LightRenderer(density * 0.5f) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val geo = remember { IntArray(2) }

    LaunchedEffect(settings) {
        renderer.configure(settings)
        geo[0] = 0
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { now -> elapsed = (now - start) % (renderer.totalMs + 700) }
        }
    }

    Canvas(modifier.size(124.dp, 258.dp).semantics { contentDescription = "Prévia da luz" }) {
        val r = 22.dp.toPx()
        drawRoundRect(Color(0xFF050608), cornerRadius = CornerRadius(r))
        drawRoundRect(Color(0xFF3A3F48), cornerRadius = CornerRadius(r), style = Stroke(1.5.dp.toPx()))
        // Hora e uma notificação, só para dar contexto
        drawRoundRect(Color(0xFF262A31), topLeft = Offset(12.dp.toPx(), 104.dp.toPx()),
            size = Size(size.width - 24.dp.toPx(), 30.dp.toPx()), cornerRadius = CornerRadius(9.dp.toPx()))
        drawRoundRect(Color(0xFFCFD5DC), topLeft = Offset(20.dp.toPx(), 112.dp.toPx()),
            size = Size(40.dp.toPx(), 5.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()))
        val hole = CameraHole(size.width / 2, 14.dp.toPx(), 3.4.dp.toPx())
        drawCircle(Color(0xFF16181D), hole.radius, Offset(hole.cx, hole.cy))
        if (geo[0] != size.width.toInt() || geo[1] != size.height.toInt()) {
            geo[0] = size.width.toInt(); geo[1] = size.height.toInt()
            renderer.setGeometry(geo[0], geo[1], r, hole)
        }
        drawIntoCanvas { renderer.draw(it.nativeCanvas, elapsed) }
    }
}

/** Linha de permissão: ícone + texto (o estado nunca depende só da cor). */
@Composable
fun PermissionRow(title: String, ok: Boolean, pendingText: String, onFix: () -> Unit) {
    val st = LocalAppStyle.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(if (ok) Icons.Filled.Check else Icons.Filled.Warning, null, tint = if (ok) st.ok else st.warn)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = st.fg)
            Text(if (ok) "Ativo" else pendingText, fontSize = 14.sp, color = st.muted)
        }
        if (!ok) PillButton("Ativar", tonal = true, onClick = onFix)
    }
}

@Composable
fun PillButton(text: String, tonal: Boolean = false, modifier: Modifier = Modifier, height: Dp = 44.dp, onClick: () -> Unit) {
    val st = LocalAppStyle.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = height)
            .card(shape, if (tonal) st.secCont else st.btnBg, shadow = if (tonal) null else st.btnShadow)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (tonal) st.onSecCont else st.btnFg, fontWeight = FontWeight.Bold,
            fontSize = if (height > 48.dp) 17.sp else 15.sp)
    }
}

/** Abas simples com indicador embaixo do texto. */
@Composable
fun Tabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val st = LocalAppStyle.current
    Row(Modifier.fillMaxWidth().drawBehind {
        drawLine(st.outlineVar, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
    }) {
        titles.forEachIndexed { i, t ->
            val on = i == selected
            Column(
                Modifier.weight(1f).height(52.dp).selectable(on, role = Role.Tab) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
            ) {
                Text(t, color = if (on) st.primary else st.muted, fontSize = 16.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.width(96.dp).height(3.dp).background(if (on) st.primary else Color.Transparent,
                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
            }
        }
    }
}

/** Cartões de escolha do tema do app, com miniatura das cores. */
@Composable
fun ThemeTile(label: String, chipBg: Color, bar1: Color, bar2: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val st = LocalAppStyle.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.heightIn(min = 100.dp).clip(shape)
            .background(if (selected) st.row2 else Color.Transparent)
            .border(2.dp, if (selected) st.primary else st.outlineVar, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.size(64.dp, 44.dp).clip(RoundedCornerShape(12.dp)).background(chipBg)
                .border(1.dp, Color(0x59808080), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.fillMaxWidth(0.7f).height(8.dp).background(bar1, RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxWidth().height(6.dp).background(bar2, RoundedCornerShape(3.dp)))
        }
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = st.fg)
    }
}

fun Color.argb() = toArgb()

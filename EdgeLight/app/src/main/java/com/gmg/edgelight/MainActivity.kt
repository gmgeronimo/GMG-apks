package com.gmg.edgelight

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmg.edgelight.ui.ButtonGroup
import com.gmg.edgelight.ui.ColorEditor
import com.gmg.edgelight.ui.EdgeLightTheme
import com.gmg.edgelight.ui.Group
import com.gmg.edgelight.ui.LabelRow
import com.gmg.edgelight.ui.LightPreview
import com.gmg.edgelight.ui.LocalAppStyle
import com.gmg.edgelight.ui.PermissionRow
import com.gmg.edgelight.ui.PillButton
import com.gmg.edgelight.ui.SectionHeader
import com.gmg.edgelight.ui.SliderRow
import com.gmg.edgelight.ui.SwitchRow
import com.gmg.edgelight.ui.Tabs
import com.gmg.edgelight.ui.ThemeTile
import com.gmg.edgelight.ui.card
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    /** Muda toda vez que o app volta para a frente: reavalia as permissões. */
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    @Composable
    private fun App() {
        var settings by remember { mutableStateOf(SettingsStore.load(this)) }
        LaunchedEffect(settings) {
            delay(250) // espera o dedo parar no slider antes de gravar
            SettingsStore.save(this@MainActivity, settings)
        }
        EdgeLightTheme(settings.theme, settings.followSystem) {
            val st = LocalAppStyle.current
            LaunchedEffect(st.dark) {
                val t = android.graphics.Color.TRANSPARENT
                val bars = if (st.dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(bars, bars)
            }
            ProvideTextStyle(TextStyle(fontFamily = st.font, color = st.fg)) {
                SettingsScreen(settings, resumeTick) { settings = it }
            }
        }
    }
}

@Composable
private fun SettingsScreen(s: Settings, resumeTick: Int, update: (Settings) -> Unit) {
    val st = LocalAppStyle.current
    val ctx = LocalContext.current
    val view = LocalView.current
    var tab by remember { mutableIntStateOf(0) }

    // Permissões, relidas sempre que o app volta para a frente
    var notifOk by remember { mutableStateOf(false) }
    var overlayOk by remember { mutableStateOf(false) }
    LaunchedEffect(resumeTick) {
        notifOk = Permissions.notificationAccess(ctx)
        overlayOk = Permissions.overlayService(ctx)
    }

    // O aparelho tem câmera em furo? (só dá para saber com a janela já na tela)
    var hasHole by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        hasHole = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            view.rootWindowInsets?.displayCutout?.let { findCameraHole(it, view.resources.displayMetrics.widthPixels) } != null
    }

    Column(
        Modifier.fillMaxSize().background(st.bg).verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 16.dp).padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Edge Light", fontSize = 40.sp, fontWeight = st.titleWeight, lineHeight = 44.sp,
            modifier = Modifier.padding(start = 8.dp, top = 40.dp, bottom = 8.dp))

        // Interruptor principal
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp)
                .card(RoundedCornerShape(50), st.masterBg, shadow = st.masterShadow)
                .padding(start = 24.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Usar Edge Light", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = st.masterFg, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(checked = s.enabled, onCheckedChange = { update(s.copy(enabled = it)) })
        }

        // Prévia
        Column(
            Modifier.fillMaxWidth().card(RoundedCornerShape(st.radiusBig), st.previewBg, st.cardBorder, st.cardShadow)
                .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            LightPreview(s)
            PillButton("Testar no aparelho", modifier = Modifier.fillMaxWidth(), height = 56.dp) {
                val service = EdgeLightService.instance
                if (service == null) {
                    Toast.makeText(ctx, "Ative \"Desenhar sobre a tela\" primeiro", Toast.LENGTH_LONG).show()
                } else {
                    SettingsStore.save(ctx, s)
                    service.flash(s)
                }
            }
        }

        SectionHeader("Permissões")
        Group(
            { PermissionRow("Acesso às notificações", notifOk, "Precisa ativar") { Permissions.openNotificationAccess(ctx) } },
            {
                PermissionRow("Desenhar sobre a tela", overlayOk, "Ative \"Edge Light\" em Acessibilidade") { Permissions.openAccessibility(ctx) }
                if (!overlayOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Text("Se a opção aparecer bloqueada: Informações do app › menu ⋮ › Permitir configurações restritas.",
                        fontSize = 14.sp, color = st.muted)
                }
            },
        )

        Tabs(listOf("Borda da tela", "Anel da câmera"), tab) { tab = it }

        if (tab == 0) EdgeSettings(s, update) else RingSettings(s, hasHole, update)

        SectionHeader("Acabamento")
        Group(
            { SwitchRow("Brilho em volta da linha", s.glow) { update(s.copy(glow = it)) } },
            { SwitchRow("Rastro que se apaga", s.fadingTrail) { update(s.copy(fadingTrail = it)) } },
            { SwitchRow("Cantos lidos da tela", s.cornersFromScreen, "Usa o arredondamento real do aparelho") { update(s.copy(cornersFromScreen = it)) } },
        )

        AppearanceSettings(s, update)
    }
}

private val PACES = listOf("Constante", "Acelera", "Desacelera")

private fun fmt1(v: Float) = String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0").replace('.', ',')
private fun fmtSeconds(v: Float) = String.format(java.util.Locale.US, "%.1f s", v).replace('.', ',')

@Composable
private fun EdgeSettings(s: Settings, update: (Settings) -> Unit) {
    val e = s.edge
    fun edge(f: (EdgeConfig) -> EdgeConfig) = update(s.copy(edge = f(e)))

    SectionHeader("Luz")
    Group(
        { LabelRow("Sentido do giro"); ButtonGroup(listOf("Horário", "Anti-horário"), if (e.clockwise) 0 else 1) { i -> edge { it.copy(clockwise = i == 0) } } },
        { LabelRow("Cor da linha"); ColorEditor(e.color) { c -> edge { it.copy(color = c) } } },
        { LabelRow("Ritmo da animação"); ButtonGroup(PACES, s.pace.ordinal) { update(s.copy(pace = Pace.entries[it])) } },
    )
    SectionHeader("Forma e tempo")
    Group(
        { SliderRow("Espessura", fmt1(e.thicknessDp) + " px", e.thicknessDp, 1f..10f, 17) { v -> edge { it.copy(thicknessDp = v) } } },
        { SliderRow("Comprimento do traço", "${e.trailPercent}%", e.trailPercent.toFloat(), 5f..60f) { v -> edge { it.copy(trailPercent = v.roundToInt()) } } },
        { SliderRow("Tempo de uma volta", fmtSeconds(e.lapSeconds), e.lapSeconds, 0.6f..5f) { v -> edge { it.copy(lapSeconds = (v * 10).roundToInt() / 10f) } } },
        { SliderRow("Voltas", "${e.laps}", e.laps.toFloat(), 1f..6f, 4) { v -> edge { it.copy(laps = v.roundToInt()) } } },
    )
}

@Composable
private fun RingSettings(s: Settings, hasHole: Boolean, update: (Settings) -> Unit) {
    val st = LocalAppStyle.current
    val r = s.ring
    fun ring(f: (RingConfig) -> RingConfig) = update(s.copy(ring = f(r)))

    SectionHeader("Anel")
    if (!hasHole) {
        Group({
            Text("Este aparelho não tem câmera em furo na tela, então o anel não aparece. A luz da borda funciona normalmente.",
                fontSize = 15.sp, color = st.muted)
        })
        return
    }
    Group(
        { SwitchRow("Mostrar anel em volta da câmera", r.enabled) { v -> ring { it.copy(enabled = v) } } },
        { LabelRow("Sentido do giro"); ButtonGroup(listOf("Horário", "Anti-horário"), if (r.clockwise) 0 else 1) { i -> ring { it.copy(clockwise = i == 0) } } },
        { LabelRow("Cor do anel"); ColorEditor(r.color) { c -> ring { it.copy(color = c) } } },
    )
    SectionHeader("Forma e tempo do anel")
    Group(
        { SliderRow("Espessura", fmt1(r.thicknessDp) + " px", r.thicknessDp, 1f..4f, 5) { v -> ring { it.copy(thicknessDp = v) } } },
        { SliderRow("Comprimento do traço", "${r.trailPercent}%", r.trailPercent.toFloat(), 10f..90f) { v -> ring { it.copy(trailPercent = v.roundToInt()) } } },
        { SliderRow("Tempo de uma volta", fmtSeconds(r.lapSeconds), r.lapSeconds, 0.3f..3f) { v -> ring { it.copy(lapSeconds = (v * 10).roundToInt() / 10f) } } },
    )
    Text("O anel acende junto com a borda e segue o mesmo ritmo.", fontSize = 14.sp, color = st.muted,
        modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun AppearanceSettings(s: Settings, update: (Settings) -> Unit) {
    val st = LocalAppStyle.current
    val ctx = LocalContext.current
    // Miniatura do tema Sistema com a cor real do papel de parede, quando o Android permite
    val sysPrimary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(ctx).primary else Color(0xFF5B8C51)
    val notes = mapOf(
        AppTheme.NOTURNO to "Azul-meia-noite, pensado para a luz brilhar mais.",
        AppTheme.PAPEL to "Claro e amigável, com botões que afundam ao toque.",
        AppTheme.SISTEMA to "Segue as cores do papel de parede e o modo claro ou escuro do celular.",
    )

    SectionHeader("Aparência do app")
    Group(
        {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeTile("Noturno", Color(0xFF0B1430), Color(0xFF8AD2F5), Color(0xFF131F44), s.theme == AppTheme.NOTURNO, Modifier.weight(1f)) {
                    update(s.copy(theme = AppTheme.NOTURNO))
                }
                ThemeTile("Papel", Color(0xFFFBF7EA), Color(0xFFF6DC3C), Color(0xFF1B2433), s.theme == AppTheme.PAPEL, Modifier.weight(1f)) {
                    update(s.copy(theme = AppTheme.PAPEL))
                }
                ThemeTile("Sistema", Color(0xFFF3F5EC), sysPrimary, Color(0xFF8F9A8A), s.theme == AppTheme.SISTEMA, Modifier.weight(1f)) {
                    update(s.copy(theme = AppTheme.SISTEMA))
                }
            }
            Text(notes.getValue(s.theme), fontSize = 14.sp, color = st.muted)
        },
        {
            if (s.theme == AppTheme.SISTEMA) {
                SwitchRow("Mudar junto com o sistema", true, "O tema Sistema já troca entre claro e escuro sozinho", enabled = false) {}
            } else {
                SwitchRow("Mudar junto com o sistema", s.followSystem, "Papel no modo claro, Noturno no modo escuro") {
                    update(s.copy(followSystem = it))
                }
            }
        },
    )
}

package com.gmg.edgelight.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gmg.edgelight.AppTheme
import com.gmg.edgelight.R

/** Tudo que muda de um tema para outro, além das cores padrão do Material. */
data class AppStyle(
    val dark: Boolean,
    val bg: Color, val fg: Color, val muted: Color,
    val row: Color, val row2: Color, val rowHighest: Color,
    val primary: Color, val onPrimary: Color,
    val secCont: Color, val onSecCont: Color,
    val outline: Color, val outlineVar: Color,
    val ok: Color, val warn: Color,
    val selBg: Color, val selFg: Color, val selShadow: Color?,
    val btnBg: Color, val btnFg: Color, val btnShadow: Color?,
    val previewBg: Color,
    val masterBg: Color, val masterFg: Color, val masterShadow: Color?,
    val cardBorder: Color?, val cardShadow: Color?,
    val radiusBig: Dp, val radiusInner: Dp, val buttonInner: Dp, val groupGap: Dp,
    val font: FontFamily, val titleWeight: FontWeight,
)

val LocalAppStyle = staticCompositionLocalOf { noturno() }

private val MonaSans = FontFamily(
    Font(R.font.mona_sans_300, FontWeight.Light),
    Font(R.font.mona_sans_500, FontWeight.Medium),
    Font(R.font.mona_sans_600, FontWeight.SemiBold),
    Font(R.font.mona_sans_700, FontWeight.Bold),
)
private val Nunito = FontFamily(
    Font(R.font.nunito_500, FontWeight.Medium),
    Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_700, FontWeight.Bold),
    Font(R.font.nunito_800, FontWeight.ExtraBold),
)

fun noturno() = AppStyle(
    dark = true,
    bg = Color(0xFF0B1430), fg = Color(0xFFEAF2FB), muted = Color(0xFFA9BADB),
    row = Color(0xFF131F44), row2 = Color(0xFF1A2852), rowHighest = Color(0xFF24345F),
    primary = Color(0xFF8AD2F5), onPrimary = Color(0xFF06233A),
    secCont = Color(0xFF26375F), onSecCont = Color(0xFFDBE6FA),
    outline = Color(0xFF8797B8), outlineVar = Color(0xFF2A3866),
    ok = Color(0xFF7FE0B0), warn = Color(0xFFFFC46B),
    selBg = Color(0xFF8AD2F5), selFg = Color(0xFF06233A), selShadow = null,
    btnBg = Color(0xFF8AD2F5), btnFg = Color(0xFF06233A), btnShadow = null,
    previewBg = Color(0xFF131F44),
    masterBg = Color(0xFF1D4A6E), masterFg = Color(0xFFD6F0FF), masterShadow = null,
    cardBorder = null, cardShadow = null,
    radiusBig = 28.dp, radiusInner = 6.dp, buttonInner = 6.dp, groupGap = 2.dp,
    font = MonaSans, titleWeight = FontWeight.Light,
)

fun papel() = AppStyle(
    dark = false,
    bg = Color(0xFFFBF7EA), fg = Color(0xFF1B2433), muted = Color(0xFF535C6A),
    row = Color(0xFFFFFDF6), row2 = Color(0xFFF1EBD6), rowHighest = Color(0xFFDDD5BC),
    primary = Color(0xFF2F5FC4), onPrimary = Color.White,
    secCont = Color(0xFFF1EBD6), onSecCont = Color(0xFF1B2433),
    outline = Color(0xFF76706A), outlineVar = Color(0xFFDDD5BC),
    ok = Color(0xFF2F8A55), warn = Color(0xFFC2410C),
    selBg = Color(0xFFF6DC3C), selFg = Color(0xFF1B2433), selShadow = Color(0xFFC9AE1C),
    btnBg = Color(0xFFF6DC3C), btnFg = Color(0xFF1B2433), btnShadow = Color(0xFFC9AE1C),
    previewBg = Color(0xFF1B2433),
    masterBg = Color(0xFFF6DC3C), masterFg = Color(0xFF1B2433), masterShadow = Color(0xFFC9AE1C),
    cardBorder = Color(0xFFE6DFC8), cardShadow = Color(0xFFE6DFC8),
    radiusBig = 20.dp, radiusInner = 20.dp, buttonInner = 11.dp, groupGap = 10.dp,
    font = Nunito, titleWeight = FontWeight.ExtraBold,
)

/** Tema "Sistema": cores do papel de parede (Material You) e fonte do aparelho. */
fun sistema(c: ColorScheme, dark: Boolean) = AppStyle(
    dark = dark,
    bg = c.surface, fg = c.onSurface, muted = c.onSurfaceVariant,
    row = c.surfaceContainer, row2 = c.surfaceContainerHigh, rowHighest = c.surfaceContainerHighest,
    primary = c.primary, onPrimary = c.onPrimary,
    secCont = c.secondaryContainer, onSecCont = c.onSecondaryContainer,
    outline = c.outline, outlineVar = c.outlineVariant,
    ok = c.tertiary, warn = c.error,
    selBg = c.primary, selFg = c.onPrimary, selShadow = null,
    btnBg = c.primary, btnFg = c.onPrimary, btnShadow = null,
    previewBg = c.surfaceContainerHigh,
    masterBg = c.primaryContainer, masterFg = c.onPrimaryContainer, masterShadow = null,
    cardBorder = null, cardShadow = null,
    radiusBig = 28.dp, radiusInner = 4.dp, buttonInner = 8.dp, groupGap = 2.dp,
    font = FontFamily.Default, titleWeight = FontWeight.Normal,
)

/** Esquema de cores do Material montado a partir do estilo, para Slider, Switch etc. */
private fun AppStyle.toScheme(): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = onPrimary,
        secondaryContainer = secCont, onSecondaryContainer = onSecCont,
        background = bg, onBackground = fg, surface = bg, onSurface = fg,
        onSurfaceVariant = muted, outline = outline, outlineVariant = outlineVar,
        surfaceContainer = row, surfaceContainerHigh = row2, surfaceContainerHighest = rowHighest,
        tertiary = ok, error = warn,
    )
}

@Composable
fun EdgeLightTheme(theme: AppTheme, followSystem: Boolean, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val (style, scheme) = when {
        theme == AppTheme.SISTEMA -> {
            val c = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (systemDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
                else -> if (systemDark) darkColorScheme() else lightColorScheme()
            }
            sistema(c, systemDark) to c
        }
        // "Mudar junto com o sistema": Noturno no modo escuro, Papel no claro
        followSystem -> (if (systemDark) noturno() else papel()).let { it to it.toScheme() }
        theme == AppTheme.PAPEL -> papel().let { it to it.toScheme() }
        else -> noturno().let { it to it.toScheme() }
    }
    CompositionLocalProvider(LocalAppStyle provides style) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

package com.tunnaduong.seeli.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Seeli {
    /** Set by [SeeliTheme]; every colour below follows it (warm paper by day, warm night by night, or Material You). */
    var dark by mutableStateOf(false)
    var dynamicScheme by mutableStateOf<ColorScheme?>(null)

    private fun c(light: Long, night: Long) = Color(if (dark) night else light)
    val BG get() = dynamicScheme?.background ?: c(0xFFF6F5F0, 0xFF161713)
    val Card get() = dynamicScheme?.surfaceContainerHigh ?: c(0xFFECEBE4, 0xFF2A2C25)
    val Surface get() = dynamicScheme?.let { if (dark) it.surfaceContainer else it.surfaceContainerLowest } ?: c(0xFFFFFFFE, 0xFF1F211C)
    val Green get() = dynamicScheme?.primary ?: c(0xFF5F6B4E, 0xFF7D8D65)
    val OnGreen get() = dynamicScheme?.onPrimary ?: Color.White
    val GreenSoft get() = dynamicScheme?.primary?.copy(alpha = 0.45f) ?: c(0xFFA9B79C, 0xFF4A5440)
    val GreenPale get() = dynamicScheme?.primaryContainer ?: c(0xFFC9D3BE, 0xFF3B4433)
    val Ink get() = dynamicScheme?.onBackground ?: c(0xFF2F302B, 0xFFECEAE2)
    val Muted get() = dynamicScheme?.onSurfaceVariant ?: c(0xFF8B8C84, 0xFF9C9D94)
    val Line get() = dynamicScheme?.outlineVariant ?: c(0xFFE2E1DA, 0xFF33352E)
    val Sun get() = dynamicScheme?.tertiary ?: c(0xFFF5D38A, 0xFFE0C07A)
    val Night = Color(0xFF1F211D)
    val Error get() = dynamicScheme?.error ?: c(0xFFB0574A, 0xFFD07A6C)

    /** Mincho / serif for "words": memory notes, vertical text. */
    val Mincho = FontFamily.Serif
}

fun ts(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = Color.Unspecified, tracking: TextUnit = TextUnit.Unspecified, family: FontFamily? = null) =
    TextStyle(fontSize = size.sp, fontWeight = weight, color = color, letterSpacing = tracking, fontFamily = family)

@Composable
fun SeeliTheme(content: @Composable () -> Unit) {
    val system = isSystemInDarkTheme()
    val dark = when (com.tunnaduong.seeli.core.AppStore.appearance) { "dark" -> true; "light" -> false; else -> system }
    val context = LocalContext.current
    val useDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && com.tunnaduong.seeli.core.AppStore.dynamicColor
    val scheme = if (useDynamic) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) {
        darkColorScheme(
            primary = Color(0xFF7D8D65), onPrimary = Color.White,
            background = Color(0xFF161713), surface = Color(0xFF1F211C),
            onSurface = Color(0xFFECEAE2), onBackground = Color(0xFFECEAE2),
            secondary = Color(0xFF4A5440), primaryContainer = Color(0xFF3B4433),
            surfaceVariant = Color(0xFF2A2C25), onSurfaceVariant = Color(0xFF9C9D94),
            outlineVariant = Color(0xFF33352E), error = Color(0xFFD07A6C),
            surfaceContainerLowest = Color(0xFF161713),
            surfaceContainerLow = Color(0xFF1F211C),
            surfaceContainer = Color(0xFF1F211C),
            surfaceContainerHigh = Color(0xFF2A2C25),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF5F6B4E), onPrimary = Color.White,
            background = Color(0xFFF6F5F0), surface = Color(0xFFFFFFFE),
            onSurface = Color(0xFF2F302B), onBackground = Color(0xFF2F302B),
            secondary = Color(0xFFA9B79C), primaryContainer = Color(0xFFC9D3BE),
            surfaceVariant = Color(0xFFECEBE4), onSurfaceVariant = Color(0xFF8B8C84),
            outlineVariant = Color(0xFFE2E1DA), error = Color(0xFFB0574A),
            surfaceContainerLowest = Color(0xFFFFFFFE),
            surfaceContainerLow = Color(0xFFF6F5F0),
            surfaceContainer = Color(0xFFF6F5F0),
            surfaceContainerHigh = Color(0xFFECEBE4),
        )
    }
    Seeli.dark = dark
    Seeli.dynamicScheme = if (useDynamic) scheme else null
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides Seeli.Ink) { content() }
    }
}

/** `.seeliCard()` — rounded paper card with a hairline border. */
fun Modifier.seeliCard(color: Color = Seeli.Surface, radius: Dp = 20.dp, border: Boolean = true): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(color, shape).then(if (border) Modifier.border(1.dp, Seeli.Line, shape) else Modifier)
}

/** Click without the Material ripple, like a SwiftUI plain button. */
@Composable
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)

@Composable
fun PrimaryButton(
    text: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(if (pressed) 0.99f else 1f)
            .alpha(if (pressed) 0.85f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Seeli.Green else Seeli.GreenSoft)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides Seeli.OnGreen) {
            ProvideTextStyle(ts(16, FontWeight.SemiBold, Seeli.OnGreen)) {
                if (content != null) content() else androidx.compose.material3.Text(text ?: "")
            }
        }
    }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .alpha(if (pressed) 0.7f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Seeli.Card)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(text, style = ts(15, FontWeight.Medium, Seeli.Green))
    }
}

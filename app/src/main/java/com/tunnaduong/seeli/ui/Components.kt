package com.tunnaduong.seeli.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunnaduong.seeli.core.AppLanguage
import com.tunnaduong.seeli.core.AppStore
import com.tunnaduong.seeli.core.L10n
import com.tunnaduong.seeli.core.t
import androidx.compose.ui.text.withLink

enum class Leading { HOME, BACK, NONE }

/** Page header: leading icon (home / back), English title, localized subtitle. */
@Composable
fun SeeliHeader(title: String, subtitle: String, leading: Leading = Leading.HOME, onBack: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = ts(17, FontWeight.SemiBold, Seeli.Ink, 1.5.sp))
            Text(subtitle, style = ts(12, color = Seeli.Muted))
        }
        when (leading) {
            Leading.HOME -> IconBox(Icons.Outlined.Home, t("common.home"), Seeli.Green, Modifier.align(Alignment.CenterStart)) { AppStore.path.clear() }
            Leading.BACK -> IconBox(Icons.AutoMirrored.Outlined.ArrowBackIos, t("common.back"), Seeli.Ink, Modifier.align(Alignment.CenterStart), 18.dp) {
                onBack?.invoke() ?: AppStore.pop()
            }
            Leading.NONE -> {}
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector, label: String, tint: Color, modifier: Modifier, size: Dp = 22.dp, onClick: () -> Unit) {
    Box(modifier.size(44.dp).tap(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(size))
    }
}

/** Screen scaffold with the warm paper background. */
@Composable
fun SeeliScreen(
    title: String,
    subtitle: String,
    leading: Leading = Leading.HOME,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Seeli.BG).statusBarsPadding().navigationBarsPadding().imePadding()) {
        SeeliHeader(title, subtitle, leading, onBack)
        content()
    }
}

@Composable
fun EyeLogo(size: Dp = 36.dp, closed: Boolean = false, color: Color = Seeli.Ink) {
    Icon(if (closed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = color, modifier = Modifier.size(size))
}

@Composable
fun Avatar(initial: String, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Seeli.Green), contentAlignment = Alignment.Center) {
        Text(initial, style = ts((size.value * 0.38f).toInt(), FontWeight.SemiBold, Seeli.OnGreen))
    }
}

@Composable
fun Chip(text: String) {
    Text(
        text, style = ts(10, FontWeight.SemiBold, Seeli.Green, 1.2.sp),
        modifier = Modifier.clip(CircleShape).background(Seeli.Surface.copy(alpha = 0.88f)).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = ts(22, FontWeight.SemiBold, Seeli.Ink))
        if (subtitle != null) Text(subtitle, style = ts(13, color = Seeli.Muted))
    }
}

/** Row with a round icon, title, subtitle and chevron (profile / safety lists). */
@Composable
fun MenuRow(icon: ImageVector, title: String, subtitle: String, chevron: Boolean = true, tint: Color = Seeli.Ink, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().seeliCard().tap(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Seeli.Card), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Seeli.Green, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = ts(15, FontWeight.Medium, tint))
            Text(subtitle, style = ts(12, color = Seeli.Muted))
        }
        if (chevron) Icon(Icons.Outlined.ChevronRight, null, tint = Seeli.Muted, modifier = Modifier.size(20.dp))
    }
}

/** Plain text field in a SEELI card (no Material underline). */
@Composable
fun SeeliTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    secure: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    fontSize: Int = 16,
    color: Color = Seeli.Ink,
    placeholderColor: Color = Seeli.Muted.copy(alpha = 0.7f),
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = ts(fontSize, color = color),
        cursorBrush = SolidColor(Seeli.Green),
        visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secure) KeyboardType.Password else keyboard,
            capitalization = capitalization, autoCorrectEnabled = false),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = ts(fontSize, color = placeholderColor))
                inner()
            }
        },
    )
}

@Composable
fun LabeledField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    secure: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = ts(12, FontWeight.Medium, Seeli.Muted))
        Box(Modifier.fillMaxWidth().height(52.dp).seeliCard(radius = 14.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            SeeliTextField(value, onValueChange, placeholder, Modifier.fillMaxWidth(), secure, keyboard)
        }
    }
}

/** Small language switcher (auth screens). */
@Composable
fun LanguageSwitch() {
    val current = L10n.language
    Row(Modifier.clip(CircleShape).background(Seeli.Card).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        AppLanguage.entries.forEach { lang ->
            val on = current == lang
            Text(
                lang.shortName,
                style = ts(11, FontWeight.SemiBold, if (on) Seeli.OnGreen else Seeli.Muted),
                modifier = Modifier.clip(CircleShape).background(if (on) Seeli.Green else Color.Transparent)
                    .tap { L10n.selectLanguage(lang) }.padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
fun ErrorText(text: String?, modifier: Modifier = Modifier) {
    if (text != null) Text(text, style = ts(13, color = Seeli.Error), modifier = modifier)
}

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HGap(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun HairLine(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Seeli.Line))

@Composable
fun Capsule(text: String, modifier: Modifier = Modifier, bg: Color = Seeli.Surface, fg: Color = Seeli.Muted) {
    Text(text, style = ts(11, color = fg), modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp))
}

/** Text with "[label](https://…)" markdown links, the same strings iOS renders. */
@Composable
fun LinkText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val pattern = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        pattern.findAll(text).forEach { m ->
            append(text.substring(last, m.range.first))
            val link = androidx.compose.ui.text.LinkAnnotation.Url(m.groupValues[2],
                androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = Seeli.Green, fontWeight = FontWeight.Medium)))
            withLink(link) { append(m.groupValues[1]) }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
    Text(annotated, style = style, modifier = modifier)
}

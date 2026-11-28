package com.tunnaduong.seeli.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tunnaduong.seeli.MainActivity
import com.tunnaduong.seeli.R
import com.tunnaduong.seeli.core.DateText
import com.tunnaduong.seeli.core.L10n
import com.tunnaduong.seeli.core.WidgetSnapshot
import com.tunnaduong.seeli.core.isMostlyCJK
import com.tunnaduong.seeli.core.t
import com.tunnaduong.seeli.core.tf
import java.time.Instant

private val BG = ColorProvider(Color(0xFFF6F5F0), Color(0xFF161713))
private val CARD = ColorProvider(Color(0xFFECEBE4), Color(0xFF2A2C25))
private val GREEN = ColorProvider(Color(0xFF5F6B4E), Color(0xFF7D8D65))
private val ON_GREEN = ColorProvider(Color.White, Color.White)
private val INK = ColorProvider(Color(0xFF2F302B), Color(0xFFECEAE2))
private val MUTED = ColorProvider(Color(0xFF8B8C84), Color(0xFF9C9D94))

private fun useDynamicColors(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        context.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE).getBoolean("seeli.dynamicColor", true)

@Composable
private fun widgetBg(): ColorProvider =
    if (useDynamicColors(LocalContext.current)) GlanceTheme.colors.surface else BG

@Composable
private fun widgetCard(): ColorProvider =
    if (useDynamicColors(LocalContext.current)) GlanceTheme.colors.surfaceVariant else CARD

@Composable
private fun widgetPrimary(): ColorProvider =
    if (useDynamicColors(LocalContext.current)) GlanceTheme.colors.primary else GREEN

@Composable
private fun widgetOnPrimary(): ColorProvider =
    if (useDynamicColors(LocalContext.current)) GlanceTheme.colors.onPrimary else ON_GREEN

@Composable
private fun widgetInk(): ColorProvider =
    if (useDynamicColors(LocalContext.current)) GlanceTheme.colors.onSurface else INK

@Composable
private fun widgetMuted(): ColorProvider =
    if (useDynamicColors(LocalContext.current)) GlanceTheme.colors.onSurfaceVariant else MUTED

private fun open(context: Context, link: String) =
    actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link), context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))

object SeeliWidgets {
    suspend fun updateAll(context: Context) {
        runCatching {
            TodayWidget().updateAll(context)
            WordsWidget().updateAll(context)
        }
    }
}

@Composable
private fun SignedOut(context: Context) {
    Column(GlanceModifier.fillMaxSize().background(widgetBg()).padding(12.dp).clickable(open(context, "seeli://home")),
        verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(ImageProvider(R.drawable.ic_eye), null, GlanceModifier.size(24.dp), colorFilter = ColorFilter.tint(widgetInk()))
        Text("SEELI", style = TextStyle(widgetInk(), 16.sp, FontWeight.Bold))
        Text(t("widget.signedOut"), style = TextStyle(widgetMuted(), 11.sp))
    }
}

@Composable
private fun CameraButton(size: Int = 38) {
    Box(GlanceModifier.size(size.dp).cornerRadius((size / 2).dp).background(widgetPrimary()), contentAlignment = Alignment.Center) {
        Image(ImageProvider(R.drawable.ic_camera_white), null, GlanceModifier.size((size * 0.45).dp), colorFilter = ColorFilter.tint(widgetOnPrimary()))
    }
}

// ---------------------------------------------------------------- Today

class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(150.dp, 150.dp), DpSize(300.dp, 150.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        L10n.syncFromPrefs(context)
        val snap = WidgetSnapshot.load(context)
        provideContent { GlanceTheme { Today(context, snap) } }
    }

    @Composable
    private fun Today(context: Context, snap: WidgetSnapshot) {
        if (!snap.loggedIn) { SignedOut(context); return }
        val now = Instant.now()
        val due = snap.dueCount(now)
        val sleeping = snap.sleepingCount(now)
        val wide = LocalSize.current.width >= 280.dp
        val bg = widgetBg()
        val card = widgetCard()
        val ink = widgetInk()
        val muted = widgetMuted()
        if (!wide) {
            Column(GlanceModifier.fillMaxSize().background(bg).padding(14.dp)
                .clickable(open(context, if (due > 0) "seeli://reunion" else "seeli://camera"))) {
                Text(DateText.header(now), style = TextStyle(muted, 10.sp), maxLines = 1)
                Spacer(GlanceModifier.defaultWeight())
                if (due > 0) {
                    Image(ImageProvider(R.drawable.ic_sparkles), null, GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(ink))
                    Text(t("widget.reunion.title"), style = TextStyle(ink, 14.sp, FontWeight.Medium))
                } else {
                    Text(t("home.question"), style = TextStyle(ink, 15.sp, fontFamily = FontFamily.Serif))
                }
                Spacer(GlanceModifier.defaultWeight())
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    if (due > 0) Text(t("widget.reunion.tap"), style = TextStyle(muted, 10.sp)) else CameraButton()
                    Spacer(GlanceModifier.defaultWeight())
                    if (sleeping > 0) Text("☾ $sleeping", style = TextStyle(muted, 11.sp))
                }
            }
        } else {
            Row(GlanceModifier.fillMaxSize().background(bg).padding(14.dp)) {
                Column(GlanceModifier.defaultWeight().fillMaxHeight().clickable(open(context, "seeli://camera"))) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(ImageProvider(R.drawable.ic_eye), null, GlanceModifier.size(12.dp), colorFilter = ColorFilter.tint(ink))
                        Text(" SEELI", style = TextStyle(ink, 12.sp, FontWeight.Bold))
                    }
                    Text(DateText.header(now), style = TextStyle(muted, 10.sp), maxLines = 1)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(t("home.question"), style = TextStyle(ink, 15.sp, fontFamily = FontFamily.Serif))
                    Spacer(GlanceModifier.defaultWeight())
                    CameraButton()
                }
                Spacer(GlanceModifier.width(14.dp))
                Column(GlanceModifier.defaultWeight().fillMaxHeight().cornerRadius(16.dp).background(card).padding(12.dp)
                    .clickable(open(context, if (due > 0) "seeli://reunion" else "seeli://memories"))) {
                    when {
                        due > 0 -> {
                            Image(ImageProvider(R.drawable.ic_sparkles), null, GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(ink))
                            Text(t("widget.reunion.title"), style = TextStyle(ink, 13.sp, FontWeight.Medium))
                            Text(if (due > 1) tf("widget.reunion.count", due) else t("widget.reunion.tap"), style = TextStyle(muted, 11.sp))
                        }
                        sleeping > 0 -> {
                            Image(ImageProvider(R.drawable.ic_moon), null, GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(ink))
                            Text(tf("widget.sleeping", sleeping), style = TextStyle(ink, 12.sp))
                            Text(t("widget.sleeping.hint"), style = TextStyle(muted, 10.sp))
                        }
                        else -> {
                            Image(ImageProvider(R.drawable.ic_leaf), null, GlanceModifier.size(18.dp), colorFilter = ColorFilter.tint(ink))
                            Text(t("widget.calm"), style = TextStyle(ink, 12.sp))
                        }
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

// ---------------------------------------------------------------- Words

/** A different note every three hours (by time slot, same as the iOS timeline). */
class WordsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(150.dp, 150.dp), DpSize(300.dp, 150.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        L10n.syncFromPrefs(context)
        val snap = WidgetSnapshot.load(context)
        provideContent { GlanceTheme { Words(context, snap) } }
    }

    @Composable
    private fun Words(context: Context, snap: WidgetSnapshot) {
        if (!snap.loggedIn) { SignedOut(context); return }
        val slot = (System.currentTimeMillis() / 10_800_000L).toInt()
        val word = snap.words.getOrNull(if (snap.words.isEmpty()) 0 else slot % snap.words.size)
        val small = LocalSize.current.width < 280.dp
        val bg = widgetBg()
        val ink = widgetInk()
        val muted = widgetMuted()
        Column(GlanceModifier.fillMaxSize().background(bg).padding(14.dp)
            .clickable(open(context, if (word == null) "seeli://camera" else "seeli://memories"))) {
            Text(t("widget.wordsOfDay"), style = TextStyle(muted, 10.sp))
            Spacer(GlanceModifier.defaultWeight())
            if (word == null) {
                Text(t("widget.words.empty"), style = TextStyle(ink, 13.sp, fontFamily = FontFamily.Serif))
            } else {
                // Glance has no vertical writing; Japanese notes keep the Mincho serif look horizontally.
                Text(word.note, maxLines = if (small) 5 else 4, style = TextStyle(ink, (if (small) 14 else 16).sp,
                    fontStyle = if (word.note.isMostlyCJK) FontStyle.Normal else FontStyle.Italic, fontFamily = FontFamily.Serif))
            }
            Spacer(GlanceModifier.defaultWeight())
            word?.let {
                Text(listOfNotNull(DateText.long(it.date), it.place).joinToString(" · "), maxLines = 2, style = TextStyle(muted, 9.sp))
            }
        }
    }
}

class WordsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordsWidget()
}

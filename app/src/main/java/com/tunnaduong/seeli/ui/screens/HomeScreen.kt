package com.tunnaduong.seeli.ui.screens

import androidx.compose.foundation.layout.offset

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunnaduong.seeli.core.AppStore
import com.tunnaduong.seeli.core.DateText
import com.tunnaduong.seeli.core.Route
import com.tunnaduong.seeli.core.t
import com.tunnaduong.seeli.core.tf
import com.tunnaduong.seeli.ui.EyeLogo
import com.tunnaduong.seeli.ui.HairLine
import com.tunnaduong.seeli.ui.Seeli
import com.tunnaduong.seeli.ui.seeliCard
import com.tunnaduong.seeli.ui.tap
import com.tunnaduong.seeli.ui.ts
import kotlinx.coroutines.delay

@Composable
fun HomeScreen() {
    LaunchedEffect(Unit) {
        while (true) { AppStore.refreshClock(); delay(10_000) }
    }
    val due = AppStore.dueMoments
    Column(Modifier.fillMaxSize().background(Seeli.BG).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(DateText.header(AppStore.now), style = ts(13, color = Seeli.Muted), modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).clip(CircleShape).background(Seeli.Card).tap { AppStore.push(Route.Profile) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, "Profile", tint = Seeli.Green, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.weight(1f).heightIn(min = 20.dp))

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EyeLogo(30.dp)
            Text("SEELI", style = ts(44, FontWeight.Bold, Seeli.Ink, 2.sp))
            Text("See more. Live more.", style = ts(16, color = Seeli.Muted))
        }

        Column(
            Modifier.padding(start = 32.dp, end = 32.dp, top = 36.dp).fillMaxWidth().seeliCard(Seeli.Card, 28.dp, false).padding(vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(t("home.question"), style = ts(19, FontWeight.Medium, Seeli.Ink), textAlign = TextAlign.Center)
            Box(
                Modifier.size(76.dp).shadow(10.dp, CircleShape, ambientColor = Seeli.Green.copy(alpha = 0.25f), spotColor = Seeli.Green.copy(alpha = 0.25f))
                    .clip(CircleShape).background(Seeli.Green).tap { AppStore.push(Route.Camera) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.PhotoCamera, t("camera.title"), tint = Seeli.OnGreen, modifier = Modifier.size(28.dp)) }
        }

        AnimatedVisibility(due.isNotEmpty(), enter = slideInVertically { it } + fadeIn(), exit = fadeOut()) {
            ReunionBanner(due.size, Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp)) {
                due.firstOrNull()?.let { AppStore.push(Route.Reunion(it.id)) }
            }
        }
        if (due.isEmpty() && AppStore.sleepingCount > 0) {
            Text(tf("home.sleeping", AppStore.sleepingCount), style = ts(12, color = Seeli.Muted),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        }

        Spacer(Modifier.weight(1f).heightIn(min = 20.dp))
        HairLine(Modifier.padding(horizontal = 24.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 26.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                NavLabel(Icons.Outlined.People, "Let's connect") { AppStore.push(Route.Connect) }
                CountBadge(AppStore.connectBadge + AppStore.friendRequests.incoming.size, Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = (-10).dp))
            }
            Spacer(Modifier.weight(1f))
            NavLabel(Icons.Outlined.Folder, "Memories") { AppStore.push(Route.Memories) }
        }
    }
}

@Composable
private fun NavLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(Modifier.tap(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = Seeli.Ink, modifier = Modifier.size(22.dp))
        Text(text, style = ts(16, FontWeight.Medium, Seeli.Ink))
    }
}

@Composable
private fun ReunionBanner(count: Int, modifier: Modifier, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(0.95f, 1.15f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "p")
    Row(
        modifier.fillMaxWidth().seeliCard().tap(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(42.dp).scale(pulse).clip(CircleShape).background(Seeli.Sun.copy(alpha = 0.35f)))
            Icon(Icons.Outlined.AutoAwesome, null, tint = Seeli.Green, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(t("home.reunion.title"), style = ts(15, FontWeight.SemiBold, Seeli.Ink))
            Text(if (count > 1) tf("home.reunion.many", count) else t("home.reunion.one"), style = ts(12, color = Seeli.Muted))
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Seeli.Muted)
    }
}

package com.tunnaduong.seeli.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.tunnaduong.seeli.core.AppStore
import com.tunnaduong.seeli.core.GoogleSignIn
import com.tunnaduong.seeli.core.SocialAuthException
import com.tunnaduong.seeli.core.t
import com.tunnaduong.seeli.core.tf
import com.tunnaduong.seeli.core.userMessage
import com.tunnaduong.seeli.ui.ErrorText
import com.tunnaduong.seeli.ui.LinkText
import com.tunnaduong.seeli.ui.EyeLogo
import com.tunnaduong.seeli.ui.Gap
import com.tunnaduong.seeli.ui.LabeledField
import com.tunnaduong.seeli.ui.LanguageSwitch
import com.tunnaduong.seeli.ui.Leading
import com.tunnaduong.seeli.ui.PrimaryButton
import com.tunnaduong.seeli.ui.SeeliHeader
import com.tunnaduong.seeli.ui.Seeli
import com.tunnaduong.seeli.ui.seeliCard
import com.tunnaduong.seeli.ui.tap
import com.tunnaduong.seeli.ui.ts
import kotlinx.coroutines.launch

private enum class AuthRoute { SIGN_UP, CREATED }

@Composable
fun AuthFlow() {
    val path = remember { mutableStateListOf<AuthRoute>() }
    // Sign-up finished on another screen: show the welcome.
    LaunchedEffect(AppStore.welcomePending) {
        if (AppStore.welcomePending && path.lastOrNull() != AuthRoute.CREATED) path.add(AuthRoute.CREATED)
    }
    BackHandler(enabled = path.isNotEmpty() && path.last() != AuthRoute.CREATED) { path.removeAt(path.lastIndex) }
    when (path.lastOrNull()) {
        null -> LoginScreen(onSignUp = { path.add(AuthRoute.SIGN_UP) })
        AuthRoute.SIGN_UP -> SignUpScreen(onBack = { path.removeAt(path.lastIndex) })
        AuthRoute.CREATED -> AccountCreatedScreen()
    }
}

@Composable
private fun LoginScreen(onSignUp: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val canSubmit = email.isNotEmpty() && password.isNotEmpty()

    Column(
        Modifier.fillMaxSize().background(Seeli.BG).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) { Spacer(Modifier.weight(1f)); LanguageSwitch() }

        Column(Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EyeLogo(34.dp)
            Text("SEELI", style = ts(40, FontWeight.Bold, Seeli.Ink, 2.sp))
            Text("See more. Live more.", style = ts(15, color = Seeli.Muted))
        }

        Column(Modifier.padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("LOGIN", style = ts(13, FontWeight.SemiBold, Seeli.Green, 1.5.sp))
            Text(t("login.title"), style = ts(22, FontWeight.SemiBold, Seeli.Ink))
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledField(t("field.email"), "you@example.com", email, { email = it }, keyboard = KeyboardType.Email)
            LabeledField(t("field.password"), "••••••••", password, { password = it }, secure = true)
        }
        ErrorText(error, Modifier.padding(top = 12.dp))

        PrimaryButton(modifier = Modifier.padding(top = 28.dp), enabled = canSubmit && !busy, onClick = {
            busy = true; error = null
            scope.launch {
                try { AppStore.login(email.trim(), password) } catch (e: Exception) { error = e.userMessage() }
                busy = false
            }
        }) { if (busy) CircularProgressIndicator(color = Seeli.OnGreen, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)) else Text(t("login.button")) }

        SocialButtons(onError = { error = it }, modifier = Modifier.padding(top = 16.dp))

        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center) {
            Text(t("login.noAccount"), style = ts(14, color = Seeli.Muted))
            Text(" " + t("login.signUp"), style = ts(14, FontWeight.SemiBold, Seeli.Green), modifier = Modifier.tap(onClick = onSignUp))
        }
        Text(t("login.localNote"), style = ts(11, color = Seeli.Muted), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 24.dp))
    }
}

/** "Continue with Google". Sign in with Apple is iPhone-only (Apple needs a web Services ID on Android). */
@Composable
private fun SocialButtons(onError: (String?) -> Unit, modifier: Modifier = Modifier) {
    if (!GoogleSignIn.isConfigured) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(1.dp).background(Seeli.Line))
            Text(t("auth.or"), style = ts(12, color = Seeli.Muted))
            Box(Modifier.weight(1f).height(1.dp).background(Seeli.Line))
        }
        Box(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(Seeli.Surface)
                .border(1.dp, Seeli.Line, RoundedCornerShape(16.dp))
                .tap(enabled = !busy) {
                    busy = true; onError(null)
                    scope.launch {
                        try {
                            AppStore.signIn(GoogleSignIn.start(context))
                        } catch (e: SocialAuthException) {
                            onError(e.description)
                        } catch (e: Exception) {
                            onError(e.userMessage())
                        }
                        busy = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoogleMark(Modifier.size(18.dp))
                Text(t("auth.google"), style = ts(16, FontWeight.SemiBold, Seeli.Ink))
            }
            if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(20.dp))
        }
    }
}

/** The four-colour Google "G", drawn so no image asset is needed. */
@Composable
private fun GoogleMark(modifier: Modifier) {
    Canvas(modifier) {
        val s = minOf(size.width, size.height)
        val c = Offset(size.width / 2, size.height / 2)
        val r = s * 0.36f
        val w = s * 0.2f
        fun arc(from: Float, to: Float, color: Color) = drawArc(color, from, to - from, false,
            topLeft = Offset(c.x - r, c.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(w, cap = StrokeCap.Butt))
        arc(-40f, 45f, Color(0xFF4285F4))
        arc(45f, 140f, Color(0xFF34A853))
        arc(140f, 215f, Color(0xFFFBBC05))
        arc(215f, 320f, Color(0xFFEA4335))
        drawRect(Color(0xFF4285F4), topLeft = Offset(c.x, c.y - w / 2), size = androidx.compose.ui.geometry.Size(r + w / 2, w))
    }
}

@Composable
private fun SignUpScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var handle by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val cleanHandle = handle.trim().lowercase().replace("@", "")
    // The first thing to fix; the button says it instead of silently staying grey.
    val problem = when {
        name.isBlank() -> "signup.check.name"
        cleanHandle.length !in 2..30 || !cleanHandle.all { it in "abcdefghijklmnopqrstuvwxyz0123456789._-" } -> "signup.check.handle"
        !email.trim().contains("@") || !email.trim().contains(".") -> "signup.check.email"
        password.length < 8 -> "signup.check.password"
        else -> null
    }

    Column(Modifier.fillMaxSize().background(Seeli.BG).statusBarsPadding().navigationBarsPadding().imePadding()) {
        SeeliHeader("SIGN UP", t("signup.header"), Leading.BACK, onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Column(Modifier.padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("signup.title"), style = ts(24, FontWeight.SemiBold, Seeli.Ink))
                Text(t("signup.subtitle"), style = ts(13, color = Seeli.Muted))
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledField(t("field.name"), t("field.name.placeholder"), name, { name = it })
                LabeledField(t("field.handle"), "seeli_user", handle, { handle = it })
                LabeledField(t("field.email"), "you@example.com", email, { email = it }, keyboard = KeyboardType.Email)
                LabeledField(t("field.password"), t("field.password.hint"), password, { password = it }, secure = true)
            }
            // Signing up means agreeing to the Terms of Use (links open in the browser).
            LinkText(t("signup.terms"), ts(12, color = Seeli.Muted), Modifier.padding(top = 22.dp))
            ErrorText(error, Modifier.padding(top = 16.dp))
            PrimaryButton(modifier = Modifier.padding(top = 28.dp), enabled = !busy, onClick = {
                if (problem != null) { error = t(problem); return@PrimaryButton }
                busy = true; error = null
                scope.launch {
                    try {
                        AppStore.signUp(name.trim(), cleanHandle, email.trim(), password)
                    } catch (e: Exception) { error = e.userMessage() }
                    busy = false
                }
            }) { if (busy) CircularProgressIndicator(color = Seeli.OnGreen, strokeWidth = 2.dp, modifier = Modifier.size(22.dp)) else Text(t("signup.button")) }
            SocialButtons(onError = { error = it }, modifier = Modifier.padding(top = 16.dp))
            Gap(24.dp)
        }
    }
}

@Composable
private fun AccountCreatedScreen() {
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val scale by animateFloatAsState(if (appear) 1f else 0.7f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow), label = "s")
    val account = AppStore.account
    Column(
        Modifier.fillMaxSize().background(Seeli.BG).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(120.dp).scale(scale).alpha(if (appear) 1f else 0f).clip(CircleShape).background(Seeli.Card), contentAlignment = Alignment.Center) {
            EyeLogo(44.dp, color = Seeli.Green)
        }
        Text(t("created.title"), style = ts(24, FontWeight.SemiBold, Seeli.Ink), modifier = Modifier.padding(top = 32.dp))
        Text(tf("created.welcome", account?.name ?: ""), style = ts(15, color = Seeli.Muted), modifier = Modifier.padding(top = 8.dp))
        if (account != null && account.hasPassword == false) {
            Text(tf("created.handle", account.handle), style = ts(12, color = Seeli.Muted), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
        Column(Modifier.padding(top = 32.dp).fillMaxWidth().seeliCard(Seeli.Card, border = false).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Principle(Icons.Outlined.Visibility, t("created.p1"))
            Principle(Icons.Outlined.HourglassEmpty, t("created.p2"))
            Principle(Icons.Outlined.People, t("created.p3"))
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(t("created.start"), onClick = {
            AppStore.welcomePending = false
            AppStore.launch { AppStore.refresh() }
        })
        Text("SEE MORE. LIVE MORE.", style = ts(10, FontWeight.Medium, Seeli.Muted, 2.sp), modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun Principle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Seeli.Green, modifier = Modifier.width(22.dp).size(20.dp))
        Text(text, style = ts(14, color = Seeli.Ink), modifier = Modifier.weight(1f))
    }
}

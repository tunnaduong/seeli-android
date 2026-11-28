package com.tunnaduong.seeli.ui.screens

import androidx.compose.material.icons.automirrored.outlined.OpenInNew

import com.tunnaduong.seeli.core.DateText

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material.icons.outlined.Close

import androidx.compose.material.icons.outlined.PersonAddAlt

import androidx.compose.material.icons.outlined.StarOutline

import androidx.compose.material.icons.filled.Star

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunnaduong.seeli.BuildConfig
import com.tunnaduong.seeli.core.ApiClient
import com.tunnaduong.seeli.core.AppLanguage
import com.tunnaduong.seeli.core.AppStore
import com.tunnaduong.seeli.core.Audience
import com.tunnaduong.seeli.core.L10n
import com.tunnaduong.seeli.core.NotificationService
import com.tunnaduong.seeli.core.Route
import com.tunnaduong.seeli.core.t
import com.tunnaduong.seeli.core.tf
import com.tunnaduong.seeli.ui.Avatar
import com.tunnaduong.seeli.ui.Chip
import com.tunnaduong.seeli.ui.ErrorText
import com.tunnaduong.seeli.ui.LabeledField
import com.tunnaduong.seeli.ui.Leading
import com.tunnaduong.seeli.ui.MenuRow
import com.tunnaduong.seeli.ui.PrimaryButton
import com.tunnaduong.seeli.ui.Seeli
import com.tunnaduong.seeli.ui.SeeliScreen
import com.tunnaduong.seeli.ui.SeeliTextField
import com.tunnaduong.seeli.ui.seeliCard
import com.tunnaduong.seeli.ui.tap
import com.tunnaduong.seeli.ui.ts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    var editing by remember { mutableStateOf(false) }
    SeeliScreen("PROFILE", t("profile.subtitle")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppStore.account?.let { account ->
                Row(Modifier.fillMaxWidth().seeliCard(Seeli.Card, 24.dp, false).padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Avatar(account.initial, 72.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(account.name, style = ts(22, FontWeight.SemiBold, Seeli.Ink))
                        Text("@${account.handle}", style = ts(13, color = Seeli.Muted))
                        Row(Modifier.clip(CircleShape).background(Seeli.Surface).tap { editing = true }.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Edit, null, tint = Seeli.Ink, modifier = Modifier.size(14.dp))
                            Text(t("profile.edit"), style = ts(12, color = Seeli.Ink), maxLines = 1)
                        }
                    }
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Seeli.Surface).tap { AppStore.push(Route.MyQR) },
                        contentAlignment = Alignment.Center) { Icon(Icons.Outlined.QrCode2, t("myqr.title"), tint = Seeli.Ink, modifier = Modifier.size(28.dp)) }
                }
                if (account.bio.isNotEmpty()) {
                    Text(account.bio, style = ts(14, color = Seeli.Ink), modifier = Modifier.fillMaxWidth().seeliCard().padding(16.dp))
                }
            }
            Text(t("profile.section"), style = ts(17, FontWeight.SemiBold, Seeli.Ink), modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            MenuRow(Icons.Outlined.People, t("profile.friends"), t("profile.friends.sub")) { AppStore.push(Route.Friends) }
            MenuRow(Icons.Outlined.VerifiedUser, t("profile.privacy"), t("profile.privacy.sub")) { AppStore.push(Route.Privacy) }
            MenuRow(Icons.Outlined.Settings, t("profile.settings"), t("profile.settings.sub")) { AppStore.push(Route.Settings) }
            MenuRow(Icons.Outlined.Verified, "Verified Account", t("profile.verified.sub")) { AppStore.push(Route.Verified) }
            Column(Modifier.padding(top = 20.dp).fillMaxWidth().seeliCard(Seeli.Card, border = false).padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("profile.footer"), style = ts(14, color = Seeli.Ink), textAlign = TextAlign.Center)
                Text("SEE MORE. LIVE MORE.", style = ts(10, color = Seeli.Muted, tracking = 2.sp))
            }
        }
    }
    if (editing) {
        ModalBottomSheet({ editing = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Seeli.BG) {
            EditProfileSheet { editing = false }
        }
    }
}

@Composable
private fun EditProfileSheet(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(AppStore.account?.name ?: "") }
    var handle by remember { mutableStateOf(AppStore.account?.handle ?: "") }
    var bio by remember { mutableStateOf(AppStore.account?.bio ?: "") }
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(t("profile.edit"), style = ts(20, FontWeight.SemiBold, Seeli.Ink))
        LabeledField(t("field.name"), t("field.name.placeholder"), name, { name = it })
        LabeledField(t("field.handle"), "seeli_user", handle, { handle = it })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t("field.bio"), style = ts(12, FontWeight.Medium, Seeli.Muted))
            Box(Modifier.fillMaxWidth().seeliCard(radius = 14.dp).padding(14.dp)) {
                SeeliTextField(bio, { bio = it }, t("field.bio.placeholder"), Modifier.fillMaxWidth(), singleLine = false, minLines = 3)
            }
        }
        ErrorText(AppStore.lastError)
        PrimaryButton(t("common.save"), enabled = name.isNotBlank(), onClick = {
            scope.launch {
                val h = handle.trim().lowercase().replace("@", "")
                if (AppStore.updateProfile(name.trim(), bio, h)) onDone()
            }
        })
    }
}

@Composable
fun FriendsScreen() {
    val scope = rememberCoroutineScope()
    SeeliScreen("MY CIRCLE", t("friends.subtitle"), Leading.BACK) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MenuRow(Icons.Outlined.QrCodeScanner, t("connect.qr.title"), t("connect.qr.body")) { AppStore.push(Route.Scan) }
            val reqs = AppStore.friendRequests
            if (reqs.incoming.isNotEmpty() || reqs.outgoing.isNotEmpty()) {
                Box {
                    MenuRow(Icons.Outlined.PersonAddAlt, t("requests.title"),
                        if (reqs.incoming.isEmpty()) tf("requests.sent.count", reqs.outgoing.size) else tf("requests.incoming.count", reqs.incoming.size)) {
                        AppStore.push(Route.Requests)
                    }
                    CountBadge(reqs.incoming.size, Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
            }
            if (AppStore.friends.isNotEmpty()) {
                Text(t("friends.close.hint"), style = ts(11, color = Seeli.Muted), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            if (AppStore.friends.isEmpty()) {
                Text(t("connect.empty"), style = ts(13, color = Seeli.Muted), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 40.dp))
            }
            AppStore.friends.forEach { f ->
                Row(Modifier.fillMaxWidth().seeliCard().tap { AppStore.push(Route.FriendProfile(f.id)) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Avatar(f.initial, 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(f.name, style = ts(15, FontWeight.Medium, Seeli.Ink))
                        Text("@${f.handle}", style = ts(12, color = Seeli.Muted))
                    }
                    Box(Modifier.size(40.dp).tap { scope.launch { AppStore.setClose(f.id, !f.isClose) } }, contentAlignment = Alignment.Center) {
                        Icon(if (f.isClose) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            if (f.isClose) t("close.remove") else t("close.add"),
                            tint = if (f.isClose) Color(0xFFD9A93E) else Seeli.Muted, modifier = Modifier.size(20.dp))
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = Seeli.Muted)
                }
            }
            Text(tf("friends.count", AppStore.friends.size), style = ts(12, color = Seeli.Muted), modifier = Modifier.padding(top = 12.dp))
        }
    }
}

private enum class FriendAction { REPORT, BLOCK, REMOVE }

@Composable
fun FriendProfileScreen(friendID: String) {
    val scope = rememberCoroutineScope()
    val friend = AppStore.friend(friendID)
    var memo by remember { mutableStateOf(friend?.memo ?: "") }
    var confirm by remember { mutableStateOf<FriendAction?>(null) }
    var reported by remember { mutableStateOf(false) }
    // Save shortly after typing stops.
    LaunchedEffect(memo) {
        delay(800)
        if (memo != AppStore.friend(friendID)?.memo) AppStore.updateMemo(friendID, memo)
    }
    SeeliScreen("PROFILE", t("friend.subtitle"), Leading.BACK) {
        if (friend == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(t("friend.gone"), color = Seeli.Muted) }
            return@SeeliScreen
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().seeliCard(Seeli.Card, 24.dp, false).padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Avatar(friend.initial, 84.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(friend.name, style = ts(22, FontWeight.SemiBold, Seeli.Ink))
                    Text("@${friend.handle}", style = ts(13, color = Seeli.Muted))
                    Chip(if (friend.isClose) "CLOSE FRIEND" else "MY CIRCLE")
                }
            }
            ToggleRow(t("close.toggle"), friend.isClose, Modifier.fillMaxWidth().seeliCard().padding(horizontal = 18.dp, vertical = 12.dp)) { on ->
                scope.launch { AppStore.setClose(friendID, on) }
            }
            Text(t("close.toggle.hint"), style = ts(11, color = Seeli.Muted), modifier = Modifier.padding(start = 4.dp))
            Column(Modifier.fillMaxWidth().heightIn(min = 110.dp).seeliCard().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(t("friend.bio"), style = ts(14, FontWeight.SemiBold, Seeli.Ink))
                Text(friend.bio.ifEmpty { t("friend.bio.empty") }, style = ts(14, color = if (friend.bio.isEmpty()) Seeli.Muted else Seeli.Ink))
            }
            Column(Modifier.fillMaxWidth().seeliCard(Seeli.Card, border = false).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.EditNote, null, tint = Seeli.Ink, modifier = Modifier.size(18.dp))
                    Text(t("friend.memo"), style = ts(14, FontWeight.Medium, Seeli.Ink))
                }
                Text(t("friend.memo.hint"), style = ts(10, color = Seeli.Muted))
                Box(Modifier.fillMaxWidth().seeliCard(radius = 12.dp).padding(14.dp)) {
                    SeeliTextField(memo, { memo = it }, t("friend.memo.placeholder"), Modifier.fillMaxWidth(), singleLine = false, minLines = 2, fontSize = 13)
                }
            }
            Text(t("friend.safety"), style = ts(17, FontWeight.SemiBold, Seeli.Ink), modifier = Modifier.padding(top = 10.dp))
            MenuRow(Icons.Outlined.Flag, t("friend.report"), t("friend.report.sub"), chevron = false) { confirm = FriendAction.REPORT }
            MenuRow(Icons.Outlined.Block, t("friend.block"), t("friend.block.sub"), chevron = false) { confirm = FriendAction.BLOCK }
            MenuRow(Icons.Outlined.PersonRemove, t("friend.remove"), t("friend.remove.sub"), chevron = false) { confirm = FriendAction.REMOVE }
            Text(t("friend.footer"), style = ts(12, color = Seeli.Muted), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        }
    }
    confirm?.let { action ->
        val (title, button) = when (action) {
            FriendAction.REPORT -> t("friend.report.confirm") to t("friend.report")
            FriendAction.BLOCK -> t("friend.block.confirm") to t("friend.block")
            FriendAction.REMOVE -> t("friend.remove.confirm") to t("friend.remove")
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Seeli.BG,
            text = { Text(title, style = ts(15, color = Seeli.Ink)) },
            confirmButton = {
                TextButton({
                    confirm = null
                    scope.launch {
                        when (action) {
                            FriendAction.REPORT -> reported = AppStore.report(friendID)
                            FriendAction.BLOCK -> { AppStore.block(friendID); AppStore.pop() }
                            FriendAction.REMOVE -> { AppStore.removeFriend(friendID); AppStore.pop() }
                        }
                    }
                }) { Text(button, color = Seeli.Error) }
            },
            dismissButton = { TextButton({ confirm = null }) { Text(t("common.cancel"), color = Seeli.Ink) } },
        )
    }
    if (reported) {
        AlertDialog({ reported = false }, containerColor = Seeli.BG, text = { Text(t("friend.reported"), style = ts(15, color = Seeli.Ink)) },
            confirmButton = { TextButton({ reported = false }) { Text("OK", color = Seeli.Green) } })
    }
}

@Composable
fun PrivacyScreen() {
    val scope = rememberCoroutineScope()
    SeeliScreen("PRIVACY", t("profile.privacy"), Leading.BACK) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(t("privacy.default"), style = ts(15, FontWeight.SemiBold, Seeli.Ink))
            // Segmented control, like the iOS picker.
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Seeli.Card).padding(2.dp)) {
                listOf(Audience.ONLY_ME to "ONLY ME", Audience.CLOSE_FRIENDS to "CLOSE", Audience.CIRCLE to "MY CIRCLE").forEach { (a, label) ->
                    val on = AppStore.defaultAudience == a
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) Seeli.Surface else Color.Transparent)
                        .tap { AppStore.defaultAudience = a; AppStore.save() }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(label, style = ts(13, if (on) FontWeight.SemiBold else FontWeight.Normal, Seeli.Ink))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().seeliCard(Seeli.Card, border = false).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Bullet(Icons.Outlined.VisibilityOff, t("privacy.p1"))
                Bullet(Icons.Outlined.LocationOff, t("privacy.p2"))
                Bullet(Icons.Outlined.ChatBubbleOutline, t("privacy.p3"))
                Bullet(Icons.Outlined.BarChart, t("privacy.p4"))
            }
            Text(t("privacy.blocked"), style = ts(15, FontWeight.SemiBold, Seeli.Ink), modifier = Modifier.padding(top = 10.dp))
            if (AppStore.blocked.isEmpty()) Text(t("privacy.blocked.empty"), style = ts(13, color = Seeli.Muted))
            AppStore.blocked.forEach { f ->
                Row(Modifier.fillMaxWidth().seeliCard().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Avatar(f.initial, 36.dp)
                    Text(f.name, style = ts(15, color = Seeli.Ink), modifier = Modifier.weight(1f))
                    Text(t("privacy.unblock"), style = ts(13, color = Seeli.Green), modifier = Modifier.tap { scope.launch { AppStore.unblock(f.id) } })
                }
            }
        }
    }
}

@Composable
private fun Bullet(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Seeli.Green, modifier = Modifier.width(22.dp).size(20.dp))
        Text(text, style = ts(13, color = Seeli.Ink))
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var serverURL by remember { mutableStateOf(ApiClient.baseURLString) }
    LaunchedEffect(Unit) { AppStore.refresh() }

    SeeliScreen("SETTINGS", t("profile.settings.sub"), Leading.BACK) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Group(t("settings.language")) {
                AppLanguage.entries.forEach { lang ->
                    Row(Modifier.fillMaxWidth().tap { L10n.selectLanguage(lang) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lang.displayName, style = ts(15, color = Seeli.Ink), modifier = Modifier.weight(1f))
                        if (L10n.language == lang) Icon(Icons.Outlined.Check, null, tint = Seeli.Green, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Group(t("settings.notifications")) {
                ToggleRow(t("settings.notifications.reunion"), AppStore.notificationsEnabled) { on ->
                    AppStore.notificationsEnabled = on
                    if (!on) NotificationService.cancelAll(context) else if (!NotificationService.hasPermission(context)) AppStore.permissionRequest = "notifications"
                    AppStore.save()
                }
                Text(t("settings.notifications.hint"), style = ts(11, color = Seeli.Muted))
                ToggleRow(t("settings.notifications.friends"), AppStore.friendPushEnabled, Modifier.padding(top = 6.dp)) { AppStore.setFriendPush(it) }
                Text(t("settings.notifications.friends.hint"), style = ts(11, color = Seeli.Muted))
            }
            if (AppStore.serverAllowsDemo) {
                Group(t("settings.demo")) {
                    ToggleRow(t("settings.demo.toggle"), AppStore.demoMode) { AppStore.demoMode = it; AppStore.save() }
                    Text(t("settings.demo.hint"), style = ts(11, color = Seeli.Muted))
                }
            }
            Group(t("settings.server")) {
                SeeliTextField(serverURL, { serverURL = it; ApiClient.baseURLString = it }, ApiClient.defaultBaseURL, Modifier.fillMaxWidth(),
                    keyboard = KeyboardType.Uri, fontSize = 13)
                Text(t("settings.server.hint"), style = ts(11, color = Seeli.Muted))
            }
            Group(t("settings.appearance")) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Seeli.Card).padding(2.dp)) {
                    listOf("system" to t("settings.appearance.system"), "light" to t("settings.appearance.light"), "dark" to t("settings.appearance.dark"))
                        .forEach { (value, label) ->
                            val on = AppStore.appearance == value
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) Seeli.Surface else Color.Transparent)
                                .tap { AppStore.chooseAppearance(value) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(label, style = ts(13, if (on) FontWeight.SemiBold else FontWeight.Normal, Seeli.Ink))
                            }
                        }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    ToggleRow(t("settings.appearance.dynamic"), AppStore.dynamicColor, Modifier.padding(top = 6.dp)) {
                        AppStore.chooseDynamicColor(it)
                    }
                    Text(t("settings.appearance.dynamic.hint"), style = ts(11, color = Seeli.Muted))
                }
            }
            Group(t("settings.legal")) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val q = if (L10n.language == AppLanguage.VI) "" else "?lang=${L10n.language.code}"
                listOf(t("settings.terms") to "https://seeli.cam/terms$q", t("settings.privacyPolicy") to "https://seeli.cam/privacy$q").forEach { (label, url) ->
                    Row(Modifier.fillMaxWidth().tap {
                        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = ts(15, color = Seeli.Ink), modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = Seeli.Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Group(t("settings.account")) {
                AppStore.account?.let { a ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(t("field.email"), style = ts(14, color = Seeli.Muted), modifier = Modifier.weight(1f))
                        Text(a.email, style = ts(14, color = Seeli.Ink))
                    }
                }
                Text(t("settings.logout"), style = ts(15, color = Seeli.Ink), modifier = Modifier.fillMaxWidth().tap { confirmLogout = true }.padding(vertical = 6.dp))
                ErrorText(AppStore.lastError)
            }
            // Destructive action kept apart from logout, at the very bottom of Settings.
            Text(t("settings.delete"), style = ts(13, color = Seeli.Error), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 36.dp).tap { confirmDelete = true }.padding(vertical = 6.dp))
            Text("SEELI ${BuildConfig.VERSION_NAME}", style = ts(11, color = Seeli.Muted), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
    }
    if (confirmLogout) {
        AlertDialog({ confirmLogout = false }, containerColor = Seeli.BG,
            text = { Text(t("settings.logout.confirm"), style = ts(15, color = Seeli.Ink)) },
            confirmButton = { TextButton({ confirmLogout = false; scope.launch { AppStore.logout() } }) { Text(t("settings.logout"), color = Seeli.Ink) } },
            dismissButton = { TextButton({ confirmLogout = false }) { Text(t("common.cancel"), color = Seeli.Muted) } })
    }
    if (confirmDelete) {
        AlertDialog({ confirmDelete = false }, containerColor = Seeli.BG,
            title = { Text(t("settings.delete.confirm"), style = ts(17, FontWeight.SemiBold, Seeli.Ink)) },
            text = { Text(t("settings.delete.body"), style = ts(14, color = Seeli.Muted)) },
            confirmButton = { TextButton({ confirmDelete = false; scope.launch { AppStore.deleteAccount() } }) { Text(t("settings.delete"), color = Seeli.Error) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text(t("common.cancel"), color = Seeli.Ink) } })
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = ts(15, color = Seeli.Ink), modifier = Modifier.weight(1f))
        Switch(value, onChange, colors = seeliSwitchColors())
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = ts(12, FontWeight.SemiBold, Seeli.Muted))
        Column(Modifier.fillMaxWidth().seeliCard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun VerifiedScreen() {
    SeeliScreen("VERIFIED", t("profile.verified.sub"), Leading.BACK) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.Verified, null, tint = Seeli.Green, modifier = Modifier.size(52.dp))
            Text(t("verified.title"), style = ts(20, FontWeight.SemiBold, Seeli.Ink))
            Text(t("verified.body"), style = ts(14, color = Seeli.Muted).copy(lineHeight = 20.sp), textAlign = TextAlign.Center)
            Spacer(Modifier.weight(2f))
        }
    }
}

/** Friend requests: people who scanned my code (accept or decline) and ones I sent. */
@Composable
fun FriendRequestsScreen() {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { AppStore.refresh() }
    SeeliScreen("REQUESTS", t("requests.subtitle"), Leading.BACK) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("requests.incoming"), style = ts(15, FontWeight.SemiBold, Seeli.Ink))
            val reqs = AppStore.friendRequests
            if (reqs.incoming.isEmpty()) Text(t("requests.incoming.empty"), style = ts(13, color = Seeli.Muted))
            reqs.incoming.forEach { r ->
                Row(Modifier.fillMaxWidth().seeliCard().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(r.user.initial, 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(r.user.name, style = ts(15, FontWeight.Medium, Seeli.Ink))
                        Text("@${r.user.handle} · ${DateText.stamp(r.createdAt)}", style = ts(11, color = Seeli.Muted))
                    }
                    if (busy == r.user.id) CircularProgressIndicator(color = Seeli.Green, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    else {
                        Text(t("requests.accept"), style = ts(13, FontWeight.SemiBold, Seeli.OnGreen),
                            modifier = Modifier.clip(CircleShape).background(Seeli.Green).tap {
                                busy = r.user.id
                                scope.launch { AppStore.acceptRequest(r.user.id); busy = null }
                            }.padding(horizontal = 14.dp, vertical = 8.dp))
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Seeli.Card).tap {
                            busy = r.user.id
                            scope.launch { AppStore.dropRequest(r.user.id); busy = null }
                        }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, t("requests.decline"), tint = Seeli.Ink, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            if (reqs.outgoing.isNotEmpty()) {
                Text(t("requests.sent"), style = ts(15, FontWeight.SemiBold, Seeli.Ink), modifier = Modifier.padding(top = 16.dp))
                reqs.outgoing.forEach { r ->
                    Row(Modifier.fillMaxWidth().seeliCard().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(r.user.initial, 36.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(r.user.name, style = ts(14, FontWeight.Medium, Seeli.Ink))
                            Text(t("requests.waiting"), style = ts(11, color = Seeli.Muted))
                        }
                        Text(t("requests.cancel"), style = ts(12, color = Seeli.Muted), modifier = Modifier.tap { scope.launch { AppStore.dropRequest(r.user.id) } })
                    }
                }
            }
        }
    }
}

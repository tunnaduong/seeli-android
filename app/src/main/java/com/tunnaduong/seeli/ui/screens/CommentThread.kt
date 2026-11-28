package com.tunnaduong.seeli.ui.screens

import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunnaduong.seeli.core.AppStore
import com.tunnaduong.seeli.core.CommentThread
import com.tunnaduong.seeli.core.L10n
import com.tunnaduong.seeli.core.ThreadComment
import com.tunnaduong.seeli.core.t
import com.tunnaduong.seeli.core.tf
import com.tunnaduong.seeli.ui.Avatar
import com.tunnaduong.seeli.ui.ErrorText
import com.tunnaduong.seeli.ui.HairLine
import com.tunnaduong.seeli.ui.Seeli
import com.tunnaduong.seeli.ui.SeeliTextField
import com.tunnaduong.seeli.ui.seeliCard
import com.tunnaduong.seeli.ui.tap
import com.tunnaduong.seeli.ui.ts
import kotlinx.coroutines.launch
import java.time.Instant

private val Amber = Color(0xFFA07A2C)

/**
 * A moment's comments, shared by its owner and everyone who can see it. Each comment says
 * who sees it: only the photo's owner, or the whole audience after the owner approves it.
 * Anyone can comment many times and reply. Mirrors CommentThreadView.swift.
 */
@Composable
fun CommentThreadSheet(momentID: String, ownerID: String, ownerName: String, note: String) {
    val scope = rememberCoroutineScope()
    var thread by remember { mutableStateOf<CommentThread?>(null) }
    var loading by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ThreadComment?>(null) }
    var editing by remember { mutableStateOf<ThreadComment?>(null) }
    var visibility by remember { mutableStateOf("owner") }
    var sending by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ThreadComment?>(null) }
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    suspend fun load() {
        AppStore.thread(momentID)?.let { thread = it }
        loading = false
    }
    LaunchedEffect(momentID) { load() }

    val isOwner = thread?.isOwner ?: false
    val comments = thread?.comments ?: emptyList()
    val byId = comments.associateBy { it.id }
    fun isPublic(c: ThreadComment) = c.author.id == ownerID || (c.visibility == "circle" && c.status == "approved")
    fun root(c: ThreadComment): String {
        var cur = c
        var n = 0
        while (true) {
            val p = cur.parentId?.let { byId[it] } ?: break
            cur = p; n++
            if (n > 50) break
        }
        return cur.id
    }
    // Top-level comments, each followed by every answer below it (one level of indent).
    val groups: List<Pair<ThreadComment, List<ThreadComment>>> = run {
        val order = mutableListOf<String>()
        val replies = mutableMapOf<String, MutableList<ThreadComment>>()
        comments.forEach { c ->
            val r = root(c)
            if (r == c.id) order += c.id else replies.getOrPut(r) { mutableListOf() } += c
        }
        order.mapNotNull { id -> byId[id]?.let { it to (replies[id] ?: emptyList()) } }
    }
    LaunchedEffect(comments.size) { if (groups.isNotEmpty()) listState.animateScrollToItem(groups.lastIndex) }

    fun startReply(c: ThreadComment) {
        editing = null; replyTo = c
        runCatching { focus.requestFocus() }
    }
    fun startEdit(c: ThreadComment) {
        replyTo = null; editing = c; draft = c.text
        runCatching { focus.requestFocus() }
    }
    fun send() {
        sending = true
        scope.launch {
            val e = editing
            if (e != null) {
                if (AppStore.editComment(momentID, e.id, draft) != null) { editing = null; draft = ""; load() }
            } else if (AppStore.postComment(momentID, draft, replyTo?.id, visibility) != null) {
                draft = ""; replyTo = null
                visibility = "owner" // each comment starts private; sharing is a deliberate choice
                load()
            }
            sending = false
        }
    }

    Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t("comments.title"), style = ts(18, FontWeight.SemiBold, Seeli.Ink))
            if (note.isNotEmpty()) Text(note, style = ts(13, color = Seeli.Muted, family = Seeli.Mincho), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (isOwner) t("thread.owner.hint") else tf("thread.friend.hint", ownerName), style = ts(11, color = Seeli.Muted))
        }
        HairLine()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator(color = Seeli.Green, modifier = Modifier.align(Alignment.Center))
                comments.isEmpty() -> Text(if (isOwner) t("comments.empty") else t("thread.empty"), style = ts(13, color = Seeli.Muted),
                    textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(horizontal = 30.dp))
                else -> LazyColumn(Modifier.fillMaxWidth(), state = listState) {
                    items(groups, key = { it.first.id }) { (top, replies) ->
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CommentRow(top, false, null, isOwner, ownerID, ownerName, comments, ::isPublic,
                                onReply = ::startReply, onEdit = ::startEdit, onDelete = { confirmDelete = it },
                                onApprove = { c, yes -> scope.launch { if (AppStore.approveComment(momentID, c.id, yes)) load() } })
                            replies.forEach { r ->
                                Box(Modifier.padding(start = 38.dp)) {
                                    CommentRow(r, true, r.parentId?.let { byId[it] }?.author?.name, isOwner, ownerID, ownerName, comments, ::isPublic,
                                        onReply = ::startReply, onEdit = ::startEdit, onDelete = { confirmDelete = it },
                                        onApprove = { c, yes -> scope.launch { if (AppStore.approveComment(momentID, c.id, yes)) load() } })
                                }
                            }
                        }
                    }
                }
            }
        }
        HairLine()
        // Composer
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val e = editing
            val r = replyTo
            if (e != null) ComposerChip(Icons.Outlined.Edit, t("thread.editing")) { editing = null; draft = "" }
            else if (r != null) ComposerChip(Icons.AutoMirrored.Outlined.Reply, tf("thread.replyingTo", if (r.mine) t("thread.you") else r.author.name)) { replyTo = null }
            if (!isOwner && e == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisibilityPill(Icons.Outlined.Lock, tf("thread.only", ownerName), visibility == "owner") { visibility = "owner" }
                    VisibilityPill(Icons.Outlined.People, t("thread.circle"), visibility == "circle") { visibility = "circle" }
                }
                if (visibility == "circle") Text(tf("thread.circle.hint", ownerName), style = ts(11, color = Seeli.Muted))
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).seeliCard(radius = 18.dp).padding(horizontal = 14.dp, vertical = 11.dp)) {
                    SeeliTextField(draft, { draft = it }, t("connect.comment.placeholder"), Modifier.fillMaxWidth().focusRequester(focus),
                        singleLine = false, fontSize = 15)
                }
                val canSend = !sending && draft.isNotBlank()
                Box(Modifier.size(42.dp).clip(CircleShape).background(Seeli.Green.copy(alpha = if (canSend) 1f else 0.4f))
                    .tap { if (canSend) send() }, contentAlignment = Alignment.Center) {
                    if (sending) CircularProgressIndicator(color = Seeli.OnGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    else Icon(Icons.Outlined.ArrowUpward, t("comments.reply.send"), tint = Seeli.OnGreen, modifier = Modifier.size(18.dp))
                }
            }
            if (!sending) ErrorText(AppStore.lastError)
        }
    }

    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = Seeli.BG,
            text = { Text(t("thread.delete.confirm"), style = ts(15, color = Seeli.Ink)) },
            confirmButton = {
                TextButton({
                    confirmDelete = null
                    scope.launch { if (AppStore.deleteComment(momentID, c.id)) load() }
                }) { Text(t("common.delete"), color = Seeli.Error) }
            },
            dismissButton = { TextButton({ confirmDelete = null }) { Text(t("common.cancel"), color = Seeli.Ink) } },
        )
    }
}

@Composable
private fun CommentRow(
    c: ThreadComment, isReply: Boolean, parentName: String?, isOwner: Boolean, ownerID: String, ownerName: String,
    all: List<ThreadComment>, isPublic: (ThreadComment) -> Boolean,
    onReply: (ThreadComment) -> Unit, onEdit: (ThreadComment) -> Unit, onDelete: (ThreadComment) -> Unit,
    onApprove: (ThreadComment, Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Avatar(c.author.initial, if (isReply) 26.dp else 32.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.clip(shape).background(if (c.mine) Seeli.GreenPale.copy(alpha = 0.45f) else Seeli.Surface)
                    .border(1.2.dp, if (c.isPending) Seeli.Sun else Color.Transparent, shape)
                    .tap { onReply(c) }.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (c.mine) t("thread.you") else c.author.name, style = ts(13, FontWeight.SemiBold, Seeli.Ink))
                    if (isReply && parentName != null) {
                        Icon(Icons.AutoMirrored.Outlined.Reply, null, tint = Seeli.Muted, modifier = Modifier.size(12.dp))
                        Text(parentName, style = ts(10, color = Seeli.Muted), maxLines = 1)
                    }
                }
                Text(c.text, style = ts(14, color = Seeli.Ink))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(ago(c.createdAt) + if (c.editedAt != null) " · " + t("thread.edited") else "", style = ts(11, color = Seeli.Muted), maxLines = 1)
                Badge(c, isOwner, ownerID, ownerName, all, isPublic)
                Text(t("comments.reply"), style = ts(11, FontWeight.Medium, Seeli.Green), modifier = Modifier.tap { onReply(c) })
                if (c.mine) Text(t("comments.reply.edit"), style = ts(11, FontWeight.Medium, Seeli.Muted), modifier = Modifier.tap { onEdit(c) })
                if (c.mine || isOwner) Text(t("common.delete"), style = ts(11, FontWeight.Medium, Seeli.Muted), modifier = Modifier.tap { onDelete(c) })
            }
            if (isOwner && c.isPending && !c.mine) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.clip(CircleShape).background(Seeli.Green).tap { onApprove(c, true) }.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Check, null, tint = Seeli.OnGreen, modifier = Modifier.size(14.dp))
                        Text(t("thread.approve"), style = ts(12, FontWeight.Medium, Seeli.OnGreen))
                    }
                    Text(t("thread.keepPrivate"), style = ts(12, FontWeight.Medium, Seeli.Ink),
                        modifier = Modifier.clip(CircleShape).background(Seeli.Card).tap { onApprove(c, false) }.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun Badge(c: ThreadComment, isOwner: Boolean, ownerID: String, ownerName: String, all: List<ThreadComment>, isPublic: (ThreadComment) -> Boolean) {
    val (icon, text, color) = when {
        c.author.id == ownerID -> {
            val parent = c.parentId?.let { id -> all.firstOrNull { it.id == id } }
            if (parent != null && !isPublic(parent)) Triple(Icons.Outlined.Lock, t("thread.private.reply"), Seeli.Muted) else return
        }
        c.isPending -> Triple(Icons.Outlined.HourglassEmpty, t("thread.pending"), Amber)
        c.visibility == "owner" || c.status == "rejected" ->
            Triple(Icons.Outlined.Lock, if (isOwner) t("thread.private.owner") else tf("thread.private", ownerName), Seeli.Muted)
        else -> Triple(Icons.Outlined.People, t("thread.public"), Seeli.Muted)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Text(text, style = ts(11, FontWeight.Medium, color), maxLines = 1)
    }
}

@Composable
private fun VisibilityPill(icon: ImageVector, text: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.clip(CircleShape).background(if (on) Seeli.Green else Seeli.Card).tap(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = if (on) Seeli.OnGreen else Seeli.Ink, modifier = Modifier.size(14.dp))
        Text(text, style = ts(12, FontWeight.Medium, if (on) Seeli.OnGreen else Seeli.Ink), maxLines = 1)
    }
}

@Composable
private fun ComposerChip(icon: ImageVector, text: String, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = Seeli.Muted, modifier = Modifier.size(14.dp))
        Text(text, style = ts(12, color = Seeli.Muted), modifier = Modifier.weight(1f), maxLines = 1)
        Icon(Icons.Outlined.Cancel, t("common.cancel"), tint = Seeli.Muted, modifier = Modifier.size(18.dp).tap(onClick = onClear))
    }
}

/** "3 phút trước", in the app's language. The server clock may run a second ahead: never say "in 0 seconds". */
private fun ago(date: Instant): String {
    val secs = maxOf(1L, (System.currentTimeMillis() - date.toEpochMilli()) / 1000)
    val f = RelativeDateTimeFormatter.getInstance(ULocale.forLocale(L10n.language.locale), null,
        RelativeDateTimeFormatter.Style.SHORT, android.icu.text.DisplayContext.CAPITALIZATION_NONE)
    val (n, unit) = when {
        secs < 60 -> return f.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
        secs < 3600 -> secs / 60 to RelativeDateTimeFormatter.RelativeUnit.MINUTES
        secs < 86_400 -> secs / 3600 to RelativeDateTimeFormatter.RelativeUnit.HOURS
        else -> secs / 86_400 to RelativeDateTimeFormatter.RelativeUnit.DAYS
    }
    return f.format(n.toDouble(), RelativeDateTimeFormatter.Direction.LAST, unit)
}

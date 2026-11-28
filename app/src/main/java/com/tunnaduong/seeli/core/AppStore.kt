package com.tunnaduong.seeli.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tunnaduong.seeli.widget.SeeliWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * App state. The server (seeli-api) is the source of truth; this keeps a local cache so the
 * app opens instantly and works offline, plus an outbox for photos. Mirrors AppStore.swift.
 */
object AppStore {
    private lateinit var ctx: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val api = ApiClient

    // Auth
    var account by mutableStateOf<UserAccount?>(null)
    private var authGeneration by mutableStateOf(0)
    val isLoggedIn: Boolean get() { authGeneration; return account != null && api.token != null }

    // Content
    var moments by mutableStateOf(listOf<Moment>())
    var friends by mutableStateOf(listOf<Friend>())
    var friendPosts by mutableStateOf(listOf<FriendPost>())
    /** Friends' photos opened in this visit to Let's connect. Memory only, never saved. */
    val onceImages = mutableStateMapOf<String, Bitmap>()
    var blocked by mutableStateOf(listOf<PublicUser>())
    var outbox by mutableStateOf(listOf<PendingUpload>())
    /** Comments on my photos, answers to my comments, friend requests (last 30 days). */
    var activity by mutableStateOf(listOf<ActivityItem>())
    var friendRequests by mutableStateOf(FriendRequests())
    /** When Let's connect was last opened; newer posts and activity count toward the badge. */
    var connectSeenAt by mutableStateOf(Instant.EPOCH)

    // Preferences
    var defaultAudience by mutableStateOf(Audience.ONLY_ME)
    var demoMode by mutableStateOf(false)
    /** Whether the server allows demo mode (ALLOW_DEMO). Hidden in Settings when false. */
    var serverAllowsDemo by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(true)
    /** Notify when a friend shares to the circle. */
    var friendPushEnabled by mutableStateOf(true)
    /** "system", "light" or "dark" (kept in prefs, read before the first frame). */
    var appearance by mutableStateOf("system")
        private set
    /** Material You dynamic color on Android 12+ (kept in prefs). */
    var dynamicColor by mutableStateOf(true)
        private set
    fun chooseAppearance(value: String) {
        appearance = value
        ctx.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE).edit().putString("seeli.appearance", value).apply()
    }
    fun chooseDynamicColor(value: Boolean) {
        dynamicColor = value
        ctx.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE).edit().putBoolean("seeli.dynamicColor", value).apply()
        updateWidgets()
    }

    // Session (not persisted)
    var draft = Draft()
    val path = mutableStateListOf<Route>()
    var now by mutableStateOf(Instant.now())
    var pendingInvite by mutableStateOf<FriendInvite?>(null)
    /** A moment whose comments should open (tapped a "new perspective" notification). */
    var pendingCommentsMoment by mutableStateOf<String?>(null)
    /** A friend's post whose comments should open in Let's connect. */
    var pendingThreadPost by mutableStateOf<String?>(null)
    var isSyncing by mutableStateOf(false)
    /** True right after sign-up, so the "account created" screen shows before entering. */
    var welcomePending by mutableStateOf(false)
    var lastError by mutableStateOf<String?>(null)
    /** Asked by the store, answered by the activity (runtime permission dialogs live there). */
    var permissionRequest by mutableStateOf<String?>(null)

    @Serializable
    private data class Health(val allowDemo: Boolean? = null)

    @Serializable
    private data class Snapshot(
        val account: UserAccount? = null,
        val moments: List<Moment> = emptyList(),
        val friends: List<Friend> = emptyList(),
        val friendPosts: List<FriendPost> = emptyList(),
        val blocked: List<PublicUser> = emptyList(),
        val outbox: List<PendingUpload> = emptyList(),
        val defaultAudience: Audience = Audience.ONLY_ME,
        val demoMode: Boolean = false,
        val notificationsEnabled: Boolean = true,
        val friendPushEnabled: Boolean? = true,
        val activity: List<ActivityItem> = emptyList(),
        val friendRequests: FriendRequests = FriendRequests(),
        val connectSeenAt: Date? = null,
    )

    private val cacheFile get() = File(ctx.filesDir, "seeli-cache.json")
    private val outboxDir get() = File(ctx.filesDir, "outbox").apply { mkdirs() }

    fun init(context: Context) {
        ctx = context.applicationContext
        val prefs = ctx.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE)
        appearance = prefs.getString("seeli.appearance", "system") ?: "system"
        dynamicColor = prefs.getBoolean("seeli.dynamicColor", true)
        api.onUnauthorized = { if (account != null) clearSession() }
        load()
        updateWidgets()
    }

    // ---- Persistence

    private fun load() {
        val s = runCatching { ApiClient.json.decodeFromString(Snapshot.serializer(), cacheFile.readText()) }.getOrNull() ?: return
        account = s.account; moments = s.moments; friends = s.friends; friendPosts = s.friendPosts
        blocked = s.blocked; outbox = s.outbox; defaultAudience = s.defaultAudience; demoMode = s.demoMode
        notificationsEnabled = s.notificationsEnabled; friendPushEnabled = s.friendPushEnabled ?: true
        activity = s.activity; friendRequests = s.friendRequests; connectSeenAt = s.connectSeenAt ?: Instant.EPOCH
    }

    fun save() {
        val s = Snapshot(account, moments, friends, friendPosts, blocked, outbox, defaultAudience, demoMode,
            notificationsEnabled, friendPushEnabled, activity, friendRequests, connectSeenAt)
        runCatching {
            val tmp = File(ctx.filesDir, "seeli-cache.json.tmp")
            tmp.writeText(ApiClient.json.encodeToString(Snapshot.serializer(), s))
            tmp.renameTo(cacheFile)
        }
        ctx.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("seeli.friendPush", friendPushEnabled).apply()
        updateWidgets()
    }

    /** Words, dates and counts (never photos) for the home-screen widgets. */
    fun updateWidgets() {
        val snap = if (isLoggedIn) WidgetSnapshot(
            loggedIn = true,
            pendingReveals = allMoments.filter { it.reunitedAt == null }.map { it.revealAt },
            words = allMoments.filter { it.note.isNotEmpty() }.sortedByDescending { it.createdAt }.take(30)
                .map { WidgetSnapshot.Word(it.note, it.createdAt, it.placeName) },
        ) else WidgetSnapshot()
        WidgetSnapshot.save(ctx, snap)
        scope.launch { SeeliWidgets.updateAll(ctx) }
    }

    // ---- Errors

    /** Runs an API call, turning failures into a user-facing message. */
    suspend fun <T> attempt(work: suspend () -> T): T? = try {
        work().also { lastError = null }
    } catch (e: Exception) {
        lastError = e.userMessage(); null
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    // ---- Auth

    @Serializable
    private data class AuthResponse(val token: String, val user: UserAccount, val isNew: Boolean? = null)

    suspend fun signUp(name: String, handle: String, email: String, password: String) {
        val res = api.request<AuthResponse>("POST", "auth/signup",
            mapOf("name" to name, "handle" to handle, "email" to email, "password" to password))
        welcomePending = true
        com.tunnaduong.seeli.ui.screens.WhatsNew.markSeen(ctx) // everything is new to a new account
        startSession(res)
    }

    /** Sign in with Google. Returns true when a new SEELI account was created. */
    suspend fun signIn(c: SocialCredential): Boolean {
        val res = api.request<AuthResponse>("POST", "auth/${c.provider}",
            mapOf("idToken" to c.idToken, "nonce" to c.nonce, "name" to c.name))
        val isNew = res.isNew ?: false
        welcomePending = isNew
        if (isNew) com.tunnaduong.seeli.ui.screens.WhatsNew.markSeen(ctx)
        startSession(res)
        if (!isNew) refresh()
        return isNew
    }

    suspend fun login(email: String, password: String) {
        val res = api.request<AuthResponse>("POST", "auth/login", mapOf("email" to email, "password" to password))
        startSession(res)
        refresh()
    }

    private fun startSession(res: AuthResponse) {
        api.token = res.token
        // A different person on this phone: drop the previous cache.
        if (account?.id != res.user.id) {
            moments = emptyList(); friends = emptyList(); friendPosts = emptyList(); blocked = emptyList(); outbox = emptyList()
            activity = emptyList(); friendRequests = FriendRequests(); connectSeenAt = Instant.EPOCH
        }
        account = res.user
        path.clear()
        authGeneration++
        save()
    }

    suspend fun logout() {
        runCatching { api.send("POST", "auth/logout") }
        clearSession()
    }

    private fun clearSession() {
        api.token = null
        NotificationService.cancelAll(ctx)
        NotificationService.stopFriendWatch(ctx)
        account = null
        moments = emptyList(); friends = emptyList(); friendPosts = emptyList(); blocked = emptyList()
        activity = emptyList(); friendRequests = FriendRequests(); connectSeenAt = Instant.EPOCH
        clearOutbox()
        path.clear()
        authGeneration++
        cacheFile.delete()
        ctx.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE).edit().remove("seeli.feedSeen").apply()
        updateWidgets()
    }

    /** Deletes the account (the server also revokes any linked Apple ID it holds a token for). */
    suspend fun deleteAccount(): Boolean {
        try {
            api.send("DELETE", "me")
        } catch (e: ApiError) {
            // An Apple-linked account created on iPhone: Apple must confirm the deletion there.
            lastError = if (e.code == "apple_reauth_required") t("error.apple_reauth_required").let {
                if (it == "error.apple_reauth_required") t("error.generic") else it
            } else e.description
            return false
        }
        clearSession()
        return true
    }

    suspend fun updateProfile(name: String, bio: String, handle: String? = null): Boolean {
        val body = mutableMapOf<String, Any?>("name" to name, "bio" to bio)
        if (handle != null) body["handle"] = handle
        val user = attempt { api.request<UserAccount>("PATCH", "me", body) } ?: return false
        account = user
        save()
        return true
    }

    suspend fun rotateInviteCode() {
        attempt { api.request<UserAccount>("POST", "me/invite/rotate") }?.let { account = it; save() }
    }

    val myInvite get() = FriendInvite(account?.inviteCode ?: "", account?.name ?: "")

    // ---- Sync

    /** Pull everything from the server and flush the outbox. */
    suspend fun refresh() {
        if (!isLoggedIn || isSyncing) return
        isSyncing = true
        try {
            flushOutbox()
            val meReq = scope.async { runCatching { api.request<UserAccount>("GET", "me") }.getOrNull() }
            val momentsReq = scope.async { runCatching { api.request<List<Moment>>("GET", "moments") }.getOrNull() }
            val friendsReq = scope.async { runCatching { api.request<List<Friend>>("GET", "friends") }.getOrNull() }
            val feedReq = scope.async { runCatching { api.request<List<FriendPost>>("GET", "feed") }.getOrNull() }
            val blocksReq = scope.async { runCatching { api.request<List<PublicUser>>("GET", "blocks") }.getOrNull() }
            val healthReq = scope.async { runCatching { api.request<Health>("GET", "health") }.getOrNull() }
            val since = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
            val activityReq = scope.async { runCatching { api.request<ActivityFeed>("GET", "activity?v=2&since=$since") }.getOrNull() }
            val requestsReq = scope.async { runCatching { api.request<FriendRequests>("GET", "friend-requests") }.getOrNull() }
            healthReq.await()?.let {
                serverAllowsDemo = it.allowDemo ?: false
                if (!serverAllowsDemo) demoMode = false
            }
            val me = meReq.await(); val ms = momentsReq.await(); val fs = friendsReq.await()
            val feed = feedReq.await(); val bl = blocksReq.await()
            if (!isLoggedIn) return
            me?.let { account = it }
            ms?.let { moments = it; rescheduleNotifications() }
            fs?.let { friends = it }
            feed?.let { list ->
                // The server is the source of truth for "viewed" (it can be reset there).
                friendPosts = list
                markFeedSeen(list)
            }
            bl?.let { blocked = it }
            activityReq.await()?.let { activity = it.items }
            requestsReq.await()?.let { friendRequests = it }
            refreshClock()
            save()
            enableFriendPush()
        } finally {
            isSyncing = false
        }
    }

    /** Pull-to-refresh: wait for a sync already running, then fetch again (otherwise the pull did nothing). */
    suspend fun pullRefresh() {
        while (isSyncing) kotlinx.coroutines.delay(150)
        refresh()
    }

    /** Posts already visible in the app should not be announced again by the background check. */
    private fun markFeedSeen(list: List<FriendPost>) {
        val prefs = ctx.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE)
        val newest = list.maxOfOrNull { it.createdAt.toEpochMilli() } ?: 0L
        prefs.edit().putLong("seeli.feedSeen", maxOf(newest, prefs.getLong("seeli.feedSeen", 0L))).apply()
    }

    private fun rescheduleNotifications() {
        if (!notificationsEnabled) return
        val n = Instant.now()
        moments.filter { it.reunitedAt == null && it.revealAt > n }
            .forEach { NotificationService.scheduleReunion(ctx, it.id, it.revealAt) }
    }

    // ---- Push-like notifications for friends' posts

    /** Asks for permission once there is someone to hear from, then starts the feed check. */
    fun enableFriendPush() {
        if (!isLoggedIn || !friendPushEnabled) return
        if (!NotificationService.hasPermission(ctx)) {
            // Only ask once there is someone to hear from; without friends, just wait.
            if (friends.isNotEmpty()) permissionRequest = "notifications"
            return
        }
        scope.launch {
            // Real push through Firebase when this build has it; otherwise check every ~15 minutes.
            // Never both, or every event would be announced twice.
            if (Push.enable(ctx)) NotificationService.stopFriendWatch(ctx) else NotificationService.startFriendWatch(ctx)
        }
    }

    fun setFriendPush(on: Boolean) {
        friendPushEnabled = on
        save()
        if (on) enableFriendPush() else {
            NotificationService.stopFriendWatch(ctx)
            scope.launch { Push.disable(ctx) }
        }
    }

    // ---- Moments

    /** Server moments plus photos still waiting in the outbox. */
    val allMoments: List<Moment>
        get() {
            val pending = outbox.map { p ->
                Moment(p.id, p.note, p.placeName, p.audience, p.createdAt, p.createdAt.plus(30, ChronoUnit.DAYS), isPending = true)
            }
            return (pending + moments).sortedByDescending { it.createdAt }
        }

    fun startDraft(imageData: ByteArray?) {
        val audience = if (defaultAudience == Audience.CLOSE_FRIENDS && friends.none { it.isClose }) Audience.CIRCLE else defaultAudience
        draft = Draft(imageData = imageData, capturedAt = Instant.now(), audience = audience)
    }

    /** Save to the outbox first (so nothing is lost offline), then upload. */
    fun commitDraft() {
        val data = draft.imageData ?: return
        val p = PendingUpload(
            id = "local-${UUID.randomUUID()}",
            note = draft.note.trim(),
            placeName = if (draft.recordPlace) draft.placeName else null,
            audience = draft.audience,
            createdAt = draft.capturedAt,
            demo = demoMode && serverAllowsDemo,
            recipients = if (draft.audience == Audience.SELECTED) draft.recipients else null,
        )
        runCatching { File(outboxDir, "${p.id}.jpg").writeBytes(PhotoPrep.jpeg(data)) }
        outbox = outbox + p
        draft = Draft()
        save()
        scope.launch { flushOutbox() }
        if (notificationsEnabled && !NotificationService.hasPermission(ctx)) permissionRequest = "notifications"
    }

    private var flushing = false

    suspend fun flushOutbox() {
        if (!isLoggedIn || flushing) return
        flushing = true
        try {
            for (p in outbox.toList()) {
                val file = File(outboxDir, "${p.id}.jpg")
                if (!file.exists()) { outbox = outbox.filterNot { it.id == p.id }; continue }
                val moment = try {
                    api.uploadMoment(p, file.readBytes())
                } catch (e: ApiError) {
                    // The chosen people are no longer friends: keep it private instead of retrying forever.
                    if (e.code == "recipients_required") {
                        outbox = outbox.map { if (it.id == p.id) it.copy(audience = Audience.ONLY_ME, recipients = null) else it }
                        save()
                        continue
                    }
                    break
                } catch (_: Exception) { break } // offline: later
                file.delete()
                outbox = outbox.filterNot { it.id == p.id }
                moments = listOf(moment) + moments
                if (notificationsEnabled) NotificationService.scheduleReunion(ctx, moment.id, moment.revealAt)
                save()
            }
        } finally {
            flushing = false
        }
    }

    private fun clearOutbox() {
        outbox = emptyList()
        outboxDir.deleteRecursively()
    }

    val dueMoments: List<Moment>
        get() = moments.filter { it.reunitedAt == null && it.revealAt <= now }.sortedBy { it.createdAt }

    val sleepingCount: Int get() = allMoments.count { it.reunitedAt == null && it.revealAt > now }

    fun moment(id: String) = allMoments.firstOrNull { it.id == id }

    private fun replace(m: Moment) {
        moments = moments.map { if (it.id == m.id) m else it }
        save()
    }

    /** Marks the reunion on the server and returns the fresh moment (with a new photo URL). */
    suspend fun reunite(id: String): Moment? {
        val m = attempt { api.request<Moment>("POST", "moments/$id/reunite") } ?: return null
        replace(m)
        return m
    }

    suspend fun saveReflection(id: String, text: String): Boolean {
        val m = attempt { api.request<Moment>("PATCH", "moments/$id", mapOf("reflection" to text.trim())) } ?: return false
        replace(m)
        return true
    }

    suspend fun deleteMoment(id: String) {
        if (id.startsWith("local-")) {
            File(outboxDir, "$id.jpg").delete()
            outbox = outbox.filterNot { it.id == id }
            save()
            return
        }
        attempt { api.send("DELETE", "moments/$id") } ?: return
        NotificationService.cancel(ctx, id)
        moments = moments.filterNot { it.id == id }
        save()
    }

    /** The owner answers a friend's perspective on their photo. */
    suspend fun replyToComment(momentID: String, commentID: String, text: String): CommentReply? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        return attempt { api.request<CommentReply>("PUT", "moments/$momentID/comments/$commentID/reply", mapOf("text" to clean)) }
    }

    suspend fun comments(id: String): List<Comment> = attempt { api.request<List<Comment>>("GET", "moments/$id/comments") } ?: emptyList()

    // ---- Comment threads

    suspend fun thread(momentID: String): CommentThread? = attempt { api.request<CommentThread>("GET", "moments/$momentID/comments?v=2") }

    suspend fun postComment(momentID: String, text: String, parentID: String?, visibility: String): ThreadComment? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        val body = mutableMapOf<String, Any?>("text" to clean, "visibility" to visibility)
        if (parentID != null) body["parentId"] = parentID
        val c = attempt { api.request<ThreadComment>("POST", "moments/$momentID/comments", body) } ?: return null
        friendPosts = friendPosts.map {
            if (it.id != momentID) it else it.copy(
                commentCount = (it.commentCount ?: 0) + 1, myCommentCount = (it.myCommentCount ?: 0) + 1,
                lastActivityAt = Instant.now(), myComment = if (parentID == null) clean else it.myComment,
            )
        }
        save()
        return c
    }

    suspend fun editComment(momentID: String, commentID: String, text: String): ThreadComment? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        return attempt { api.request<ThreadComment>("PATCH", "moments/$momentID/comments/$commentID", mapOf("text" to clean)) }
    }

    suspend fun deleteComment(momentID: String, commentID: String): Boolean {
        val ok = attempt { api.send("DELETE", "moments/$momentID/comments/$commentID") } != null
        if (ok) scope.launch { refresh() }
        return ok
    }

    suspend fun approveComment(momentID: String, commentID: String, approve: Boolean): Boolean {
        val ok = attempt { api.send("POST", "moments/$momentID/comments/$commentID/approve", mapOf("approve" to approve)) } != null
        if (ok) {
            moments = moments.map { if (it.id == momentID && (it.pendingCount ?: 0) > 0) it.copy(pendingCount = it.pendingCount!! - 1) else it }
            save()
        }
        return ok
    }

    /** Opens a moment's comments: my own photo in Memories, a friend's in Let's connect. */
    fun openComments(momentID: String?) {
        if (momentID == null) { setPath(Route.Connect); return }
        if (moments.any { it.id == momentID }) {
            pendingCommentsMoment = momentID
            setPath(Route.Memories)
        } else {
            pendingThreadPost = momentID
            setPath(Route.Connect)
        }
    }

    // ---- Let's connect badge

    /** New friend posts and activity since Let's connect was last opened (requests are counted separately). */
    val connectBadge: Int
        get() = friendPosts.count { it.createdAt > connectSeenAt && it.viewedAt == null } +
            activity.count { it.at > connectSeenAt && it.type != "friend_request" }

    fun markConnectSeen() {
        connectSeenAt = Instant.now()
        save()
    }

    fun refreshClock() { now = Instant.now() }

    /** seeli:// deep links from widgets, notifications and QR codes. */
    fun handle(uri: Uri) {
        if (uri.scheme != "seeli") return
        when (uri.host) {
            "camera" -> setPath(Route.Camera)
            "memories" -> setPath(Route.Memories)
            "connect" -> setPath(Route.Connect)
            "friend" -> {
                val id = uri.lastPathSegment
                scope.launch {
                    // The new friend may not be in the local list yet.
                    refresh()
                    if (id != null && friend(id) != null) setPath(Route.Profile, Route.Friends, Route.FriendProfile(id))
                    else setPath(Route.Profile, Route.Friends)
                }
            }
            "comments" -> {
                openComments(uri.lastPathSegment)
                scope.launch { refresh() }
            }
            "requests" -> {
                setPath(Route.Connect, Route.Requests)
                scope.launch { refresh() }
            }
            "reunion" -> {
                refreshClock()
                dueMoments.firstOrNull()?.let { setPath(Route.Reunion(it.id)) } ?: path.clear()
            }
            "home" -> path.clear()
            else -> FriendInvite.parse(uri.toString())?.let { pendingInvite = it }
        }
    }

    fun setPath(vararg routes: Route) { path.clear(); path.addAll(routes) }
    fun push(route: Route) { path.add(route) }
    fun pop() { if (path.isNotEmpty()) path.removeAt(path.lastIndex) }

    // ---- Friends

    @Serializable
    data class InvitePreview(
        val user: PublicUser, val isSelf: Boolean, val isFriend: Boolean,
        val requested: Boolean? = null, val requestedMe: Boolean? = null,
    )

    sealed interface AddResult {
        data object Requested : AddResult
        data class Friends(val friend: Friend) : AddResult
    }

    @Serializable
    private data class AddResponse(val status: String, val friend: Friend? = null)

    fun friend(id: String) = friends.firstOrNull { it.id == id }

    val visiblePosts get() = friendPosts.sortedByDescending { it.createdAt }

    suspend fun preview(invite: FriendInvite): InvitePreview = api.request("GET", "invites/${invite.code}")

    /** Sends a friend request (or connects right away if they had already asked). */
    suspend fun addFriend(invite: FriendInvite): AddResult? {
        val r = attempt { api.request<AddResponse>("POST", "friends", mapOf("code" to invite.code, "request" to true)) } ?: return null
        scope.launch { refresh() }
        val f = r.friend
        if (r.status == "friends" && f != null) {
            friends = listOf(f) + friends.filterNot { it.id == f.id }
            save()
            return AddResult.Friends(f)
        }
        return AddResult.Requested
    }

    suspend fun acceptRequest(userID: String): Boolean {
        val f = attempt { api.request<Friend>("POST", "friend-requests/$userID/accept") } ?: return false
        friendRequests = friendRequests.copy(incoming = friendRequests.incoming.filterNot { it.user.id == userID })
        friends = listOf(f) + friends.filterNot { it.id == f.id }
        save()
        scope.launch { refresh() }
        return true
    }

    /** Declines an incoming request or cancels one I sent. */
    suspend fun dropRequest(userID: String) {
        attempt { api.send("DELETE", "friend-requests/$userID") } ?: return
        friendRequests = FriendRequests(
            friendRequests.incoming.filterNot { it.user.id == userID },
            friendRequests.outgoing.filterNot { it.user.id == userID },
        )
        save()
    }

    suspend fun setClose(id: String, close: Boolean) {
        if (friends.none { it.id == id }) return
        friends = friends.map { if (it.id == id) it.copy(close = close) else it }
        save()
        attempt { api.request<Friend>("PATCH", "friends/$id", mapOf("close" to close)) }?.let { f ->
            friends = friends.map { if (it.id == id) f else it }
            save()
        }
    }

    suspend fun updateMemo(id: String, memo: String) {
        if (friends.none { it.id == id }) return
        friends = friends.map { if (it.id == id) it.copy(memo = memo) else it }
        save()
        attempt { api.request<Friend>("PATCH", "friends/$id", mapOf("memo" to memo)) }
    }

    suspend fun removeFriend(id: String) {
        attempt { api.send("DELETE", "friends/$id") } ?: return
        friends = friends.filterNot { it.id == id }
        friendPosts = friendPosts.filterNot { it.author.id == id }
        save()
    }

    suspend fun block(id: String) {
        attempt { api.send("POST", "friends/$id/block") } ?: return
        friend(id)?.let { blocked = blocked + PublicUser(it.id, it.name, it.handle) }
        friends = friends.filterNot { it.id == id }
        friendPosts = friendPosts.filterNot { it.author.id == id }
        save()
    }

    suspend fun unblock(id: String) {
        attempt { api.send("DELETE", "blocks/$id") } ?: return
        blocked = blocked.filterNot { it.id == id }
        save()
    }

    suspend fun report(id: String): Boolean =
        attempt { api.send("POST", "reports", mapOf("userId" to id, "reason" to "user_report")) } != null

    @Serializable
    private data class ViewResponse(val photoUrl: String, val viewedAt: Date)

    /** Opens a friend's photo. The server records the view, so it works only once. */
    private suspend fun openOncePhoto(postID: String): String? = try {
        val r = api.request<ViewResponse>("POST", "moments/$postID/view")
        lastError = null
        markViewed(postID, r.viewedAt)
        r.photoUrl
    } catch (e: ApiError) {
        if (e.code == "already_viewed") markViewed(postID, Instant.now())
        lastError = e.description
        null
    }

    /** Fetches a friend's photo once, straight into memory (no HTTP cache, no disk). */
    suspend fun loadOncePhoto(postID: String) {
        val url = openOncePhoto(postID) ?: return
        repeat(3) { attempt ->
            val bytes = api.fetchBytes(url, api.ephemeral)
            val bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bmp != null) { onceImages[postID] = bmp; return }
            if (attempt < 2) delay(1000)
        }
    }

    fun forgetOncePhotos() = onceImages.clear()

    private fun markViewed(postID: String, at: Instant) {
        if (friendPosts.none { it.id == postID && it.viewedAt == null }) return
        friendPosts = friendPosts.map { if (it.id == postID) it.copy(viewedAt = at) else it }
        save()
    }

    suspend fun sendPerspective(postID: String, text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty()) return false
        attempt { api.send("PUT", "moments/$postID/comment", mapOf("text" to clean)) } ?: return false
        friendPosts = friendPosts.map { if (it.id == postID) it.copy(myComment = clean) else it }
        save()
        return true
    }
}

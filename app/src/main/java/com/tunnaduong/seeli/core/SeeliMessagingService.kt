package com.tunnaduong.seeli.core

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Real push for Android through Firebase Cloud Messaging. The server sends data messages
 * (same events as APNs on iPhone); this builds the notification so a tap deep-links correctly.
 */
class SeeliMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { Push.register(applicationContext, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        val title = d["title"] ?: "SEELI"
        val body = d["body"] ?: return
        val (link, id) = when (d["type"]) {
            "friend_post" -> "seeli://connect" to (d["momentId"] ?: body)
            "comment_reply" -> "seeli://comments/${d["momentId"]}" to "reply" + d["momentId"]
            "friend_request" -> "seeli://requests" to "request" + d["friendId"]
            "comment" -> "seeli://comments/${d["momentId"]}" to "comment" + d["momentId"]
            "friend_added" -> "seeli://friend/${d["friendId"]}" to "friend" + d["friendId"]
            else -> "seeli://home" to body
        }
        NotificationService.post(applicationContext, NotificationService.CH_FRIENDS, id.hashCode(), title, body, link)
    }
}

object Push {
    private const val PREFS = "seeli.prefs"
    private const val KEY = "seeli.fcmToken"

    /** True when Firebase is set up in this build (google-services.json was present). */
    fun available(context: Context) = runCatching { FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null }
        .getOrDefault(false)

    /** Gets the FCM token and sends it to the server. Returns false if push isn't possible. */
    suspend fun enable(context: Context): Boolean {
        if (!available(context)) return false
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return false
        return register(context, token)
    }

    suspend fun register(context: Context, token: String): Boolean {
        ApiClient.init(context)
        if (ApiClient.token == null) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("seeli.friendPush", true)) return false
        val lang = L10n.language.code
        val ok = runCatching {
            ApiClient.send("PUT", "me/device", mapOf("token" to token, "platform" to "android", "lang" to lang))
        }.isSuccess
        if (ok) prefs.edit().putString(KEY, token).apply()
        return ok
    }

    /** Stops pushes to this phone (switch turned off). Logout removes all tokens on the server. */
    suspend fun disable(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY, null) ?: return
        runCatching { ApiClient.send("DELETE", "me/device", mapOf("token" to token)) }
        prefs.edit().remove(KEY).apply()
    }
}

package com.tunnaduong.seeli.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tunnaduong.seeli.MainActivity
import com.tunnaduong.seeli.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.random.Random

// ---------------------------------------------------------------- Notifications

object NotificationService {
    private const val CH_REUNION = "reunion"
    const val CH_FRIENDS = "friends"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REUNION, t("settings.notifications.reunion"), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_FRIENDS, t("settings.notifications.friends"), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun hasPermission(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Same idea as UNTimeIntervalNotificationTrigger: a delayed one-off job per moment. */
    fun scheduleReunion(context: Context, id: String, at: Instant) {
        val delay = maxOf(5_000L, Duration.between(Instant.now(), at).toMillis())
        val work = OneTimeWorkRequestBuilder<ReunionWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString("momentID", id).build())
            .addTag("reunion")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("reunion-$id", ExistingWorkPolicy.REPLACE, work)
    }

    fun cancel(context: Context, id: String) = WorkManager.getInstance(context).cancelUniqueWork("reunion-$id")

    fun cancelAll(context: Context) = WorkManager.getInstance(context).cancelAllWorkByTag("reunion")

    /** Checks the circle feed every ~15 minutes (Android's minimum) and notifies about new posts. */
    fun startFriendWatch(context: Context) {
        val work = PeriodicWorkRequestBuilder<FriendPostWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("friend-watch", ExistingPeriodicWorkPolicy.KEEP, work)
    }

    fun stopFriendWatch(context: Context) = WorkManager.getInstance(context).cancelUniqueWork("friend-watch")

    @SuppressLint("MissingPermission")
    fun post(context: Context, channel: String, id: Int, title: String, body: String, link: String) {
        if (!hasPermission(context)) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    fun postReunion(context: Context, momentID: String) =
        post(context, CH_REUNION, momentID.hashCode(), "SEELI", t("notification.reunion"), "seeli://reunion")

    fun postFriend(context: Context, post: FriendPost) {
        val body = when (L10n.language) {
            AppLanguage.JA -> "さんが瞬間をシェアしました。写真は一度だけ見られます。"
            AppLanguage.VI -> "vừa chia sẻ một khoảnh khắc. Ảnh chỉ hiện một lần thôi."
            AppLanguage.EN -> "just shared a moment. The photo can only be seen once."
            AppLanguage.RU -> "только что поделился(-ась) моментом. Фото можно увидеть лишь раз."
            AppLanguage.ZH -> "刚刚分享了一个瞬间。照片只能看一次。"
        }
        post(context, CH_FRIENDS, post.id.hashCode(), post.author.name, body, "seeli://connect")
    }

    /** A friend's perspective on my photo, or the owner's reply to mine. */
    fun postActivity(context: Context, item: ActivityItem) {
        val (key, link) = when (item.type) {
            "comment" -> "notification.comment" to "seeli://comments/${item.momentId}"
            "friend_added" -> "notification.friend" to "seeli://friend/${item.user.id}"
            else -> "notification.reply" to "seeli://connect"
        }
        val body = tf(key, item.user.name)
        post(context, CH_FRIENDS, (item.type + item.momentId + item.user.id).hashCode(), "SEELI", body, link)
    }
}

class ReunionWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        L10n.syncFromPrefs(applicationContext)
        NotificationService.postReunion(applicationContext, inputData.getString("momentID") ?: return Result.success())
        return Result.success()
    }
}

/** Polls GET /feed and notifies about circle posts newer than the last one seen. */
class FriendPostWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        L10n.syncFromPrefs(ctx)
        ApiClient.init(ctx)
        if (ApiClient.token == null) return Result.success()
        val prefs = ctx.getSharedPreferences("seeli.prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("seeli.friendPush", true)) return Result.success()
        val feed = runCatching { ApiClient.request<List<FriendPost>>("GET", "feed") }.getOrElse { return Result.retry() }
        val last = prefs.getLong("seeli.feedSeen", -1L)
        val newest = feed.maxOfOrNull { it.createdAt.toEpochMilli() } ?: run { checkActivity(ctx, prefs); return Result.success() }
        if (last >= 0) {
            feed.filter { it.createdAt.toEpochMilli() > last }.sortedBy { it.createdAt }.takeLast(5)
                .forEach { NotificationService.postFriend(ctx, it) }
        }
        prefs.edit().putLong("seeli.feedSeen", maxOf(newest, last)).apply()
        checkActivity(ctx, prefs)
        return Result.success()
    }

    /** New perspectives on my photos and replies to mine (iPhone gets these as push). */
    private suspend fun checkActivity(ctx: Context, prefs: android.content.SharedPreferences) {
        val since = prefs.getLong("seeli.activitySeen", -1L)
        val res = runCatching {
            ApiClient.request<ActivityFeed>("GET", "activity?since=${maxOf(since, 0L)}")
        }.getOrNull() ?: return
        // First run only sets the mark, so old comments are not announced.
        if (since >= 0) res.items.sortedBy { it.at }.takeLast(5).forEach { NotificationService.postActivity(ctx, it) }
        prefs.edit().putLong("seeli.activitySeen", res.now.toEpochMilli()).apply()
    }
}

// ---------------------------------------------------------------- Location

/** One-shot location → human readable place name (神奈川県横浜市 / Hà Nội). */
object LocationService {
    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun currentPlaceName(context: Context): String? {
        if (!hasPermission(context)) return null
        val location = currentLocation(context) ?: return null
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            val address = runCatching {
                Geocoder(context, L10n.language.locale).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            }.getOrNull() ?: return@withContext null
            val parts = listOfNotNull(address.adminArea, address.locality ?: address.subAdminArea).distinct()
            when (L10n.language) {
                AppLanguage.JA, AppLanguage.ZH -> parts.joinToString("")
                AppLanguage.VI, AppLanguage.EN, AppLanguage.RU -> parts.reversed().joinToString(", ")
            }.ifEmpty { null }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(context: Context): Location? {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter {
            runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (Build.VERSION.SDK_INT >= 30) {
            for (p in providers) {
                val loc = withTimeoutOrNull(8_000) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        lm.getCurrentLocation(p, signal, context.mainExecutor) { cont.resume(it) }
                    }
                }
                if (loc != null) return loc
            }
        }
        return providers.firstNotNullOfOrNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
    }
}

// ---------------------------------------------------------------- QR

object QRCode {
    fun bitmap(text: String, size: Int = 720): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0))
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        return bmp
    }
}

// ---------------------------------------------------------------- Placeholder photo

/** A soft placeholder "photo" for devices without a camera (emulators). */
object PlaceholderPhoto {
    fun make(): ByteArray {
        val w = 1200
        val h = 1600
        val palettes = listOf(
            intArrayOf(Color.rgb(217, 227, 214), Color.rgb(115, 135, 102)),
            intArrayOf(Color.rgb(245, 224, 191), Color.rgb(158, 140, 122)),
            intArrayOf(Color.rgb(204, 219, 235), Color.rgb(89, 107, 133)),
        )
        val colors = palettes.random()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), colors[0], colors[1], Shader.TileMode.CLAMP)
        })
        val sunX = Random.nextInt(200, 800).toFloat()
        c.drawOval(sunX, 300f, sunX + 240f, 540f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(230, 245, 212, 138) })
        for (i in 0 until 3) {
            val base = h * (0.62f + i * 0.1f)
            val p = Path().apply {
                moveTo(0f, base)
                cubicTo(w * 0.3f, base - 220, w * 0.6f, base + 120, w.toFloat(), base - 60)
                lineTo(w.toFloat(), h.toFloat()); lineTo(0f, h.toFloat()); close()
            }
            c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((255 * (0.12f + i * 0.08f)).toInt(), 26, 26, 26) })
        }
        return ByteArrayOutputStream().use { s -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, s); s.toByteArray() }
    }
}

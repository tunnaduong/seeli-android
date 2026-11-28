package com.tunnaduong.seeli.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val locale: Locale, val shortName: String) {
    VI("vi", "Tiếng Việt", Locale.forLanguageTag("vi-VN"), "VI"),
    JA("ja", "日本語", Locale.JAPAN, "日本"),
    EN("en", "English", Locale.ENGLISH, "EN"),
    RU("ru", "Русский", Locale.forLanguageTag("ru-RU"), "RU"),
    ZH("zh", "中文", Locale.SIMPLIFIED_CHINESE, "中文");

    companion object {
        fun from(code: String?) = entries.firstOrNull { it.code == code }
    }
}

/**
 * In-app language manager. The same string tables as the iOS app (Localizable.strings),
 * read from assets so both platforms share one source of truth.
 * Composables that call [t] re-read [language] and recompose when it changes.
 */
object L10n {
    private const val PREFS = "seeli.prefs"
    private const val KEY = "seeli.language"

    var language by mutableStateOf(AppLanguage.EN)
        private set

    private val tables = mutableMapOf<AppLanguage, Map<String, String>>()
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        val saved = prefs().getString(KEY, null)
        language = AppLanguage.from(saved) ?: when (Locale.getDefault().language) {
            "ja" -> AppLanguage.JA
            "vi" -> AppLanguage.VI
            "ru" -> AppLanguage.RU
            "zh" -> AppLanguage.ZH
            else -> AppLanguage.EN
        }
    }

    /** Widgets run in the same process here, but re-read in case the app process was restarted. */
    fun syncFromPrefs(context: Context) {
        if (!::appContext.isInitialized) init(context)
        AppLanguage.from(prefs().getString(KEY, null))?.let { language = it }
    }

    fun selectLanguage(lang: AppLanguage) {
        language = lang
        prefs().edit().putString(KEY, lang.code).apply()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun string(key: String): String = table(language)[key] ?: key

    private fun table(lang: AppLanguage): Map<String, String> = tables.getOrPut(lang) {
        val text = appContext.assets.open("l10n/${lang.code}.strings").bufferedReader().use { it.readText() }
        parseStrings(text)
    }

    /** Parses Apple's `"key" = "value";` format, handling escapes and comments. */
    internal fun parseStrings(text: String): Map<String, String> {
        val out = HashMap<String, String>()
        val entry = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*;")
        val noComments = text.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")
        for (m in entry.findAll(noComments)) out[unescape(m.groupValues[1])] = unescape(m.groupValues[2])
        return out
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(n)
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }
}

/** Translate a key into the current app language. */
fun t(key: String): String = L10n.string(key)

/** Translate with format arguments. iOS `%@` becomes `%s`. */
fun tf(key: String, vararg args: Any): String =
    String.format(L10n.language.locale, L10n.string(key).replace("%@", "%s").replace(Regex("%(\\d+)\\$@"), "%$1\\\$s"), *args)

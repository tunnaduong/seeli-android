package com.tunnaduong.seeli.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Date formatting with a Japanese "newspaper" flavour (kanji numerals, wareki)
 * and natural Vietnamese formats. Mirrors DateText.swift.
 */
object DateText {
    private val kanjiDigits = listOf("〇", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val lang get() = L10n.language

    /** 0...99 → kanji (42 → 四十二, 18 → 十八). */
    fun kanji(n: Int): String {
        if (n < 0) return n.toString()
        if (n < 10) return kanjiDigits[n]
        if (n < 100) {
            val tens = n / 10
            val ones = n % 10
            val tensPart = if (tens == 1) "十" else kanjiDigits[tens] + "十"
            return tensPart + if (ones == 0) "" else kanjiDigits[ones]
        }
        return n.toString().map { kanjiDigits[it.digitToInt()] }.joinToString("")
    }

    private fun zdt(i: Instant): ZonedDateTime = i.atZone(zone)

    /** Japanese era name and year (令和 since 2019-05-01, 平成 before). */
    private fun wareki(d: LocalDate): Pair<String, Int> =
        if (d >= LocalDate.of(2019, 5, 1)) "令和" to d.year - 2018 else "平成" to d.year - 1988

    /** Home header. ja: 令和八年十一月十八日 / vi: Thứ Ba, 24 tháng 11, 2026 */
    fun header(i: Instant): String {
        val d = zdt(i)
        return when (lang) {
            AppLanguage.JA -> {
                val (era, year) = wareki(d.toLocalDate())
                val y = if (year == 1) "元" else kanji(year)
                "$era${y}年${kanji(d.monthValue)}月${kanji(d.dayOfMonth)}日"
            }
            AppLanguage.VI -> {
                val f = DateTimeFormatter.ofPattern("EEEE, d 'tháng' M, yyyy", lang.locale)
                d.format(f).replaceFirstChar { it.uppercase() }
            }
            AppLanguage.EN -> d.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", lang.locale))
            AppLanguage.RU -> {
                val f = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", lang.locale)
                d.format(f).replaceFirstChar { it.uppercase() }
            }
            AppLanguage.ZH -> d.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", lang.locale))
        }
    }

    /** Stamp beside notes. ja: 十一月十八日 午後四時四十二分 / vi: 18 tháng 11 · 16:42 */
    fun stamp(i: Instant): String {
        val d = zdt(i)
        return when (lang) {
            AppLanguage.JA -> {
                val ampm = if (d.hour < 12) "午前" else "午後"
                val minute = if (d.minute == 0) "" else "${kanji(d.minute)}分"
                "${kanji(d.monthValue)}月${kanji(d.dayOfMonth)}日 $ampm${kanji(d.hour % 12)}時$minute"
            }
            AppLanguage.VI -> String.format("%d tháng %d · %02d:%02d", d.dayOfMonth, d.monthValue, d.hour, d.minute)
            AppLanguage.EN -> d.format(DateTimeFormatter.ofPattern("MMM d · HH:mm", lang.locale))
            AppLanguage.RU -> d.format(DateTimeFormatter.ofPattern("d MMM · HH:mm", lang.locale))
            AppLanguage.ZH -> String.format("%d月%d日 · %02d:%02d", d.monthValue, d.dayOfMonth, d.hour, d.minute)
        }
    }

    /** Long date for the reunion screen. */
    fun long(i: Instant): String {
        val pattern = when (lang) {
            AppLanguage.JA, AppLanguage.ZH -> "yyyy年M月d日"
            AppLanguage.VI -> "d 'tháng' M, yyyy"
            AppLanguage.EN -> "MMMM d, yyyy"
            AppLanguage.RU -> "d MMMM yyyy"
        }
        return zdt(i).format(DateTimeFormatter.ofPattern(pattern, lang.locale))
    }

    fun monthTitle(year: Int, month: Int): String = when (lang) {
        AppLanguage.JA, AppLanguage.ZH -> "${year}年 ${month}月"
        AppLanguage.VI -> "Tháng $month, $year"
        AppLanguage.EN, AppLanguage.RU ->
            LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("LLLL yyyy", lang.locale)).replaceFirstChar { it.uppercase() }
    }

    fun daysAgo(i: Instant, now: Instant = Instant.now()): Int =
        ChronoUnit.DAYS.between(zdt(i).toLocalDate(), zdt(now).toLocalDate()).toInt()
}

/** True when the text is mostly CJK — decides vertical (tategaki) layout. */
val String.isMostlyCJK: Boolean
    get() {
        val letters = codePoints().toArray().filter { cp ->
            !Character.isWhitespace(cp) && Character.getType(cp).let { type ->
                type != Character.OTHER_PUNCTUATION.toInt() && type != Character.START_PUNCTUATION.toInt() &&
                    type != Character.END_PUNCTUATION.toInt() && type != Character.DASH_PUNCTUATION.toInt() &&
                    type != Character.CONNECTOR_PUNCTUATION.toInt() && type != Character.INITIAL_QUOTE_PUNCTUATION.toInt() &&
                    type != Character.FINAL_QUOTE_PUNCTUATION.toInt()
            }
        }
        if (letters.isEmpty()) return false
        val cjk = letters.count { s ->
            s in 0x3040..0x30FF || s in 0x4E00..0x9FFF || s in 0x3400..0x4DBF || s in 0xFF66..0xFF9F
        }
        return cjk.toDouble() / letters.size > 0.5
    }

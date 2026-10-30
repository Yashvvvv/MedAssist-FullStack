package com.medassist.app.ui.common

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IndianLocale = Locale("en", "IN")
private val rupeeFormat: NumberFormat
    get() = NumberFormat.getCurrencyInstance(IndianLocale).apply { maximumFractionDigits = 2; minimumFractionDigits = 2 }
private val rupeeRound: NumberFormat
    get() = NumberFormat.getCurrencyInstance(IndianLocale).apply { maximumFractionDigits = 0 }

/** ₹1,23,456.50 with Indian digit grouping. */
fun BigDecimal.rupees(): String = rupeeFormat.format(this)

/** ₹1,23,457 for dashboards where paise are noise. */
fun BigDecimal.rupeesRounded(): String = rupeeRound.format(this)

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", IndianLocale)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", IndianLocale)
private val dateTime = DateTimeFormatter.ofPattern("d MMM, h:mm a", IndianLocale)

/** "29 Oct 2026" from an ISO date. */
fun String.asDate(): String = runCatching { LocalDate.parse(this).format(dayMonthYear) }.getOrDefault(this)

/** "29 Oct" from an ISO date. */
fun String.asShortDate(): String = runCatching { LocalDate.parse(this).format(dayMonth) }.getOrDefault(this)

/** "29 Oct, 3:40 pm" from an ISO instant, in the phone's time zone. */
fun String.asDateTime(): String =
    runCatching { Instant.parse(this).atZone(ZoneId.systemDefault()).format(dateTime) }.getOrDefault(this)

/** "just now", "5 min ago", "3 h ago", else the date. */
fun String.asRelative(now: Instant = Instant.now()): String = runCatching {
    val then = Instant.parse(this)
    val d = Duration.between(then, now)
    when {
        d.toMinutes() < 1 -> "just now"
        d.toMinutes() < 60 -> "${d.toMinutes()} min ago"
        d.toHours() < 24 -> "${d.toHours()} h ago"
        else -> then.atZone(ZoneId.systemDefault()).format(dayMonth)
    }
}.getOrDefault(this)

fun Double.km(): String = if (this < 1) "${(this * 1000).toInt()} m" else String.format(IndianLocale, "%.1f km", this)

/** Today as the server's business date (India), for report ranges. */
fun todayIst(): LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata"))

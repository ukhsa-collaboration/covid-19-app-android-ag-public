package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import uk.nhs.nhsx.covid19.android.app.R
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun LocalDateTime.daysUntilToday(clock: Clock): Int =
    until(
        LocalDateTime.now(clock), ChronoUnit.DAYS
    ).toInt()

fun LocalDateTime.hoursUntilNow(clock: Clock): Int =
    until(
        LocalDateTime.now(clock),
        ChronoUnit.HOURS
    ).toInt()

val lastDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH")

private val uiTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.uiFormat(context: Context): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy", context.getResourcesLocale())
        .format(this)

fun LocalTime.uiFormat(): String = uiTimeFormatter.format(this)

fun LocalDateTime.uiFormat(context: Context): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", context.getResourcesLocale())
        .format(this)

fun Instant.roundDownToNearestQuarter(): Instant {
    val now = this.atZone(ZoneOffset.UTC)

    val roundedTime = when (now.minute) {
        in 0..14 -> now.withMinute(0)
        in 15..29 -> now.withMinute(15)
        in 30..44 -> now.withMinute(30)
        in 45..59 -> now.withMinute(45)
        else -> now
    }

    return roundedTime.truncatedTo(ChronoUnit.MINUTES).toInstant()
}

fun Instant.roundUpToNearestQuarter(): Instant {
    val now = this.atZone(ZoneOffset.UTC)

    val roundedTime = when (now.minute) {
        in 0..14 -> now.withMinute(15)
        in 15..29 -> now.withMinute(30)
        in 30..44 -> now.withMinute(45)
        in 45..59 -> now.plusHours(1).withMinute(0)
        else -> now
    }

    return roundedTime.truncatedTo(ChronoUnit.MINUTES).toInstant()
}

fun Instant.getNextLocalMidnightTime(clock: Clock): Instant {
    val nextMidnight = this
        .atZone(clock.zone)
        .plusDays(1)
        .truncatedTo(ChronoUnit.DAYS)

    return nextMidnight.toInstant()
}

fun Instant.toISOSecondsFormat(): String =
    DateTimeFormatter.ISO_INSTANT.format(this.truncatedTo(ChronoUnit.SECONDS))

fun Context.getResourcesLocale(): Locale =
    try {
        Locale(resources.getString(R.string.locale))
    } catch (e: NullPointerException) {
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            resources.configuration.locale
        }
    }

package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.util.MinuteIntervals.FIRST_QUARTER
import uk.nhs.nhsx.covid19.android.app.util.MinuteIntervals.SECOND_QUARTER
import uk.nhs.nhsx.covid19.android.app.util.MinuteIntervals.THIRD_QUARTER
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.LONG
import java.time.format.FormatStyle.MEDIUM
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

fun Instant.minutesUntilNow(clock: Clock): Int =
    until(
        Instant.now(clock),
        ChronoUnit.MINUTES
    ).toInt()

val lastDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH")

fun LocalDateTime.keysQueryFormat(): String {
    return format(lastDateFormatter)
}

private val uiTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.uiFormat(context: Context): String {
    return if (context.getResourcesLocale().language == SupportedLanguage.CHINESE.code) {
        DateTimeFormatter.ofLocalizedDate(MEDIUM).withLocale(context.getResourcesLocale())
            .format(this)
    } else {
        DateTimeFormatter.ofPattern("d MMM yyyy", context.getResourcesLocale())
            .format(this)
    }
}

fun LocalDate.uiLongFormat(context: Context): String {
    return if (context.getResourcesLocale().language == SupportedLanguage.CHINESE.code) {
        DateTimeFormatter.ofLocalizedDate(LONG).withLocale(context.getResourcesLocale())
            .format(this)
    } else {
        DateTimeFormatter.ofPattern("d MMMM yyyy", context.getResourcesLocale())
            .format(this)
    }
}

fun LocalTime.uiFormat(): String = uiTimeFormatter.format(this)

fun LocalDateTime.uiFormat(context: Context): String {
    return if (context.getResourcesLocale().language == SupportedLanguage.CHINESE.code) {
        val date =
            DateTimeFormatter.ofLocalizedDate(MEDIUM).withLocale(context.getResourcesLocale())
                .format(this)
        val time = uiTimeFormatter.withLocale(context.getResourcesLocale()).format(this)
        "$date $time"
    } else {
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", context.getResourcesLocale())
            .format(this)
    }
}

fun Instant.isBeforeOrEqual(instant: Instant) =
    !this.isAfter(instant)

fun Instant.isEqualOrAfter(instant: Instant) =
    !this.isBefore(instant)

fun LocalDate.isBeforeOrEqual(date: LocalDate) =
    !this.isAfter(date)

fun LocalDate.isEqualOrAfter(date: LocalDate) =
    !this.isBefore(date)

fun LocalDateTime.isEqualOrAfter(dateTime: LocalDateTime) =
    !this.isBefore(dateTime)

private enum class MinuteIntervals(val range: IntRange) {
    FIRST_QUARTER(0..14),
    SECOND_QUARTER(15..29),
    THIRD_QUARTER(30..44)
}

fun Instant.roundDownToNearestQuarter(): Instant {
    val now = this.atZone(ZoneOffset.UTC)

    val roundedTime = when (now.minute) {
        in FIRST_QUARTER.range -> now.withMinute(0)
        in SECOND_QUARTER.range -> now.withMinute(15)
        in THIRD_QUARTER.range -> now.withMinute(30)
        else -> now.withMinute(45)
    }

    return roundedTime.truncatedTo(ChronoUnit.MINUTES).toInstant()
}

fun Instant.roundUpToNearestQuarter(): Instant {
    val now = this.atZone(ZoneOffset.UTC)

    val roundedTime = when (now.minute) {
        in FIRST_QUARTER.range -> now.withMinute(15)
        in SECOND_QUARTER.range -> now.withMinute(30)
        in THIRD_QUARTER.range -> now.withMinute(45)
        else -> now.plusHours(1).withMinute(0)
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

fun Instant.toLocalDate(zoneId: ZoneId): LocalDate =
    this.atZone(zoneId).toLocalDate()

fun Instant.toISOSecondsFormat(): String =
    DateTimeFormatter.ISO_INSTANT.format(this.truncatedTo(ChronoUnit.SECONDS))

fun Context.getResourcesLocale(): Locale =
    try {
        Locale(resources.getString(R.string.locale))
    } catch (e: NullPointerException) {
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
    }

fun selectEarliest(
    localDate1: LocalDate?,
    localDate2: LocalDate
): LocalDate {
    return if (localDate1?.isBefore(localDate2) == true) {
        localDate1
    } else {
        localDate2
    }
}

fun selectNewest(
    localDateTime1: LocalDateTime?,
    localDateTime2: LocalDateTime
): LocalDateTime {
    return if (localDateTime1?.isAfter(localDateTime2) == true) {
        localDateTime1
    } else {
        localDateTime2
    }
}

fun selectNewest(
    localDate1: LocalDate?,
    localDate2: LocalDate
): LocalDate {
    return if (localDate1?.isAfter(localDate2) == true) {
        localDate1
    } else {
        localDate2
    }
}

package uk.nhs.nhsx.covid19.android.app.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

private val uiDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

private val uiTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val uiDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

fun LocalDate.uiFormat(): String = uiDateFormatter.format(this)

fun LocalTime.uiFormat(): String = uiTimeFormatter.format(this)

fun LocalDateTime.uiFormat(): String = uiDateTimeFormatter.format(this)

fun LocalDate.toReadableFormat(): String =
    DateTimeFormatter.ofPattern("dd MMM yyyy").format(this)

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

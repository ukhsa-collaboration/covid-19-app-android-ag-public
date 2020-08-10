package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Daily
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Hourly
import uk.nhs.nhsx.covid19.android.app.util.daysUntilToday
import uk.nhs.nhsx.covid19.android.app.util.hoursUntilNow
import uk.nhs.nhsx.covid19.android.app.util.lastDateFormatter
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class DownloadKeysParams(
    private val lastDownloadedKeyTimeProvider: LastDownloadedKeyTimeProvider,
    private val clock: Clock
) {

    @Inject
    constructor(lastDownloadedKeyTimeProvider: LastDownloadedKeyTimeProvider) : this(
        lastDownloadedKeyTimeProvider,
        Clock.systemUTC()
    )

    private lateinit var latestDownloadTime: LocalDateTime

    fun getNextQueries(): List<Intervals> = mutableListOf<Intervals>().apply {

        latestDownloadTime = lastDownloadedKeyTimeProvider.getLatestStoredTime()

        addAll(getHourlyIntervalsPriorDaily())

        addAll(dailyIntervals())

        addAll(hourIntervalsLeftUntilNow())
    }

    private fun getHourlyIntervalsPriorDaily(): List<Hourly> =
        with(latestDownloadTime) {
            if (isBeforeToday(clock) && hoursUntilEndOfDay() != 24) {
                handleHourlyIntervals(hoursUntilEndOfDay() / 2)
            } else listOf()
        }

    private fun dailyIntervals(): List<Daily> =
        with(latestDownloadTime) {
            if (isMoreThanTwoDaysAgo(clock)) {
                handleDailyIntervals(daysUntilToday(clock))
            } else listOf()
        }

    private fun hourIntervalsLeftUntilNow(): List<Hourly> =
        handleHourlyIntervals(latestDownloadTime.hoursUntilNow(clock) / 2)

    private fun handleHourlyIntervals(
        intervals: Int
    ): List<Hourly> = mutableListOf<Hourly>().apply {
        repeat(intervals) {
            latestDownloadTime = latestDownloadTime.plusHours(TWO_HOURS)
            add(Hourly(latestDownloadTime.toDomainString()))
        }
    }.toList()

    private fun handleDailyIntervals(
        intervals: Int
    ): List<Daily> = mutableListOf<Daily>().apply {
        repeat(intervals) {
            latestDownloadTime = latestDownloadTime.plusDays(ONE_DAY).withHour(0)
            add(Daily(latestDownloadTime.toDomainString()))
        }
    }.toList()

    private fun LocalDateTime.hoursUntilEndOfDay(): Int =
        until(
            withHour(0).plusDays(1),
            ChronoUnit.HOURS
        ).toInt()

    private fun LocalDateTime.isMoreThanTwoDaysAgo(clock: Clock): Boolean =
        isBefore(LocalDateTime.now(clock).minusDays(1).withHour(1))

    private fun LocalDateTime.isBeforeToday(clock: Clock): Boolean =
        isBefore(LocalDateTime.now(clock).withHour(0))

    private fun LocalDateTime.toDomainString(): String {
        return format(lastDateFormatter)
    }

    companion object {
        const val TWO_HOURS = 2L
        const val ONE_DAY = 1L
    }

    sealed class Intervals {

        abstract val timestamp: String

        data class Daily(override val timestamp: String) : Intervals()

        data class Hourly(override val timestamp: String) : Intervals()
    }
}

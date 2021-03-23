package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetAnalyticsWindow @Inject constructor(private val clock: Clock) {

    operator fun invoke(instant: Instant = Instant.now(clock)): Pair<Instant, Instant> =
        getWindowStart(instant) to getWindowEnd(instant)

    private fun getWindowStart(instant: Instant): Instant {
        return instant.truncatedTo(ChronoUnit.DAYS)
    }

    private fun getWindowEnd(instant: Instant): Instant {
        return instant.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
    }
}

fun Pair<Instant, Instant>.toAnalyticsWindow() = AnalyticsWindow(
    startDate = first.toISOSecondsFormat(),
    endDate = second.toISOSecondsFormat()
)

fun AnalyticsWindow.startDateToInstant(): Instant = Instant.parse(startDate)

fun AnalyticsWindow.endDateToInstant(): Instant = Instant.parse(endDate)

package uk.nhs.nhsx.covid19.android.app.analytics

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetAnalyticsWindow @Inject constructor(private val clock: Clock) {

    operator fun invoke(instant: Instant = Instant.now(clock)): Pair<Instant, Instant> =
        getWindowStart(instant) to getWindowEnd(instant)

    fun getLastWindow(): Pair<Instant, Instant> =
        invoke(Instant.now(clock).minus(1, ChronoUnit.DAYS))

    private fun getWindowStart(instant: Instant): Instant {
        return instant.truncatedTo(ChronoUnit.DAYS)
    }

    private fun getWindowEnd(instant: Instant): Instant {
        return instant.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
    }
}

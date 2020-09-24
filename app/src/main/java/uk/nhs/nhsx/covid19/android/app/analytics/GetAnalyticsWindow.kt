package uk.nhs.nhsx.covid19.android.app.analytics

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetAnalyticsWindow(private val clock: Clock) {

    @Inject
    constructor() : this(Clock.systemUTC())

    operator fun invoke(): Pair<Instant, Instant> =
        getLastCompletedWindowStart() to getLastCompletedWindowEnd()

    fun getCurrentWindowEnd(): Instant {
        return Instant.now(clock).truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
    }

    private fun getLastCompletedWindowStart(): Instant {
        return Instant.now(clock).truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS)
    }

    private fun getLastCompletedWindowEnd(): Instant {
        return Instant.now(clock).truncatedTo(ChronoUnit.DAYS)
    }
}

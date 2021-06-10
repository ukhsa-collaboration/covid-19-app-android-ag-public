package uk.nhs.nhsx.covid19.android.app.state

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class CalculateExpirationNotificationTime @Inject constructor(
    private val clock: Clock
) {

    operator fun invoke(
        expiryDate: LocalDate
    ): Instant =
        expiryDate
            .atStartOfDay()
            .atZone(clock.zone)
            .minusHours(3)
            .toInstant()
}

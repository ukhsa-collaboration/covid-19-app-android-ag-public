package uk.nhs.nhsx.covid19.android.app.state

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CalculateExpirationNotificationTime @Inject constructor() {

    operator fun invoke(
        expiryDate: LocalDate,
        zoneId: ZoneId
    ): Instant =
        expiryDate
            .atStartOfDay()
            .atZone(zoneId)
            .minusHours(3)
            .toInstant()
}

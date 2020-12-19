package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class FilterOutdatedVisits @Inject constructor(private val clock: Clock) : (List<VenueVisit>) -> List<VenueVisit> {

    override fun invoke(p1: List<VenueVisit>): List<VenueVisit> =
        p1.filter {
            it.to.isAfter(Instant.now(clock).minus(21, ChronoUnit.DAYS))
        }
}

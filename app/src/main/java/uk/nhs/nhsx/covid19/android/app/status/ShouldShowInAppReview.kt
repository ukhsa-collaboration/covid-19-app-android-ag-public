package uk.nhs.nhsx.covid19.android.app.status

import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ShouldShowInAppReview @Inject constructor(
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val lastAppRatingStartedDateProvider: LastAppRatingStartedDateProvider,
    private val clock: Clock
) {

    suspend operator fun invoke(): Boolean {
        val visits = visitedVenuesStorage.getVisits()

        if (visits.isNullOrEmpty()) {
            return false
        }

        val earliestVisit =
            visits.minByOrNull { it.from.atZone(clock.zone).truncatedTo(ChronoUnit.DAYS) }
        val latestVisit =
            visits.maxByOrNull { it.from.atZone(clock.zone).truncatedTo(ChronoUnit.DAYS) }

        val daysBetween = ChronoUnit.DAYS.between(earliestVisit!!.from, latestVisit!!.from)

        return daysBetween > 0 && lastAppRatingStartedDateProvider.value == null
    }
}

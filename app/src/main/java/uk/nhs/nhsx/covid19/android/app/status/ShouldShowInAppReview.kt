package uk.nhs.nhsx.covid19.android.app.status

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ShouldShowInAppReview @Inject constructor(
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val lastAppRatingStartedDateProvider: LastAppRatingStartedDateProvider
) {

    suspend operator fun invoke(): Boolean {
        val visits = visitedVenuesStorage.getVisits()

        if (visits.isNullOrEmpty()) {
            return false
        }

        val earliestVisit =
            visits.minBy { it.from.atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS) }
        val latestVisit =
            visits.maxBy { it.from.atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS) }

        val daysBetween = ChronoUnit.DAYS.between(earliestVisit!!.from, latestVisit!!.from)

        return daysBetween > 0 &&
            RuntimeBehavior.isFeatureEnabled(FeatureFlag.IN_APP_REVIEW) &&
            lastAppRatingStartedDateProvider.value == null
    }
}

package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import javax.inject.Inject

class DownloadAndProcessRiskyVenues @Inject constructor(
    private val riskyVenuesApi: RiskyVenuesApi,
    private val venueMatchFinder: VenueMatchFinder,
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val filterOutdatedVisits: FilterOutdatedVisits,
    private val riskyVenuesCircuitBreakerTasks: RiskyVenuesCircuitBreakerTasks
) {

    suspend operator fun invoke(): Result<Unit> {
        if (!RuntimeBehavior.isFeatureEnabled(FeatureFlag.HIGH_RISK_VENUES)) {
            return Result.Success(Unit)
        }

        return withContext(Dispatchers.IO) {
            runSafely {
                clearOutDatedVisits()

                val riskyVenues = riskyVenuesApi.getListOfRiskyVenues().venues

                if (riskyVenues.isEmpty()) return@runSafely

                val notNotifiedVisitedRiskyVenuesIds = venueMatchFinder.findMatches(riskyVenues)

                if (notNotifiedVisitedRiskyVenuesIds.isEmpty()) return@runSafely

                visitedVenuesStorage.markAsWasInRiskyList(notNotifiedVisitedRiskyVenuesIds)

                riskyVenuesCircuitBreakerTasks.scheduleRiskyVenuesCircuitBreakerInitial(
                    notNotifiedVisitedRiskyVenuesIds
                )
            }
        }
    }

    private suspend fun clearOutDatedVisits() {
        val visits = visitedVenuesStorage.getVisits()
        val updatedVisits = filterOutdatedVisits.invoke(visits)

        visitedVenuesStorage.setVisits(updatedVisits)
    }
}

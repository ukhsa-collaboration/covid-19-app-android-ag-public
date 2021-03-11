package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import javax.inject.Inject

class DownloadAndProcessRiskyVenues @Inject constructor(
    private val riskyVenuesApi: RiskyVenuesApi,
    private val venueMatchFinder: VenueMatchFinder,
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val filterOutdatedVisits: FilterOutdatedVisits,
    private val riskyVenuesCircuitBreakerPolling: RiskyVenuesCircuitBreakerPolling,
    private val riskyVenueCircuitBreakerConfigurationProvider: RiskyVenueCircuitBreakerConfigurationProvider
) {

    suspend operator fun invoke(clearOutdatedVisits: Boolean = true): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runSafely {
                if (clearOutdatedVisits) clearOutDatedVisits()

                val riskyVenues = riskyVenuesApi.getListOfRiskyVenues().venues

                if (riskyVenues.isEmpty()) return@runSafely

                val unnotifiedVenueVisits: Map<RiskyVenue, List<VenueVisit>> = venueMatchFinder.findMatches(riskyVenues)

                if (unnotifiedVenueVisits.isEmpty()) return@runSafely

                visitedVenuesStorage.markAsWasInRiskyList(unnotifiedVenueVisits.values.flatten())

                val configurations = unnotifiedVenueVisits.flatMap { (riskyVenue, venueVisits) ->
                    venueVisits.map { venueVisit ->
                        // venueVisit.to is an instant calculated as nowForCheckIn.getNextLocalMidnightTime(clock)
                        // which is a start of the next day of check in, startedAt has to be at the day of check in so
                        // it is necessary to subtract 1 millisecond
                        RiskyVenueCircuitBreakerConfiguration(
                            startedAt = venueVisit.to.minusMillis(1),
                            venueId = riskyVenue.id,
                            approvalToken = null,
                            isPolling = false,
                            messageType = riskyVenue.messageType
                        )
                    }
                }
                riskyVenueCircuitBreakerConfigurationProvider.addAll(configurations)

                riskyVenuesCircuitBreakerPolling()
            }
        }
    }

    private suspend fun clearOutDatedVisits() {
        val visits = visitedVenuesStorage.getVisits()
        val updatedVisits = filterOutdatedVisits.invoke(visits)

        visitedVenuesStorage.setVisits(updatedVisits)
    }
}

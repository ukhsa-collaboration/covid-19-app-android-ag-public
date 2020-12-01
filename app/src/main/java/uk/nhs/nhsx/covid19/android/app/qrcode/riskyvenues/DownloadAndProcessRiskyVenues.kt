package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class DownloadAndProcessRiskyVenues(
    private val riskyVenuesApi: RiskyVenuesApi,
    private val venueMatchFinder: VenueMatchFinder,
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val filterOutdatedVisits: FilterOutdatedVisits,
    private val riskyVenuesCircuitBreakerPolling: RiskyVenuesCircuitBreakerPolling,
    private val riskyVenueCircuitBreakerConfigurationProvider: RiskyVenueCircuitBreakerConfigurationProvider,
    private val clock: Clock
) {

    @Inject
    constructor(
        riskyVenuesApi: RiskyVenuesApi,
        venueMatchFinder: VenueMatchFinder,
        visitedVenuesStorage: VisitedVenuesStorage,
        filterOutdatedVisits: FilterOutdatedVisits,
        riskyVenuesCircuitBreakerPolling: RiskyVenuesCircuitBreakerPolling,
        riskyVenueCircuitBreakerConfigurationProvider: RiskyVenueCircuitBreakerConfigurationProvider
    ) : this(
        riskyVenuesApi,
        venueMatchFinder,
        visitedVenuesStorage,
        filterOutdatedVisits,
        riskyVenuesCircuitBreakerPolling,
        riskyVenueCircuitBreakerConfigurationProvider,
        Clock.systemUTC()
    )

    suspend operator fun invoke(clearOutdatedVisits: Boolean = true): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runSafely {
                if (clearOutdatedVisits) clearOutDatedVisits()

                val riskyVenues = riskyVenuesApi.getListOfRiskyVenues().venues

                if (riskyVenues.isEmpty()) return@runSafely

                val notNotifiedVisitedRiskyVenuesIds = venueMatchFinder.findMatches(riskyVenues)

                if (notNotifiedVisitedRiskyVenuesIds.isEmpty()) return@runSafely

                visitedVenuesStorage.markAsWasInRiskyList(notNotifiedVisitedRiskyVenuesIds)

                val configurations = notNotifiedVisitedRiskyVenuesIds.map { venueId ->
                    RiskyVenueCircuitBreakerConfiguration(
                        startedAt = Instant.now(clock),
                        venueId = venueId,
                        approvalToken = null,
                        isPolling = false
                    )
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

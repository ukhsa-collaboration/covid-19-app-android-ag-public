package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import java.time.Instant
import java.time.temporal.ChronoUnit

class MockRiskyVenuesApi : RiskyVenuesApi {
    override suspend fun getListOfRiskyVenues() = RiskyVenuesResponse(
        venues = listOf(
            RiskyVenue(
                id = "ABCD1234",
                riskyWindow = RiskyWindow(
                    from = Instant.now().minus(15, ChronoUnit.DAYS),
                    to = Instant.now().plus(15, ChronoUnit.DAYS)
                )
            )
        )
    )
}

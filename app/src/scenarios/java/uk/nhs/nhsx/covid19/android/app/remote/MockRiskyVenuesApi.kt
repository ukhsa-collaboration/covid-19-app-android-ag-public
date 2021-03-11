package uk.nhs.nhsx.covid19.android.app.remote

import java.time.Instant
import java.time.temporal.ChronoUnit
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow

class MockRiskyVenuesApi : RiskyVenuesApi {

    var riskyVenuesResponse = RiskyVenuesResponse(
        venues = listOf(
            RiskyVenue(
                id = "ABCD1234",
                riskyWindow = RiskyWindow(
                    from = Instant.now().minus(15, ChronoUnit.DAYS),
                    to = Instant.now().plus(15, ChronoUnit.DAYS)
                ),
                messageType = INFORM
            )
        )
    )

    override suspend fun getListOfRiskyVenues(): RiskyVenuesResponse =
        MockApiModule.behaviour.invoke { riskyVenuesResponse }
}

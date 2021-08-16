package uk.nhs.nhsx.covid19.android.app.notifications

import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider.Companion.RISKY_VENUE
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON

class RiskyVenueAlertProviderTest : ProviderTest<RiskyVenueAlertProvider, RiskyVenueAlert?>() {
    override val getTestSubject = ::RiskyVenueAlertProvider
    override val property = RiskyVenueAlertProvider::riskyVenueAlert
    override val key = RISKY_VENUE
    override val defaultValue: RiskyVenueAlert? = null
    override val expectations: List<ProviderTestExpectation<RiskyVenueAlert?>> = listOf(
        ProviderTestExpectation(json = riskyVenueAlertJson, objectValue = riskyVenueAlert),
        ProviderTestExpectation(json = null, objectValue = null, direction = OBJECT_TO_JSON)
    )

    companion object {
        val riskyVenueAlert = RiskyVenueAlert(
            id = "12345",
            messageType = INFORM
        )

        const val riskyVenueAlertJson =
            """{"id":"12345","messageType":"M1"}"""
    }
}

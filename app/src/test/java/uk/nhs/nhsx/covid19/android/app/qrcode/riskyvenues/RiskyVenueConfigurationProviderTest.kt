package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider.Companion.RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation

class RiskyVenueConfigurationProviderTest : ProviderTest<RiskyVenueConfigurationProvider, RiskyVenueConfigurationDurationDays>() {

    override val getTestSubject = ::RiskyVenueConfigurationProvider
    override val property = RiskyVenueConfigurationProvider::durationDays
    override val key = RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY
    override val defaultValue = RiskyVenueConfigurationDurationDays()
    override val expectations: List<ProviderTestExpectation<RiskyVenueConfigurationDurationDays>> = listOf(
        ProviderTestExpectation(json = durationDaysJson, objectValue = durationDays)
    )

    companion object {
        private val durationDays = RiskyVenueConfigurationDurationDays(
            optionToBookATest = 5
        )

        private const val durationDaysJson =
            """{"optionToBookATest":5}"""
    }
}

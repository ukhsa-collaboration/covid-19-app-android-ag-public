package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationResponse

class MockRiskyVenueConfigurationApi : RiskyVenueConfigurationApi {
    override suspend fun getRiskyVenueConfiguration(): RiskyVenueConfigurationResponse =
        MockApiModule.behaviour.invoke {
            RiskyVenueConfigurationResponse(
                durationDays = RiskyVenueConfigurationDurationDays(optionToBookATest = DEFAULT_OPTION_BOOK_A_TEST)
            )
        }

    companion object {
        const val DEFAULT_OPTION_BOOK_A_TEST = 11
    }
}

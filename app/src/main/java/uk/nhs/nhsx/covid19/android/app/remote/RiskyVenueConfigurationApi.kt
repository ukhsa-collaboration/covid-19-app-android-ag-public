package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationResponse

interface RiskyVenueConfigurationApi {

    @GET("distribution/risky-venue-configuration")
    suspend fun getRiskyVenueConfiguration(): RiskyVenueConfigurationResponse
}

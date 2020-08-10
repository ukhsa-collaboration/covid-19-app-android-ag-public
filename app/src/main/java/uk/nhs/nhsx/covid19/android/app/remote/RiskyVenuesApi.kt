package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse

interface RiskyVenuesApi {
    @GET("distribution/risky-venues")
    suspend fun getListOfRiskyVenues(): RiskyVenuesResponse
}

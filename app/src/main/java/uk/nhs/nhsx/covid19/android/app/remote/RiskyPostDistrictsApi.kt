package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.PostDistrictsResponse

interface RiskyPostDistrictsApi {
    @GET("distribution/risky-post-districts")
    suspend fun fetchRiskyPostDistricts(): PostDistrictsResponse
}

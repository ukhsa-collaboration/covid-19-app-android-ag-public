package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyPostCodeDistributionResponse

interface RiskyPostDistrictsApi {
    @GET("distribution/risky-post-districts-v2")
    suspend fun fetchRiskyPostCodeDistribution(): RiskyPostCodeDistributionResponse
}

package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsResponse

interface LocalStatsApi {
    @GET("distribution/v1/local-covid-stats-daily")
    suspend fun fetchLocalStats(): LocalStatsResponse
}

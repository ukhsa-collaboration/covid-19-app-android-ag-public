package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface KeysDistributionApi {
    @Streaming
    @GET("distribution/daily/{timestamp}")
    suspend fun fetchDailyKeys(@Path("timestamp") timestamp: String): ResponseBody

    @Streaming
    @GET("distribution/two-hourly/{timestamp}")
    suspend fun fetchHourlyKeys(@Path("timestamp") timestamp: String): ResponseBody
}

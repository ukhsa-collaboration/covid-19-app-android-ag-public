package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse

interface AppAvailabilityApi {

    @GET("distribution/availability-android")
    suspend fun getAvailability(): AppAvailabilityResponse
}

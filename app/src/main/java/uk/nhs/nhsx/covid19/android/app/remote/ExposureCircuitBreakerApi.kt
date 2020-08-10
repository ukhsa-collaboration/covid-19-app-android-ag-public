package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerResponse

interface ExposureCircuitBreakerApi {
    @POST("circuit-breaker/exposure-notification/request")
    suspend fun submitExposureInfo(@Body exposureCircuitBreakerRequest: ExposureCircuitBreakerRequest): ExposureCircuitBreakerResponse

    @GET("circuit-breaker/exposure-notification/resolution/{approvalToken}")
    suspend fun getExposureCircuitBreakerResolution(@Path("approvalToken") approvalToken: String): ExposureCircuitBreakerPollingResponse
}

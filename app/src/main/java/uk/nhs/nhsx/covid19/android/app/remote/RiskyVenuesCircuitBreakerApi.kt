package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerResponse

interface RiskyVenuesCircuitBreakerApi {
    @POST("circuit-breaker/venue/request")
    suspend fun submitInitialVenueIdForApproval(
        @Body riskyVenueCircuitBreakerRequest: RiskyVenuesCircuitBreakerRequest
    ): RiskyVenuesCircuitBreakerResponse

    @GET("circuit-breaker/venue/resolution/{approvalToken}")
    suspend fun getRiskyVenuesBreakerResolution(
        @Path("approvalToken") approvalToken: String
    ): RiskyVenuesCircuitBreakerPollingResponse
}

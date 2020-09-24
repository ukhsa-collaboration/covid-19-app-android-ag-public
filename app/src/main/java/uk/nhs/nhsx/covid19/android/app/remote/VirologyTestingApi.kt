package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestOrderResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse

interface VirologyTestingApi {
    @POST("virology-test/home-kit/order")
    suspend fun getHomeKitOrder(@Body emptyBodyObject: Any = Object()): VirologyTestOrderResponse

    @POST("virology-test/results")
    suspend fun getTestResult(
        @Body virologyTestResultRequestBody: VirologyTestResultRequestBody
    ): Response<VirologyTestResultResponse>

    @POST("virology-test/cta-exchange")
    suspend fun getTestResultForCtaToken(
        @Body virologyCtaExchangeRequest: VirologyCtaExchangeRequest
    ): Response<VirologyCtaExchangeResponse>
}

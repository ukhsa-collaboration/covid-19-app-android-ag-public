package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlResponse

interface IsolationPaymentApi {
    @POST("isolation-payment/ipc-token/create")
    suspend fun createToken(@Body isolationPaymentCreateTokenRequest: IsolationPaymentCreateTokenRequest): IsolationPaymentCreateTokenResponse

    @POST("isolation-payment/ipc-token/update")
    suspend fun requestUrl(@Body isolationPaymentUrlRequest: IsolationPaymentUrlRequest): IsolationPaymentUrlResponse
}

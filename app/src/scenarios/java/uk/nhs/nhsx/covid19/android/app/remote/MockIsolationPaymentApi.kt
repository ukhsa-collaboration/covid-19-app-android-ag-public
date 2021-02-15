package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlResponse

class MockIsolationPaymentApi : IsolationPaymentApi {
    override suspend fun createToken(isolationPaymentCreateTokenRequest: IsolationPaymentCreateTokenRequest): IsolationPaymentCreateTokenResponse =
        MockApiModule.behaviour.invoke {
            IsolationPaymentCreateTokenResponse(
                isEnabled = true,
                ipcToken = "ipcToken"
            )
        }

    override suspend fun requestUrl(isolationPaymentUrlRequest: IsolationPaymentUrlRequest): IsolationPaymentUrlResponse =
        MockApiModule.behaviour.invoke {
            IsolationPaymentUrlResponse("about:blank")
        }
}

package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlResponse
import java.io.IOException

class MockIsolationPaymentApi : IsolationPaymentApi {

    var shouldPass: Boolean = true

    override suspend fun createToken(isolationPaymentCreateTokenRequest: IsolationPaymentCreateTokenRequest): IsolationPaymentCreateTokenResponse {
        if (!shouldPass) throw IOException()
        return IsolationPaymentCreateTokenResponse(
            isEnabled = true,
            ipcToken = "ipcToken"
        )
    }

    override suspend fun requestUrl(isolationPaymentUrlRequest: IsolationPaymentUrlRequest): IsolationPaymentUrlResponse {
        if (!shouldPass) throw IOException()
        return IsolationPaymentUrlResponse("about:blank")
    }
}

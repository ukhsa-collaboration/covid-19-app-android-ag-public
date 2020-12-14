package uk.nhs.nhsx.covid19.android.app.payment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.IsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlResponse
import javax.inject.Inject

class RequestIsolationPaymentUrl @Inject constructor(private val isolationPaymentApi: IsolationPaymentApi) {

    suspend operator fun invoke(
        isolationPaymentUrlRequest: IsolationPaymentUrlRequest
    ): Result<IsolationPaymentUrlResponse> =
        withContext(Dispatchers.IO) {
            runSafely {
                isolationPaymentApi.requestUrl(isolationPaymentUrlRequest)
            }
        }
}

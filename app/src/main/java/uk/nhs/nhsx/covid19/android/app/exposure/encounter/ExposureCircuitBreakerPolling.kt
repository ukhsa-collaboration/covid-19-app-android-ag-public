package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Instant
import javax.inject.Inject

class ExposureCircuitBreakerPolling @Inject constructor(
    private val exposureCircuitBreakerApi: ExposureCircuitBreakerApi,
    private val stateMachine: IsolationStateMachine
) {

    suspend fun doWork(approvalToken: String, exposureDate: Long): Result = withContext(Dispatchers.IO) {
        runCatching {
            val response =
                exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = approvalToken)

            when (CircuitBreakerResult.valueOf(response.approval.toUpperCase())) {
                YES -> handleYes(exposureDate)
                NO -> Result.success()
                PENDING -> Result.retry()
            }
        }.getOrElse {
            Result.retry()
        }
    }

    private fun handleYes(exposureDate: Long): Result {
        stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(exposureDate)))
        return Result.success()
    }
}

package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.work.ListenableWorker.Result
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerRequest
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class HandleExposureNotification(
    private val exposureNotificationApi: ExposureNotificationApi,
    private val stateMachine: IsolationStateMachine,
    private val periodicTasks: PeriodicTasks,
    private val exposureCircuitBreakerApi: ExposureCircuitBreakerApi,
    private val configurationApi: ExposureConfigurationApi,
    private val riskCalculator: RiskCalculator = RiskCalculator()
) {

    @Inject
    constructor(
        exposureNotificationApi: ExposureNotificationApi,
        stateMachine: IsolationStateMachine,
        periodicTasks: PeriodicTasks,
        exposureCircuitBreakerApi: ExposureCircuitBreakerApi,
        configurationApi: ExposureConfigurationApi
    ) : this(
        exposureNotificationApi,
        stateMachine,
        periodicTasks,
        exposureCircuitBreakerApi,
        configurationApi,
        RiskCalculator()
    )

    suspend fun doWork(token: String): Result = withContext(Dispatchers.IO) {
        runCatching {

            val exposureInformationList =
                exposureNotificationApi.getExposureInformation(token)
            val exposureConfiguration = configurationApi.getExposureConfiguration()

            val calculatedRisk =
                riskCalculator(exposureInformationList, exposureConfiguration.riskCalculation)

            val threshold = exposureConfiguration.riskCalculation.riskThreshold
            Timber.d("calculatedRisk: $calculatedRisk threshold: $threshold")
            if (calculatedRisk != null) {
                val summary = exposureNotificationApi.getExposureSummary(token)
                val response = exposureCircuitBreakerApi
                    .submitExposureInfo(
                        ExposureCircuitBreakerRequest(
                            maximumRiskScore = calculatedRisk.second,
                            daysSinceLastExposure = Instant.ofEpochMilli(calculatedRisk.first).until(Instant.now(), ChronoUnit.DAYS).toInt(),
                            matchedKeyCount = 1
                        )
                    )

                when (CircuitBreakerResult.valueOf(response.approval.toUpperCase())) {
                    YES -> handleYes(calculatedRisk.first)
                    NO -> success()
                    PENDING -> startPolling(response.approvalToken, calculatedRisk.first)
                }
            }
            success()
        }.getOrElse {
            Timber.e(it)
            retry()
        }
    }

    private fun startPolling(approvalToken: String, exposureDate: Long) {
        periodicTasks.scheduleExposureCircuitBreakerPolling(approvalToken, exposureDate)
    }

    private fun handleYes(exposureDate: Long) {
        stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(exposureDate)))
    }
}

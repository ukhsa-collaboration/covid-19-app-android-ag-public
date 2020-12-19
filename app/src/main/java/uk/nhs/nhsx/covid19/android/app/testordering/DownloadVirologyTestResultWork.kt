package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.annotation.VisibleForTesting
import androidx.work.ListenableWorker
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class DownloadVirologyTestResultWork @Inject constructor(
    private val virologyTestingApi: VirologyTestingApi,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val stateMachine: IsolationStateMachine,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock
) {

    suspend operator fun invoke(): ListenableWorker.Result {
        val configs = testOrderingTokensProvider.configs

        configs.forEach { config ->
            try {
                // FIXME: move into separate class
                if (removeIfOld(config)) {
                    return@forEach
                }

                val pollingToken = config.testResultPollingToken
                val testResultResponse = fetchVirologyTestResult(pollingToken) ?: return@forEach
                val receivedTestResult = ReceivedTestResult(
                    config.diagnosisKeySubmissionToken,
                    testResultResponse.testEndDate,
                    testResultResponse.testResult
                )
                logAnalytics(receivedTestResult.testResult)
                stateMachine.processEvent(OnTestResult(receivedTestResult))
                testOrderingTokensProvider.remove(config)
            } catch (exception: Exception) {
                Timber.e(exception)
            }
        }

        return ListenableWorker.Result.success()
    }

    private suspend fun logAnalytics(result: VirologyTestResult) {
        when (result) {
            POSITIVE -> analyticsEventProcessor.track(PositiveResultReceived)
            NEGATIVE -> analyticsEventProcessor.track(NegativeResultReceived)
            VOID -> analyticsEventProcessor.track(VoidResultReceived)
        }
        analyticsEventProcessor.track(ResultReceived(result, INSIDE_APP))
    }

    private suspend fun fetchVirologyTestResult(pollingToken: String): VirologyTestResultResponse? {
        val testResultResponse =
            virologyTestingApi.getTestResult(VirologyTestResultRequestBody(pollingToken))

        if (testResultResponse.code() != 200) {
            Timber.e("Test result polling returned error status code ${testResultResponse.code()}")
        }

        return testResultResponse.body()
    }

    @VisibleForTesting
    fun removeIfOld(config: TestOrderPollingConfig): Boolean {
        val houseKeepingDeletionPeriod =
            isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val configExpiryInstant = config.startedAt.plus(houseKeepingDeletionPeriod.toLong(), DAYS)
        val now = Instant.now(clock)
        if (now.isAfter(configExpiryInstant)) {
            testOrderingTokensProvider.remove(config)
            return true
        }
        return false
    }
}

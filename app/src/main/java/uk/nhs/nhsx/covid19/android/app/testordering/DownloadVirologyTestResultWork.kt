package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.annotation.VisibleForTesting
import androidx.work.ListenableWorker
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork.ReceivedVirologyTestResult.NoTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork.ReceivedVirologyTestResult.ParsableTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork.ReceivedVirologyTestResult.UnparsableTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class DownloadVirologyTestResultWork @Inject constructor(
    private val virologyTestingApi: VirologyTestingApi,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val stateMachine: IsolationStateMachine,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val receivedUnknownTestResultProvider: ReceivedUnknownTestResultProvider,
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

                when (val receivedTestResultResponse = fetchVirologyTestResult(pollingToken)) {
                    is ParsableTestResult -> {
                        val testResultResponse = receivedTestResultResponse.virologyTestResultResponse
                        val receivedTestResult = ReceivedTestResult(
                            config.diagnosisKeySubmissionToken,
                            testResultResponse.testEndDate,
                            testResultResponse.testResult,
                            testResultResponse.testKit,
                            testResultResponse.diagnosisKeySubmissionSupported,
                            testResultResponse.requiresConfirmatoryTest,
                            confirmatoryDayLimit = testResultResponse.confirmatoryDayLimit
                        )
                        logAnalytics(testResultResponse.testResult, testResultResponse.testKit)
                        stateMachine.processEvent(OnTestResult(receivedTestResult))
                        testOrderingTokensProvider.remove(config)
                    }
                    UnparsableTestResult -> {
                        receivedUnknownTestResultProvider.value = true
                        testOrderingTokensProvider.remove(config)
                    }
                    NoTestResult -> return@forEach
                }
            } catch (exception: Exception) {
                Timber.e(exception)
            }
        }

        return ListenableWorker.Result.success()
    }

    private suspend fun logAnalytics(
        result: VirologyTestResult,
        testKitType: VirologyTestKitType
    ) {
        when (result) {
            POSITIVE -> analyticsEventProcessor.track(PositiveResultReceived)
            NEGATIVE -> analyticsEventProcessor.track(NegativeResultReceived)
            VOID -> analyticsEventProcessor.track(VoidResultReceived)
        }
        analyticsEventProcessor.track(ResultReceived(result, testKitType, INSIDE_APP))
    }

    private suspend fun fetchVirologyTestResult(pollingToken: String): ReceivedVirologyTestResult =
        localAuthorityPostCodeProvider.getPostCodeDistrict()?.supportedCountry?.let { country ->
            try {
                val testResultResponse =
                    virologyTestingApi.getTestResult(VirologyTestResultRequestBody(pollingToken, country))

                if (!testResultResponse.isSuccessful) {
                    Timber.e("Test result polling returned error status code ${testResultResponse.code()}")
                }
                if (testResultResponse.code() == 204) {
                    NoTestResult
                }

                val virologyTestResultResponse = testResultResponse.body() ?: return NoTestResult
                ParsableTestResult(virologyTestResultResponse)
            } catch (exception: JsonDataException) {
                UnparsableTestResult
            } catch (jsonEncodingException: JsonEncodingException) {
                UnparsableTestResult
            }
        } ?: NoTestResult

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

    sealed class ReceivedVirologyTestResult {
        data class ParsableTestResult(
            val virologyTestResultResponse: VirologyTestResultResponse
        ) : ReceivedVirologyTestResult()

        object UnparsableTestResult : ReceivedVirologyTestResult()
        object NoTestResult : ReceivedVirologyTestResult()
    }
}

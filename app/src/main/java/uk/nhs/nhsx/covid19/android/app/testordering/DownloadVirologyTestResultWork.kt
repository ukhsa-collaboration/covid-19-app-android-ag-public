package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.work.ListenableWorker
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork.ReceivedVirologyTestResult.NoTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork.ReceivedVirologyTestResult.ParsableTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork.ReceivedVirologyTestResult.UnparsableTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import javax.inject.Inject

class DownloadVirologyTestResultWork @Inject constructor(
    private val virologyTestingApi: VirologyTestingApi,
    private val testOrderingTokensProvider: TestOrderingTokensProvider,
    private val stateMachine: IsolationStateMachine,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val receivedUnknownTestResultProvider: ReceivedUnknownTestResultProvider
) {

    suspend operator fun invoke(): ListenableWorker.Result {
        val configs = testOrderingTokensProvider.configs

        configs.forEach { config ->
            try {
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
                            testResultResponse.shouldOfferFollowUpTest,
                            confirmatoryDayLimit = testResultResponse.confirmatoryDayLimit
                        )

                        stateMachine.processEvent(OnTestResult(receivedTestResult, testOrderType = INSIDE_APP))
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

    sealed class ReceivedVirologyTestResult {
        data class ParsableTestResult(
            val virologyTestResultResponse: VirologyTestResultResponse
        ) : ReceivedVirologyTestResult()

        object UnparsableTestResult : ReceivedVirologyTestResult()
        object NoTestResult : ReceivedVirologyTestResult()
    }
}

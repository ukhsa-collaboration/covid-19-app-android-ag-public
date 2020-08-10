package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestOrderResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import java.io.IOException
import java.time.Instant

class MockVirologyTestingApi : VirologyTestingApi {

    var shouldPass: Boolean = true
    var pollingTestResultHttpStatusCode = 200
    var pollingToken = "1234"
    var testResultForPollingToken = mutableMapOf(pollingToken to POSITIVE)

    override suspend fun getHomeKitOrder(emptyBodyObject: Any): VirologyTestOrderResponse {
        if (!shouldPass) throw IOException()

        return VirologyTestOrderResponse(
            websiteUrlWithQuery = "https://a.b/c&d=e",
            tokenParameterValue = "e",
            testResultPollingToken = pollingToken,
            diagnosisKeySubmissionToken = "g"
        )
    }

    override suspend fun getTestResult(
        virologyTestResultRequestBody: VirologyTestResultRequestBody
    ): Response<VirologyTestResultResponse> {
        if (!shouldPass) throw IOException()

        val testResult = testResultForPollingToken[virologyTestResultRequestBody.testResultPollingToken] ?: throw IOException("No test result for token")

        val virologyTestResultResponse = if (pollingTestResultHttpStatusCode == 200) {
            VirologyTestResultResponse(
                testEndDate = Instant.now(),
                testResult = testResult
            )
        } else {
            null
        }

        return if (pollingTestResultHttpStatusCode == 200 || pollingTestResultHttpStatusCode == 204) {
            Response.success(pollingTestResultHttpStatusCode, virologyTestResultResponse)
        } else {
            Response.error(
                pollingTestResultHttpStatusCode,
                "".toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull())
            )
        }
    }

    fun setDefaultTestResult(testResult: VirologyTestResult) {
        testResultForPollingToken = mutableMapOf(pollingToken to testResult)
    }
}

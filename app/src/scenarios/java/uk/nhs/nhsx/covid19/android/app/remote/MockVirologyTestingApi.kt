package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestOrderResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MockVirologyTestingApi : VirologyTestingApi {

    var shouldPass: Boolean = true
    var pollingTestResultHttpStatusCode = 200
    var pollingToken = "1234"
    var testResultForPollingToken = mutableMapOf(pollingToken to POSITIVE)
    var diagnosisKeySubmissionToken: String? = null

    override suspend fun getHomeKitOrder(emptyBodyObject: Any): VirologyTestOrderResponse {
        if (!shouldPass) throw IOException()

        val token = diagnosisKeySubmissionToken ?: UUID.randomUUID().toString()

        return VirologyTestOrderResponse(
            websiteUrlWithQuery = "about:blank",
            tokenParameterValue = "e",
            testResultPollingToken = pollingToken,
            diagnosisKeySubmissionToken = token
        )
    }

    override suspend fun getTestResult(
        virologyTestResultRequestBody: VirologyTestResultRequestBody
    ): Response<VirologyTestResultResponse> {
        if (!shouldPass) throw IOException()

        val virologyTestResultResponse = if (pollingTestResultHttpStatusCode == 200) {
            val testResult =
                testResultForPollingToken[virologyTestResultRequestBody.testResultPollingToken]
                    ?: throw IOException("No test result for token")

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

    override suspend fun getTestResultForCtaToken(
        virologyCtaExchangeRequest: VirologyCtaExchangeRequest
    ): Response<VirologyCtaExchangeResponse> {
        return when (virologyCtaExchangeRequest.ctaToken) {
            "pstvpstv" -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE
                    )
                )
            }
            "f3dzcfdt" -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = NEGATIVE
                    )
                )
            }
            "8vb7xehg" -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = VOID
                    )
                )
            }
            "n0c0nneb" -> {
                throw IOException("No connection")
            }
            "nexpectn" -> {
                throw Exception("Unexpected error")
            }
            else -> {
                Response.error(
                    404,
                    "".toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull())
                )
            }
        }
    }

    fun setDefaultTestResult(testResult: VirologyTestResult) {
        testResultForPollingToken = mutableMapOf(pollingToken to testResult)
    }
}

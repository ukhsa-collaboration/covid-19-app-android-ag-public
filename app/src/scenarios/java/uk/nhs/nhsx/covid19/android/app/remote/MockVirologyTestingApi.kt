package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
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
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED

class MockVirologyTestingApi : VirologyTestingApi {

    var shouldPass: Boolean = true
    var pollingTestResultHttpStatusCode = 200
    var pollingToken = "1234"
    var testResponseForPollingToken = mutableMapOf(pollingToken to TestResponse(POSITIVE, LAB_RESULT))
    var diagnosisKeySubmissionToken: String? = null
    var testEndDate: Instant? = null

    fun reset() {
        shouldPass = true
        pollingTestResultHttpStatusCode = 200
        pollingToken = "1234"
        testResponseForPollingToken = mutableMapOf(pollingToken to TestResponse(POSITIVE, LAB_RESULT))
        diagnosisKeySubmissionToken = "g"
        testEndDate = null
    }

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
            val testResponse =
                testResponseForPollingToken[virologyTestResultRequestBody.testResultPollingToken]
                    ?: throw IOException("No test result for token")

            VirologyTestResultResponse(
                testEndDate = testEndDate ?: Instant.now(),
                testResult = testResponse.testResult,
                testKit = testResponse.testKitType,
                diagnosisKeySubmissionSupported = testResponse.diagnosisKeySubmissionSupported
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
            POSITIVE_PCR_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }
            POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = false
                    )
                )
            }
            POSITIVE_LFD_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }
            POSITIVE_RAPID_SELF_REPORTED_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = RAPID_SELF_REPORTED,
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }
            NEGATIVE_PCR_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = NEGATIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }
            NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = NEGATIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = false
                    )
                )
            }
            NEGATIVE_LFD_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = NEGATIVE,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }
            VOID_PCR_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = VOID,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }
            VOID_PCR_TOKEN_NO_KEY_SUBMISSION -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = VOID,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = false
                    )
                )
            }
            VOID_LFD_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = "diagnosis_submission_token",
                        testEndDate = testEndDate ?: Instant.now().minus(2, ChronoUnit.DAYS),
                        testResult = VOID,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }
            NO_CONNECTION_TOKEN -> {
                throw IOException("No connection")
            }
            UNEXPECTED_ERROR_TOKEN -> {
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

    fun setDefaultTestResponse(testResult: VirologyTestResult, testKitType: VirologyTestKitType = LAB_RESULT) {
        testResponseForPollingToken = mutableMapOf(pollingToken to TestResponse(testResult, testKitType))
    }

    companion object {
        const val POSITIVE_PCR_TOKEN = "pstvpstv"
        const val POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION = "tbdfjaj0"
        const val POSITIVE_LFD_TOKEN = "fdpstvp6"
        const val POSITIVE_RAPID_SELF_REPORTED_TOKEN = "xzmgc0vz"
        const val NEGATIVE_PCR_TOKEN = "f3dzcfdt"
        const val NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION = "7p40rzgq"
        const val NEGATIVE_LFD_TOKEN = "fdngtngw"
        const val VOID_PCR_TOKEN = "8vb7xehg"
        const val VOID_PCR_TOKEN_NO_KEY_SUBMISSION = "cp3xxadb"
        const val VOID_LFD_TOKEN = "fdvdvdvx"
        const val NO_CONNECTION_TOKEN = "n0c0nneb"
        const val UNEXPECTED_ERROR_TOKEN = "nexpectn"
    }
}

data class TestResponse(
    val testResult: VirologyTestResult,
    val testKitType: VirologyTestKitType,
    val diagnosisKeySubmissionSupported: Boolean = true
)

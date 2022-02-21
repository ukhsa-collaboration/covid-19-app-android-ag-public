package uk.nhs.nhsx.covid19.android.app.remote

import com.squareup.moshi.JsonDataException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestOrderResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultRequestBody
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResultResponse
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

class MockVirologyTestingApi @Inject constructor(val clock: Clock) : VirologyTestingApi {
    var pollingTestResultHttpStatusCode = 200
    var pollingToken = "1234"
    var testResponseForPollingToken = mutableMapOf(pollingToken to TestResponse(POSITIVE, LAB_RESULT))
    var diagnosisKeySubmissionToken: String? = null
    var testEndDate: Instant? = null
    var testResultForCtaTokenStatusCode = 200
    var responseMock =
        VirologyCtaExchangeResponse(
            diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
            testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
            testResult = NEGATIVE,
            testKit = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false,
            confirmatoryDayLimit = null,
            shouldOfferFollowUpTest = false
        )

    init {
        Timber.d("Creating instance")
    }

    fun reset() {
        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED
        pollingTestResultHttpStatusCode = 200
        pollingToken = "1234"
        testResponseForPollingToken = mutableMapOf(pollingToken to TestResponse(POSITIVE, LAB_RESULT))
        diagnosisKeySubmissionToken = "g"
        testEndDate = null
    }

    override suspend fun getHomeKitOrder(emptyBodyObject: Any): VirologyTestOrderResponse =
        MockApiModule.behaviour.invoke {
            val token = diagnosisKeySubmissionToken ?: UUID.randomUUID().toString()

            VirologyTestOrderResponse(
                websiteUrlWithQuery = "about:blank",
                tokenParameterValue = "e",
                testResultPollingToken = pollingToken,
                diagnosisKeySubmissionToken = token
            )
        }

    override suspend fun getTestResult(
        virologyTestResultRequestBody: VirologyTestResultRequestBody
    ): Response<VirologyTestResultResponse> = MockApiModule.behaviour.invoke {
        val virologyTestResultResponse = if (pollingTestResultHttpStatusCode == 200) {
            val testResponse =
                testResponseForPollingToken[virologyTestResultRequestBody.testResultPollingToken]
                    ?: throw IOException("No test result for token")

            VirologyTestResultResponse(
                testEndDate = testEndDate ?: Instant.now(clock),
                testResult = testResponse.testResult,
                testKit = testResponse.testKitType,
                diagnosisKeySubmissionSupported = testResponse.diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest = testResponse.requiresConfirmatoryTest,
                shouldOfferFollowUpTest = testResponse.shouldOfferFollowUpTest,
                confirmatoryDayLimit = testResponse.confirmatoryDayLimit
            )
        } else {
            null
        }

        if (pollingTestResultHttpStatusCode == 200 || pollingTestResultHttpStatusCode == 204) {
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
    ): Response<VirologyCtaExchangeResponse> = MockApiModule.behaviour.invoke {
        if (testResultForCtaTokenStatusCode != 200) {
            return@invoke Response.error(
                testResultForCtaTokenStatusCode,
                "".toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull())
            )
        }
        when (virologyCtaExchangeRequest.ctaToken) {
            POSITIVE_PCR_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = false,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            POSITIVE_LFD_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            POSITIVE_LFD_TOKEN_INDICATIVE -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = true,
                        shouldOfferFollowUpTest = true,
                        confirmatoryDayLimit = 2
                    )
                )
            }
            POSITIVE_LFD_TOKEN_INDICATIVE_NO_KEY_SUBMISSION -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = false,
                        requiresConfirmatoryTest = true,
                        shouldOfferFollowUpTest = true,
                        confirmatoryDayLimit = null
                    )
                )
            }
            POSITIVE_RAPID_SELF_REPORTED_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = RAPID_SELF_REPORTED,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            POSITIVE_RAPID_SELF_REPORTED_TOKEN_INDICATIVE -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = POSITIVE,
                        testKit = RAPID_SELF_REPORTED,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = true,
                        shouldOfferFollowUpTest = true,
                        confirmatoryDayLimit = 1
                    )
                )
            }
            NEGATIVE_PCR_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = NEGATIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = NEGATIVE,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = false,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            NEGATIVE_LFD_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = NEGATIVE,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            VOID_PCR_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = VOID,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            VOID_PCR_TOKEN_NO_KEY_SUBMISSION -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = VOID,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = false,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            VOID_LFD_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = VOID,
                        testKit = RAPID_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            PLOD_PCR_TOKEN -> {
                Response.success(
                    VirologyCtaExchangeResponse(
                        diagnosisKeySubmissionToken = DIAGNOSIS_KEY_SUBMISSION_TOKEN,
                        testEndDate = testEndDate ?: Instant.now(clock).minus(2, ChronoUnit.DAYS),
                        testResult = PLOD,
                        testKit = LAB_RESULT,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false,
                        shouldOfferFollowUpTest = false,
                        confirmatoryDayLimit = null
                    )
                )
            }
            MANUAL_CONFIGURATION_TOKEN -> {
                Timber.d(responseMock.toString())
                Response.success(responseMock)
            }
            NO_CONNECTION_TOKEN -> {
                throw IOException("No connection")
            }
            UNKNOWN_RESULT_TOKEN -> {
                throw JsonDataException("Could not parse body.")
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

    fun setDefaultTestResponse(
        testResult: VirologyTestResult,
        testKitType: VirologyTestKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported: Boolean = true,
        requiresConfirmatoryTest: Boolean = false
    ) {
        testResponseForPollingToken = mutableMapOf(
            pollingToken to TestResponse(
                testResult,
                testKitType,
                diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest
            )
        )
    }

    fun mockResponse(
        diagnosisKeySubmissionToken: String?,
        virologyTestResultValue: VirologyTestResult,
        virologyTestKitType: VirologyTestKitType,
        diagnosisKeySubmissionSupported: Boolean,
        requiresConfirmatoryTest: Boolean,
        confirmatoryDayLimit: Int?,
        shouldOfferFollowUpTest: Boolean,
        testEndDate: Instant
    ) {
        responseMock =
            VirologyCtaExchangeResponse(
                diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
                testEndDate = testEndDate,
                testResult = virologyTestResultValue,
                testKit = virologyTestKitType,
                diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest = requiresConfirmatoryTest,
                confirmatoryDayLimit = confirmatoryDayLimit,
                shouldOfferFollowUpTest = shouldOfferFollowUpTest
            )
    }

    companion object {
        const val POSITIVE_PCR_TOKEN = "pstvpstv"
        const val POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION = "tbdfjaj0"
        const val POSITIVE_LFD_TOKEN = "fdpstvp6"
        const val POSITIVE_LFD_TOKEN_INDICATIVE = "fdpstc0s"
        const val POSITIVE_LFD_TOKEN_INDICATIVE_NO_KEY_SUBMISSION = "fdpstn0j"
        const val POSITIVE_RAPID_SELF_REPORTED_TOKEN = "xzmgc0vz"
        const val POSITIVE_RAPID_SELF_REPORTED_TOKEN_INDICATIVE = "w2vrvqhe"
        const val NEGATIVE_PCR_TOKEN = "f3dzcfdt"
        const val NEGATIVE_PCR_TOKEN_NO_KEY_SUBMISSION = "7p40rzgq"
        const val NEGATIVE_LFD_TOKEN = "fdngtngw"
        const val VOID_PCR_TOKEN = "8vb7xehg"
        const val VOID_PCR_TOKEN_NO_KEY_SUBMISSION = "cp3xxadb"
        const val VOID_LFD_TOKEN = "fdvdvdvx"
        const val PLOD_PCR_TOKEN = "8rqdg14d"
        const val UNKNOWN_RESULT_TOKEN = "ebz6dxht"
        const val NO_CONNECTION_TOKEN = "n0c0nneb"
        const val UNEXPECTED_ERROR_TOKEN = "nexpectn"
        const val MANUAL_CONFIGURATION_TOKEN = "testendd"

        const val DIAGNOSIS_KEY_SUBMISSION_TOKEN = "diagnosis_submission_token"
    }
}

data class TestResponse(
    val testResult: VirologyTestResult,
    val testKitType: VirologyTestKitType,
    val diagnosisKeySubmissionSupported: Boolean = true,
    val requiresConfirmatoryTest: Boolean = false,
    val shouldOfferFollowUpTest: Boolean = false,
    val confirmatoryDayLimit: Int? = null
)

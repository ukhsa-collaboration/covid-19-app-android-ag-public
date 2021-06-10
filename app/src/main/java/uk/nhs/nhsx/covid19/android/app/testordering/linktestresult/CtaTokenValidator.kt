package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.UnparsableTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType.UNEXPECTED
import java.io.IOException
import javax.inject.Inject

class CtaTokenValidator @Inject constructor(
    private val virologyTestingApi: VirologyTestingApi,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val crockfordDammValidator: CrockfordDammValidator
) {

    suspend fun validate(ctaToken: String): CtaTokenValidationResult = withContext(Dispatchers.IO) {
        try {
            if (ctaToken.length != CTA_TOKEN_LENGTH || !crockfordDammValidator.validate(ctaToken)) {
                return@withContext Failure(INVALID)
            }

            val country = localAuthorityPostCodeProvider.getPostCodeDistrict()?.supportedCountry
            if (country != null) {
                val result = virologyTestingApi.getTestResultForCtaToken(VirologyCtaExchangeRequest(ctaToken, country))
                when {
                    result.isSuccessful -> processSuccessfulResponse(result.body()!!)
                    result.code() == 400 || result.code() == 404 -> Failure(INVALID)
                    else -> Failure(UNEXPECTED)
                }
            } else {
                Timber.e("Could not resolve supported country. It should not have been possible to get to this point without a supported country")
                Failure(UNEXPECTED)
            }
        } catch (jsonException: JsonDataException) {
            UnparsableTestResult
        } catch (jsonEncodingException: JsonEncodingException) {
            UnparsableTestResult
        } catch (ioException: IOException) {
            Failure(NO_CONNECTION)
        } catch (exception: Exception) {
            Failure(UNEXPECTED)
        }
    }

    private fun processSuccessfulResponse(response: VirologyCtaExchangeResponse): CtaTokenValidationResult =
        if (response.testResult != POSITIVE && (response.testKit == RAPID_RESULT || response.testKit == RAPID_SELF_REPORTED || response.requiresConfirmatoryTest))
            Failure(UNEXPECTED)
        else if (!response.requiresConfirmatoryTest && response.confirmatoryDayLimit != null)
            Failure(UNEXPECTED)
        else Success(response)

    companion object {
        const val CTA_TOKEN_LENGTH = 8
    }

    sealed class CtaTokenValidationResult {
        data class Success(
            val virologyCtaExchangeResponse: VirologyCtaExchangeResponse
        ) : CtaTokenValidationResult()

        object UnparsableTestResult : CtaTokenValidationResult()
        data class Failure(val type: ValidationErrorType) : CtaTokenValidationResult()
    }

    enum class ValidationErrorType {
        INVALID, NO_CONNECTION, UNEXPECTED
    }
}

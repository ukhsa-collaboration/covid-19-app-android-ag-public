package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostalDistrictProviderWrapper
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.UNEXPECTED
import java.io.IOException
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED

class CtaTokenValidator @Inject constructor(
    private val virologyTestingApi: VirologyTestingApi,
    private val postalDistrictProviderWrapper: PostalDistrictProviderWrapper,
    private val crockfordDammValidator: CrockfordDammValidator
) {

    suspend fun validate(ctaToken: String): CtaTokenValidationResult = withContext(Dispatchers.IO) {
        try {
            if (ctaToken.length != CTA_TOKEN_LENGTH || !crockfordDammValidator.validate(ctaToken)) {
                return@withContext Failure(INVALID)
            }

            val country = postalDistrictProviderWrapper.getPostCodeDistrict()?.supportedCountry
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
        } catch (ioException: IOException) {
            Failure(NO_CONNECTION)
        } catch (exception: Exception) {
            Failure(UNEXPECTED)
        }
    }

    private fun processSuccessfulResponse(response: VirologyCtaExchangeResponse): CtaTokenValidationResult =
        if (response.testResult != POSITIVE &&
            (response.testKit == RAPID_RESULT || response.testKit == RAPID_SELF_REPORTED)
        ) Failure(UNEXPECTED)
        else Success(response)

    companion object {
        const val CTA_TOKEN_LENGTH = 8
    }

    sealed class CtaTokenValidationResult {
        data class Success(val virologyCtaExchangeResponse: VirologyCtaExchangeResponse) :
            CtaTokenValidationResult()

        data class Failure(val type: LinkTestResultErrorType) : CtaTokenValidationResult()
    }
}

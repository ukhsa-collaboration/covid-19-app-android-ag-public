package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.UNEXPECTED
import java.io.IOException
import javax.inject.Inject

class CtaTokenValidator @Inject constructor(
    private val virologyTestingApi: VirologyTestingApi
) {

    suspend fun validate(ctaToken: String): CtaTokenValidationResult = withContext(Dispatchers.IO) {
        try {
            if (ctaToken.length != CTA_TOKEN_LENGTH) {
                return@withContext Failure(INVALID)
            }

            val result =
                virologyTestingApi.getTestResultForCtaToken(VirologyCtaExchangeRequest(ctaToken))

            when {
                result.isSuccessful -> Success(result.body()!!)
                result.code() == 400 || result.code() == 404 -> Failure(INVALID)
                else -> Failure(UNEXPECTED)
            }
        } catch (ioException: IOException) {
            Failure(NO_CONNECTION)
        } catch (exception: Exception) {
            Failure(UNEXPECTED)
        }
    }

    companion object {
        const val CTA_TOKEN_LENGTH = 8
    }

    sealed class CtaTokenValidationResult {
        data class Success(val virologyCtaExchangeResponse: VirologyCtaExchangeResponse) :
            CtaTokenValidationResult()

        data class Failure(val type: LinkTestResultErrorType) : CtaTokenValidationResult()
    }
}

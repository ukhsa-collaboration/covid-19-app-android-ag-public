package uk.nhs.nhsx.covid19.android.app.onboarding.authentication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.remote.ActivationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ActivationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationCodeValidator @Inject constructor(
    private val activationApi: ActivationApi
) {
    suspend fun validate(authCode: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (authCode.length != AUTH_CODE_LENGTH) {
                return@runCatching false
            }

            val result = activationApi.activate(ActivationRequest(authCode))
            result.isSuccessful
        }.fold(
            onFailure = { false },
            onSuccess = { it }
        )
    }

    companion object {
        const val AUTH_CODE_LENGTH = 8
    }
}

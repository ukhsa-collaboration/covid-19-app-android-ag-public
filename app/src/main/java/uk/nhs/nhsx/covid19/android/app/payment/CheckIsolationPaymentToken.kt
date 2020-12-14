package uk.nhs.nhsx.covid19.android.app.payment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostalDistrictProviderWrapper
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.remote.IsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenRequest
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CheckIsolationPaymentToken @Inject constructor(
    private val canClaimIsolationPayment: CanClaimIsolationPayment,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val isolationPaymentApi: IsolationPaymentApi,
    private val postalDistrictProviderWrapper: PostalDistrictProviderWrapper
) {

    private val mutex = Mutex()

    suspend operator fun invoke() = runSafely {
        mutex.withLock {
            if (canClaimIsolationPayment()) {
                ensureTokenResolved()
            } else {
                clearToken()
            }
        }
    }

    private suspend fun ensureTokenResolved() {
        if (isolationPaymentTokenStateProvider.tokenState == Unresolved) {
            createToken()
        }
    }

    private suspend fun createToken() {
        withContext(Dispatchers.IO) {
            getIsolationPaymentCountry()?.let { country ->
                val response = isolationPaymentApi.createToken(IsolationPaymentCreateTokenRequest(country))
                isolationPaymentTokenStateProvider.tokenState =
                    if (response.isEnabled) {
                        if (response.ipcToken != null) {
                            Token(response.ipcToken)
                        } else {
                            Timber.e("Unexpected null token in response with isEnabled=true: $response")
                            Unresolved
                        }
                    } else {
                        Disabled
                    }
            }
        }
    }

    private suspend fun getIsolationPaymentCountry(): IsolationPaymentCountry? {
        return postalDistrictProviderWrapper.getPostCodeDistrict()?.let { postCodeDistrict ->
            when (postCodeDistrict) {
                ENGLAND -> IsolationPaymentCountry.ENGLAND
                WALES -> IsolationPaymentCountry.WALES
                else -> null
            }
        }
    }

    private fun clearToken() {
        isolationPaymentTokenStateProvider.tokenState = Unresolved
    }
}

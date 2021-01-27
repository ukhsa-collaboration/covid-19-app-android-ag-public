package uk.nhs.nhsx.covid19.android.app.payment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedActiveIpcToken
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostalDistrictProviderWrapper
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.remote.IsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentCreateTokenRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckIsolationPaymentToken @Inject constructor(
    private val canClaimIsolationPayment: CanClaimIsolationPayment,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val isolationPaymentApi: IsolationPaymentApi,
    private val postalDistrictProviderWrapper: PostalDistrictProviderWrapper,
    private val analyticsEventProcessor: AnalyticsEventProcessor
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
            getSupportedCountry()?.let { country ->
                val response = isolationPaymentApi.createToken(IsolationPaymentCreateTokenRequest(country))
                isolationPaymentTokenStateProvider.tokenState =
                    if (response.isEnabled) {
                        if (response.ipcToken != null) {
                            analyticsEventProcessor.track(ReceivedActiveIpcToken)
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

    private suspend fun getSupportedCountry(): SupportedCountry? =
        postalDistrictProviderWrapper.getPostCodeDistrict()?.supportedCountry

    private fun clearToken() {
        isolationPaymentTokenStateProvider.tokenState = Unresolved
    }
}

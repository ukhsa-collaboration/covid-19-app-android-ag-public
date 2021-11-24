package uk.nhs.nhsx.covid19.android.app.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationPaymentUrlRequest
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.lang.IllegalStateException
import java.time.Clock
import java.time.ZoneOffset
import javax.inject.Inject

class RedirectToIsolationPaymentWebsiteViewModel @Inject constructor(
    private val requestIsolationPaymentUrl: RequestIsolationPaymentUrl,
    private val isolationPaymentTokenProvider: IsolationPaymentTokenStateProvider,
    private val isolationStateMachine: IsolationStateMachine,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock
) : ViewModel() {

    private val fetchWebsiteUrlLiveData = MutableLiveData<Lce<String>>()
    fun fetchWebsiteUrl(): LiveData<Lce<String>> = fetchWebsiteUrlLiveData

    fun loadIsolationPaymentUrl() {
        viewModelScope.launch {
            fetchWebsiteUrlLiveData.postValue(Lce.Loading)

            val tokenState = isolationPaymentTokenProvider.tokenState
            val currentIsolation = isolationStateMachine.readLogicalState() as? PossiblyIsolating
            val contactCase = currentIsolation?.getActiveContactCase(clock)

            if (tokenState !is IsolationPaymentTokenState.Token || contactCase == null) {
                fetchWebsiteUrlLiveData.postValue(Lce.Error(IllegalStateException("Can't request isolation payment when not in contact isolation or payment token is not valid")))
                return@launch
            }

            val riskyEncounterDate = contactCase.exposureDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            val isolationPeriodEndDate = contactCase.expiryDate.atStartOfDay(ZoneOffset.UTC).toInstant()

            when (
                val result =
                    requestIsolationPaymentUrl.invoke(
                        IsolationPaymentUrlRequest(
                            ipcToken = tokenState.token,
                            riskyEncounterDate = riskyEncounterDate,
                            isolationPeriodEndDate = isolationPeriodEndDate
                        )
                    )
            ) {
                is Success -> {
                    analyticsEventProcessor.track(LaunchedIsolationPaymentsApplication)
                    fetchWebsiteUrlLiveData.postValue(Lce.Success(result.value.websiteUrlWithQuery))
                }
                is Failure -> fetchWebsiteUrlLiveData.postValue(Lce.Error(result.throwable))
            }
        }
    }

    sealed class ViewState {
        object Loading : ViewState()
        data class Success(val url: String) : ViewState()
        object Error : ViewState()
    }
}

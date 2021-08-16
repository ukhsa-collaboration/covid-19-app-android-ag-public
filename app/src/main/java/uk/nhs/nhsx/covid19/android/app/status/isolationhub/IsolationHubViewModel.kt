package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.payment.CanClaimIsolationPayment
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget.BookTest
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubViewModel.NavigationTarget.IsolationPayment
import uk.nhs.nhsx.covid19.android.app.status.testinghub.CanBookPcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.EvaluateBookTestNavigation
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class IsolationHubViewModel @Inject constructor(
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    canBookPcrTest: CanBookPcrTest,
    private val canClaimIsolationPayment: CanClaimIsolationPayment,
    private val evaluateBookTestNavigation: EvaluateBookTestNavigation,
    private val notificationProvider: NotificationProvider,
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    init {
        viewModelScope.launch {
            viewStateLiveData.postValue(
                ViewState(
                    showIsolationPaymentButton = mustShowIsolationPaymentButton(),
                    showBookTestButton = canBookPcrTest()
                )
            )
        }
    }

    fun onCreate() {
        notificationProvider.cancelIsolationHubReminderNotification()
    }

    fun onItemIsolationPaymentClicked() {
        analyticsEventProcessor.track(SelectedIsolationPaymentsButton)
        navigationTargetLiveData.postValue(IsolationPayment)
    }

    fun onItemBookTestClicked() {
        navigationTargetLiveData.postValue(BookTest(evaluateBookTestNavigation()))
    }

    private fun mustShowIsolationPaymentButton(): Boolean =
        canClaimIsolationPayment() && isolationPaymentTokenStateProvider.tokenState is Token

    data class ViewState(
        val showIsolationPaymentButton: Boolean,
        val showBookTestButton: Boolean
    )

    sealed class NavigationTarget {
        object IsolationPayment : NavigationTarget()
        data class BookTest(val navigationTarget: EvaluateBookTestNavigation.NavigationTarget) : NavigationTarget()
    }
}

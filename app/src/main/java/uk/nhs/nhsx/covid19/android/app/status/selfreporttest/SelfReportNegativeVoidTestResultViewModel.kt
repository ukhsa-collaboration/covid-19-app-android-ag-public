package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelfReportedNegativeSelfLFDTestResultEnteredManually
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelfReportedVoidSelfLFDTestResultEnteredManually
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportNegativeVoidTestResultViewModel.NavigationTarget.Status
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class SelfReportNegativeVoidTestResultViewModel @AssistedInject constructor(
    @Assisted private val isNegative: Boolean,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    fun fetchCountry() {
        runBlocking {
            viewStateLiveData.postValue(ViewState(localAuthorityPostCodeProvider.requirePostCodeDistrict()))
        }
    }

    fun onClickBackToHome() {
        if (isNegative) {
            analyticsEventProcessor.track(SelfReportedNegativeSelfLFDTestResultEnteredManually)
        } else {
            analyticsEventProcessor.track(SelfReportedVoidSelfLFDTestResultEnteredManually)
        }

        navigateLiveData.postValue(Status)
    }

    sealed class NavigationTarget {
        object Status : NavigationTarget()
    }

    data class ViewState(val country: PostCodeDistrict?)

    @AssistedFactory
    interface Factory {
        fun create(
            isNegative: Boolean,
        ): SelfReportNegativeVoidTestResultViewModel
    }
}

package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAppWillNotNotifyOtherUsersViewModel.NavigationTarget.TestKit
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAppWillNotNotifyOtherUsersViewModel.NavigationTarget.ShareKeysInfo
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class SelfReportAppWillNotNotifyOtherUsersViewModel @AssistedInject constructor(
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    fun onClickContinue() {
        navigateLiveData.postValue(TestKit(questions))
    }

    fun onBackPressed() {
        navigateLiveData.postValue(ShareKeysInfo(questions))
    }

    sealed class NavigationTarget {
        data class TestKit(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class ShareKeysInfo(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): SelfReportAppWillNotNotifyOtherUsersViewModel
    }
}

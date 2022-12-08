package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.NavigationTarget.Advice
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class SelfReportThankYouViewModel @AssistedInject constructor(
    @Assisted private val sharingSuccessful: Boolean,
    @Assisted private val hasReported: Boolean
) : ViewModel() {

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    enum class FlowCase {
        SuccessfullySharedKeysAndNoNeedToReportTest,
        SuccessfullySharedKeysAndNHSTestNotReported,
        UnsuccessfullySharedKeysAndNoNeedToReportTest,
        UnsuccessfullySharedKeysAndNHSTestNotReported
    }

    init {
        setScreenFlow()
    }

    fun onClickContinue() {
        navigateLiveData.postValue(Advice(hasReported))
    }

    private fun setScreenFlow() {
        if (sharingSuccessful) {
            hasTestBeenReported(hasReportedCase = SuccessfullySharedKeysAndNoNeedToReportTest, hasNotReportedCase = SuccessfullySharedKeysAndNHSTestNotReported)
        } else {
            hasTestBeenReported(hasReportedCase = UnsuccessfullySharedKeysAndNoNeedToReportTest, hasNotReportedCase = UnsuccessfullySharedKeysAndNHSTestNotReported)
        }
    }

    private fun hasTestBeenReported(hasReportedCase: FlowCase, hasNotReportedCase: FlowCase) {
        if (hasReported)
            viewStateLiveData.postValue(ViewState(hasReportedCase))
        else
            viewStateLiveData.postValue(ViewState(hasNotReportedCase))
    }

    sealed class NavigationTarget {
        data class Advice(val hasReported: Boolean) : NavigationTarget()
    }

    data class ViewState(val flowCase: FlowCase)

    @AssistedFactory
    interface Factory {
        fun create(
            sharingSuccessful: Boolean,
            hasReported: Boolean
        ): SelfReportThankYouViewModel
    }
}

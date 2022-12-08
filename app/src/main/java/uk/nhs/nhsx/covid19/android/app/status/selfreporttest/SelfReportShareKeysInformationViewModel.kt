package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.FetchKeysFlow
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.DeclinedKeySharing
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.SharedKeys
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.TestType
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class SelfReportShareKeysInformationViewModel @AssistedInject constructor(
    fetchKeysFlowFactory: FetchKeysFlow.Factory,
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel(), FetchKeysFlow.Callback {

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private val permissionRequestLiveData = SingleLiveEvent<(Activity) -> Unit>()
    fun permissionRequest(): LiveData<(Activity) -> Unit> = permissionRequestLiveData

    private val fetchKeysFlow by lazy {
        fetchKeysFlowFactory.create(
            this,
            viewModelScope,
            null
        )
    }

    fun onClickContinue() {
        fetchKeysFlow()
    }

    override fun onFetchKeysSuccess(
        temporaryExposureKeys: List<NHSTemporaryExposureKey>,
        diagnosisKeySubmissionToken: String?
    ) {
        navigateLiveData.postValue(SharedKeys(questions.copy(temporaryExposureKeys = temporaryExposureKeys)))
    }

    override fun onFetchKeysPermissionDenied() {
        navigateLiveData.postValue(DeclinedKeySharing(questions))
    }

    override fun onFetchKeysUnexpectedError() {
        navigateLiveData.postValue(DeclinedKeySharing(questions))
    }

    override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
        permissionRequestLiveData.postValue(permissionRequest)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        fetchKeysFlow.onActivityResult(requestCode, resultCode)
    }

    fun onBackPressed() {
        navigateLiveData.postValue(TestType(questions))
    }

    sealed class NavigationTarget {
        data class DeclinedKeySharing(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class SharedKeys(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class TestType(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): SelfReportShareKeysInformationViewModel
    }
}

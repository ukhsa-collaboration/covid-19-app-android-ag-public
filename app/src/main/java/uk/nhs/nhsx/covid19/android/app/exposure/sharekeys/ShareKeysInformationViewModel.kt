package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.StatusActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import javax.inject.Inject

class ShareKeysInformationViewModel @Inject constructor(
    private val submitEpidemiologyDataForTestResult: SubmitEpidemiologyDataForTestResult,
    fetchKeysFlowFactory: FetchKeysFlow.Factory,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val clock: Clock
) : ViewModel() {

    private val navigationLiveData = SingleLiveEvent<ShareKeysNavigationTarget>()
    fun navigation(): LiveData<ShareKeysNavigationTarget> = navigationLiveData

    private val permissionRequestLiveData = SingleLiveEvent<(Activity) -> Unit>()
    fun permissionRequest(): LiveData<(Activity) -> Unit> = permissionRequestLiveData

    private lateinit var keySharingInfo: KeySharingInfo

    private val fetchKeysFlow by lazy {
        fetchKeysFlowFactory.create(
            object : FetchKeysFlow.Callback {
                override fun onFetchKeysSuccess(
                    temporaryExposureKeys: List<NHSTemporaryExposureKey>,
                    diagnosisKeySubmissionToken: String
                ) {
                    navigationLiveData.postValue(
                        SubmitKeysProgressActivity(temporaryExposureKeys, diagnosisKeySubmissionToken)
                    )
                }

                override fun onFetchKeysPermissionDenied() {
                    val keysSharingInfo = keySharingInfoProvider.keySharingInfo
                    if (keysSharingInfo != null && keysSharingInfo.wasAcknowledgedMoreThan24HoursAgo(clock)) {
                        keySharingInfoProvider.reset()
                    } else {
                        keySharingInfoProvider.setHasDeclinedSharingKeys()
                    }
                    navigationLiveData.postValue(StatusActivity)
                }

                override fun onFetchKeysUnexpectedError() {
                    navigationLiveData.postValue(Finish)
                }

                override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
                    permissionRequestLiveData.postValue(permissionRequest)
                }
            },
            viewModelScope,
            keySharingInfo
        )
    }

    fun onCreate() {
        keySharingInfoProvider.keySharingInfo?.let {
            keySharingInfo = it
        } ?: navigationLiveData.postValue(Finish)
    }

    fun onShareKeysButtonClicked() {
        fetchKeysFlow()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        fetchKeysFlow.onActivityResult(requestCode, resultCode)
        if (resultCode == Activity.RESULT_OK && requestCode == ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS) {
            onSuccessfulKeySubmission()
        }
    }

    private fun onSuccessfulKeySubmission() {
        viewModelScope.launch {
            keySharingInfoProvider.reset()
            submitEpidemiologyDataForTestResult(keySharingInfo)
            navigationLiveData.postValue(ShareKeysResultActivity)
        }
    }

    sealed class ShareKeysInformationNavigateTo : ShareKeysNavigationTarget {
        object StatusActivity : ShareKeysInformationNavigateTo()
    }
}

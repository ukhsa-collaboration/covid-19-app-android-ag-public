package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInReminderScreen
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class ShareKeysReminderViewModel @Inject constructor(
    private val submitObfuscationData: SubmitObfuscationData,
    fetchKeysFlowFactory: FetchKeysFlow.Factory,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
) : ViewModel(), FetchKeysFlow.Callback {

    private val navigationLiveData = SingleLiveEvent<ShareKeysNavigationTarget>()
    fun navigation(): LiveData<ShareKeysNavigationTarget> = navigationLiveData

    private val permissionRequestLiveData = SingleLiveEvent<(Activity) -> Unit>()
    fun permissionRequest(): LiveData<(Activity) -> Unit> = permissionRequestLiveData

    private lateinit var keySharingInfo: KeySharingInfo
    private var hasAlreadyConsentedToShareKeys = false

    private val fetchKeysFlow by lazy {
        fetchKeysFlowFactory.create(
            this,
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

    fun onDoNotShareKeysClicked() {
        keySharingInfoProvider.reset()
        submitObfuscationData()
        navigationLiveData.postValue(Finish)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        fetchKeysFlow.onActivityResult(requestCode, resultCode)
        if (requestCode == ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS) {
            if (resultCode == Activity.RESULT_OK) {
                onSuccessfulKeySubmission()
            } else {
                // Submitting the keys has failed and the user has dismissed the error screen
                onDoNotShareKeysClicked()
            }
        }
    }

    private fun onSuccessfulKeySubmission() {
        keySharingInfoProvider.reset()
        navigationLiveData.postValue(ShareKeysNavigateTo.ShareKeysResultActivity(bookFollowUpTest = false))
    }

    //region FetchKeysFlow.Callback

    override fun onFetchKeysSuccess(
        temporaryExposureKeys: List<NHSTemporaryExposureKey>,
        diagnosisKeySubmissionToken: String
    ) {
        trackConsentedToShareKeys()
        navigationLiveData.postValue(
            SubmitKeysProgressActivity(temporaryExposureKeys, diagnosisKeySubmissionToken)
        )
    }

    override fun onFetchKeysPermissionDenied() {
        onDoNotShareKeysClicked()
    }

    override fun onFetchKeysUnexpectedError() {
        navigationLiveData.postValue(Finish)
    }

    override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
        permissionRequestLiveData.postValue(permissionRequest)
    }

    //endregion

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun trackConsentedToShareKeys() {
        if (!hasAlreadyConsentedToShareKeys) {
            analyticsEventProcessor.track(ConsentedToShareExposureKeysInReminderScreen)
            hasAlreadyConsentedToShareKeys = true
        }
    }
}

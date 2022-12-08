package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysBaseActivity.Companion.REQUEST_CODE_SUBMIT_KEYS
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

abstract class ShareKeysBaseViewModel constructor(
    fetchKeysFlowFactory: FetchKeysFlow.Factory,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel(), FetchKeysFlow.Callback {

    protected abstract val bookFollowUpTest: Boolean
    protected abstract val analyticsEvent: AnalyticsEvent

    protected val navigationLiveData = SingleLiveEvent<ShareKeysNavigationTarget>()
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

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        fetchKeysFlow.onActivityResult(requestCode, resultCode)
        if (requestCode == REQUEST_CODE_SUBMIT_KEYS) {
            if (resultCode == Activity.RESULT_OK) {
                onSuccessfulKeySubmission()
            } else {
                // Submitting the keys has failed and the user has dismissed the error screen
                onFetchKeysPermissionDenied()
            }
        }
    }

    private fun onSuccessfulKeySubmission() {
        keySharingInfoProvider.reset()
        navigationLiveData.postValue(ShareKeysResultActivity(bookFollowUpTest))
    }

    override fun onFetchKeysSuccess(
        temporaryExposureKeys: List<NHSTemporaryExposureKey>,
        diagnosisKeySubmissionToken: String?
    ) {
        if (diagnosisKeySubmissionToken != null) {
            trackConsentedToShareKeys()
            navigationLiveData.postValue(
                SubmitKeysProgressActivity(temporaryExposureKeys, diagnosisKeySubmissionToken)
            )
        } else {
            Timber.e("Unexpected null value for diagnosisKeySubmissionToken returned by onFetchKeysSuccess")
            keySharingInfoProvider.reset()
            navigationLiveData.postValue(Finish)
        }
    }

    override fun onFetchKeysUnexpectedError() {
        navigationLiveData.postValue(Finish)
    }

    override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
        permissionRequestLiveData.postValue(permissionRequest)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun trackConsentedToShareKeys() {
        if (!hasAlreadyConsentedToShareKeys) {
            analyticsEventProcessor.track(analyticsEvent)
            hasAlreadyConsentedToShareKeys = true
        }
    }
}

package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationViewModel.ShareKeysInformationNavigateTo.BookFollowUpTestActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.StatusActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock

class ShareKeysInformationViewModel @AssistedInject constructor(
    fetchKeysFlowFactory: FetchKeysFlow.Factory,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val clock: Clock,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    @Assisted private val bookFollowUpTest: Boolean
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

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        fetchKeysFlow.onActivityResult(requestCode, resultCode)
        if (requestCode == ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS) {
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
        val keysSharingInfo = keySharingInfoProvider.keySharingInfo
        if (keysSharingInfo != null && keysSharingInfo.wasAcknowledgedMoreThan24HoursAgo(clock)) {
            keySharingInfoProvider.reset()
        } else {
            keySharingInfoProvider.setHasDeclinedSharingKeys()
        }
        val navigationTarget = if (bookFollowUpTest) {
            BookFollowUpTestActivity
        } else {
            StatusActivity
        }
        navigationLiveData.postValue(navigationTarget)
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
            analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow)
            hasAlreadyConsentedToShareKeys = true
        }
    }

    sealed class ShareKeysInformationNavigateTo : ShareKeysNavigationTarget {
        object StatusActivity : ShareKeysInformationNavigateTo()
        object BookFollowUpTestActivity : ShareKeysInformationNavigateTo()
    }

    @AssistedFactory
    interface Factory {
        fun create(bookFollowUpTest: Boolean): ShareKeysInformationViewModel
    }
}

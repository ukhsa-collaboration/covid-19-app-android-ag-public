package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInReminderScreen
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import javax.inject.Inject

class ShareKeysReminderViewModel @Inject constructor(
    private val submitObfuscationData: SubmitObfuscationData,
    fetchKeysFlowFactory: FetchKeysFlow.Factory,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    analyticsEventProcessor: AnalyticsEventProcessor,
) : ShareKeysBaseViewModel(fetchKeysFlowFactory, keySharingInfoProvider, analyticsEventProcessor) {

    override val bookFollowUpTest = false
    override val analyticsEvent = ConsentedToShareExposureKeysInReminderScreen

    override fun onFetchKeysPermissionDenied() {
        onDoNotShareKeysClicked()
    }

    fun onDoNotShareKeysClicked() {
        keySharingInfoProvider.reset()
        submitObfuscationData()
        navigationLiveData.postValue(Finish)
    }
}

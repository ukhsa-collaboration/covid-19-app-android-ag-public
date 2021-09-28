package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationViewModel.ShareKeysInformationNavigateTo.BookFollowUpTestActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.StatusActivity
import java.time.Clock

class ShareKeysInformationViewModel @AssistedInject constructor(
    fetchKeysFlowFactory: FetchKeysFlow.Factory,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val clock: Clock,
    analyticsEventProcessor: AnalyticsEventProcessor,
    @Assisted override val bookFollowUpTest: Boolean
) : ShareKeysBaseViewModel(fetchKeysFlowFactory, keySharingInfoProvider, analyticsEventProcessor) {

    override val analyticsEvent = ConsentedToShareExposureKeysInTheInitialFlow

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

    sealed class ShareKeysInformationNavigateTo : ShareKeysNavigationTarget {
        object StatusActivity : ShareKeysInformationNavigateTo()
        object BookFollowUpTestActivity : ShareKeysInformationNavigateTo()
    }

    @AssistedFactory
    interface Factory {
        fun create(bookFollowUpTest: Boolean): ShareKeysInformationViewModel
    }
}

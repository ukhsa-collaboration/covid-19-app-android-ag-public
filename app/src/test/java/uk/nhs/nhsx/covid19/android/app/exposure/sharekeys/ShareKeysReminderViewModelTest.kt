package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInReminderScreen
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysBaseActivity.Companion.REQUEST_CODE_SUBMIT_KEYS
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey

class ShareKeysReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val submitObfuscationData = mockk<SubmitObfuscationData>(relaxUnitFun = true)
    private val fetchKeysFlowFactory = mockk<FetchKeysFlow.Factory>()
    private val fetchKeysFlow = mockk<FetchKeysFlow>(relaxUnitFun = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    private val navigationObserver = mockk<Observer<ShareKeysNavigationTarget>>(relaxUnitFun = true)
    private val permissionRequestObserver = mockk<Observer<(Activity) -> Unit>>(relaxUnitFun = true)

    private val keySharingInfo = mockk<KeySharingInfo>()

    private val testSubject = ShareKeysReminderViewModel(
        submitObfuscationData,
        fetchKeysFlowFactory,
        keySharingInfoProvider,
        analyticsEventProcessor
    )

    @Before
    fun setUp() {
        every { fetchKeysFlowFactory.create(any(), any(), any()) } returns fetchKeysFlow
        every { keySharingInfoProvider.keySharingInfo } returns keySharingInfo
        testSubject.navigation().observeForever(navigationObserver)
        testSubject.permissionRequest().observeForever(permissionRequestObserver)
        testSubject.onCreate()
    }

    @Test
    fun `user chooses to share keys then fetch keys flow is called`() {
        testSubject.onShareKeysButtonClicked()

        verify { fetchKeysFlow() }
    }

    @Test
    fun `user chooses to not share keys`() {
        testSubject.onDoNotShareKeysClicked()

        verifyOrder {
            keySharingInfoProvider.reset()
            submitObfuscationData()
            navigationObserver.onChanged(Finish)
        }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `onActivityResult when request code does not indicate successful key submission`() {
        val unexpectedRequestCode = 1234

        testSubject.onActivityResult(unexpectedRequestCode, Activity.RESULT_OK)

        verify { fetchKeysFlow.onActivityResult(unexpectedRequestCode, Activity.RESULT_OK) }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `onActivityResult after key submission with result canceled`() {
        testSubject.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, Activity.RESULT_CANCELED)

        verifyOrder {
            fetchKeysFlow.onActivityResult(
                REQUEST_CODE_SUBMIT_KEYS,
                Activity.RESULT_CANCELED
            )
            keySharingInfoProvider.reset()
            navigationObserver.onChanged(Finish)
        }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `onActivityResult after successful key submission`() {
        testSubject.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, Activity.RESULT_OK)

        verifyOrder {
            fetchKeysFlow.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, Activity.RESULT_OK)
            keySharingInfoProvider.reset()
            navigationObserver.onChanged(ShareKeysResultActivity(bookFollowUpTest = false))
        }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `analytics for key sharing consent is only tracked once`() {
        testSubject.trackConsentedToShareKeys()
        testSubject.trackConsentedToShareKeys()

        verify(exactly = 1) { analyticsEventProcessor.track(ConsentedToShareExposureKeysInReminderScreen) }
    }

    @Test
    fun `on fetch keys success`() {
        val keys = emptyList<NHSTemporaryExposureKey>()
        val token = ""

        testSubject.onFetchKeysSuccess(keys, token)

        verifyOrder {
            analyticsEventProcessor.track(ConsentedToShareExposureKeysInReminderScreen)
            navigationObserver.onChanged(SubmitKeysProgressActivity(keys, token))
        }
    }

    @Test
    fun `on fetch keys permission denied`() {
        testSubject.onFetchKeysPermissionDenied()

        verifyOrder {
            keySharingInfoProvider.reset()
            submitObfuscationData.invoke()
            navigationObserver.onChanged(Finish)
        }
    }

    @Test
    fun `on fetch keys unexpected error`() {
        testSubject.onFetchKeysUnexpectedError()

        verify { navigationObserver.onChanged(Finish) }
    }

    @Test
    fun `on fetch keys permission required`() {
        val permissionRequest: (Activity) -> Unit = { }

        testSubject.onPermissionRequired(permissionRequest)

        verify { permissionRequestObserver.onChanged(permissionRequest) }
    }
}

package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
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
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysBaseActivity.Companion.REQUEST_CODE_SUBMIT_KEYS
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationViewModel.ShareKeysInformationNavigateTo.BookFollowUpTestActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.StatusActivity
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ShareKeysInformationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fetchKeysFlowFactory = mockk<FetchKeysFlow.Factory>()
    private val fetchKeysFlow = mockk<FetchKeysFlow>(relaxUnitFun = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T00:05:00.00Z"), ZoneOffset.UTC)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val navigationObserver = mockk<Observer<ShareKeysNavigationTarget>>(relaxUnitFun = true)
    private val permissionRequestObserver = mockk<Observer<(Activity) -> Unit>>(relaxUnitFun = true)
    private val keySharingInfo = mockk<KeySharingInfo>()

    private val testSubject = ShareKeysInformationViewModel(
        fetchKeysFlowFactory,
        keySharingInfoProvider,
        fixedClock,
        analyticsEventProcessor,
        bookFollowUpTest = false
    )

    private val testSubjectWithBookFollowUpTest = ShareKeysInformationViewModel(
        fetchKeysFlowFactory,
        keySharingInfoProvider,
        fixedClock,
        analyticsEventProcessor,
        bookFollowUpTest = true
    )

    @Before
    fun setUp() {
        every { keySharingInfoProvider.keySharingInfo } returns keySharingInfo
        every { fetchKeysFlowFactory.create(any(), any(), any()) } returns fetchKeysFlow
        testSubject.navigation().observeForever(navigationObserver)
        testSubjectWithBookFollowUpTest.navigation().observeForever(navigationObserver)
        testSubject.permissionRequest().observeForever(permissionRequestObserver)
        testSubject.onCreate()
        testSubjectWithBookFollowUpTest.onCreate()
    }

    @Test
    fun `when user chooses to submit keys then fetch keys flow is called`() {
        testSubject.onShareKeysButtonClicked()

        verify { fetchKeysFlow() }
    }

    @Test
    fun `onActivityResult when request code does not indicate successful key submission`() {
        val unexpectedRequestCode = 1234

        testSubject.onActivityResult(unexpectedRequestCode, RESULT_OK)

        verify { fetchKeysFlow.onActivityResult(unexpectedRequestCode, RESULT_OK) }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `onActivityResult after key submission with result canceled`() {
        every { keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(fixedClock) } returns true

        testSubject.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, RESULT_CANCELED)

        verifyOrder {
            fetchKeysFlow.onActivityResult(
                REQUEST_CODE_SUBMIT_KEYS,
                RESULT_CANCELED
            )
            keySharingInfoProvider.reset()
            navigationObserver.onChanged(StatusActivity)
        }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `onActivityResult after successful key submission navigates to ShareKeysResultActivity without follow-up test`() {
        testSubject.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, RESULT_OK)

        verifyOrder {
            fetchKeysFlow.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, RESULT_OK)
            keySharingInfoProvider.reset()
            navigationObserver.onChanged(ShareKeysResultActivity(bookFollowUpTest = false))
        }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `onActivityResult after successful key submission navigates to ShareKeysResultActivity with follow-up test`() {
        testSubjectWithBookFollowUpTest.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, RESULT_OK)

        verifyOrder {
            fetchKeysFlow.onActivityResult(REQUEST_CODE_SUBMIT_KEYS, RESULT_OK)
            keySharingInfoProvider.reset()
            navigationObserver.onChanged(ShareKeysResultActivity(bookFollowUpTest = true))
        }

        confirmVerified(fetchKeysFlow, navigationObserver)
    }

    @Test
    fun `on fetch keys success with token navigates to SubmitKeysProgressActivity`() {
        val keys = emptyList<NHSTemporaryExposureKey>()
        val token = ""

        testSubject.onFetchKeysSuccess(keys, token)

        verifyOrder {
            analyticsEventProcessor.track(ConsentedToShareExposureKeysInTheInitialFlow)
            navigationObserver.onChanged(SubmitKeysProgressActivity(keys, token))
        }
    }

    @Test
    fun `on fetch keys unexpectedly called without token finishes and reset keySharingInfo`() {
        val keys = emptyList<NHSTemporaryExposureKey>()
        val token = null

        testSubject.onFetchKeysSuccess(keys, token)

        verifyOrder {
            keySharingInfoProvider.reset()
            navigationObserver.onChanged(Finish)
        }
    }

    @Test
    fun `on fetch keys permission denied with old acknowledgement`() {
        every { keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(fixedClock) } returns true

        testSubject.onFetchKeysPermissionDenied()

        verifyOrder {
            keySharingInfoProvider.reset()
            navigationObserver.onChanged(StatusActivity)
        }
    }

    @Test
    fun `on fetch keys permission denied with recent acknowledgement`() {
        every { keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(fixedClock) } returns false

        testSubject.onFetchKeysPermissionDenied()

        verifyOrder {
            keySharingInfoProvider.setHasDeclinedSharingKeys()
            navigationObserver.onChanged(StatusActivity)
        }
    }

    @Test
    fun `on fetch keys permission denied with recent acknowledgement and book follow up test`() {
        every { keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(fixedClock) } returns false

        testSubjectWithBookFollowUpTest.onFetchKeysPermissionDenied()

        verifyOrder {
            keySharingInfoProvider.setHasDeclinedSharingKeys()
            navigationObserver.onChanged(BookFollowUpTestActivity)
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

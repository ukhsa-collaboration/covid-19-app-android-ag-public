package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

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
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ShareKeysInformationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val submitEpidemiologyDataForTestResult = mockk<SubmitEpidemiologyDataForTestResult>(relaxUnitFun = true)
    private val fetchKeysFlowFactory = mockk<FetchKeysFlow.Factory>()
    private val fetchKeysFlow = mockk<FetchKeysFlow>(relaxUnitFun = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T00:05:00.00Z"), ZoneOffset.UTC)

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "token1",
        acknowledgedDate = Instant.parse("2020-07-10T01:00:00.00Z"),
        notificationSentDate = null,
        testKitType = LAB_RESULT,
        requiresConfirmatoryTest = false
    )

    private val testSubject = ShareKeysInformationViewModel(
        submitEpidemiologyDataForTestResult,
        fetchKeysFlowFactory,
        keySharingInfoProvider,
        fixedClock
    )

    private val navigationObserver = mockk<Observer<ShareKeysNavigationTarget>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        every { keySharingInfoProvider.keySharingInfo } returns keySharingInfo
        every { fetchKeysFlowFactory.create(any(), any(), any()) } returns fetchKeysFlow
        testSubject.navigation().observeForever(navigationObserver)
        testSubject.onCreate()
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

        confirmVerified(fetchKeysFlow, submitEpidemiologyDataForTestResult, navigationObserver)
    }

    @Test
    fun `onActivityResult after key submission with result canceled`() {
        testSubject.onActivityResult(ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS, RESULT_CANCELED)

        verify {
            fetchKeysFlow.onActivityResult(
                ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS,
                RESULT_CANCELED
            )
        }

        confirmVerified(fetchKeysFlow, submitEpidemiologyDataForTestResult, navigationObserver)
    }

    @Test
    fun `onActivityResult after successful key submission`() {
        testSubject.onActivityResult(ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS, RESULT_OK)

        verifyOrder {
            fetchKeysFlow.onActivityResult(ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS, RESULT_OK)
            keySharingInfoProvider.reset()
            submitEpidemiologyDataForTestResult(keySharingInfo)
            navigationObserver.onChanged(ShareKeysResultActivity)
        }

        confirmVerified(fetchKeysFlow, submitEpidemiologyDataForTestResult, navigationObserver)
    }
}

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
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysNavigateTo.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import java.time.Instant

class ShareKeysReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val submitObfuscationData = mockk<SubmitObfuscationData>(relaxUnitFun = true)
    private val submitEpidemiologyDataForTestResult = mockk<SubmitEpidemiologyDataForTestResult>(relaxUnitFun = true)
    private val fetchKeysFlowFactory = mockk<FetchKeysFlow.Factory>()
    private val fetchKeysFlow = mockk<FetchKeysFlow>(relaxUnitFun = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val navigationObserver = mockk<Observer<ShareKeysNavigationTarget>>(relaxUnitFun = true)

    private val keySharingInfo = KeySharingInfo(
        diagnosisKeySubmissionToken = "token1",
        acknowledgedDate = Instant.now(),
        notificationSentDate = null,
        testKitType = LAB_RESULT,
        requiresConfirmatoryTest = false
    )

    private val testSubject = ShareKeysReminderViewModel(
        submitObfuscationData,
        submitEpidemiologyDataForTestResult,
        fetchKeysFlowFactory,
        keySharingInfoProvider
    )

    @Before
    fun setUp() {
        every { fetchKeysFlowFactory.create(any(), any(), any()) } returns fetchKeysFlow
        every { keySharingInfoProvider.keySharingInfo } returns keySharingInfo
        testSubject.navigation().observeForever(navigationObserver)
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

        confirmVerified(fetchKeysFlow, submitEpidemiologyDataForTestResult, navigationObserver)
    }

    @Test
    fun `onActivityResult when request code does not indicate successful key submission`() {
        val unexpectedRequestCode = 1234

        testSubject.onActivityResult(unexpectedRequestCode, Activity.RESULT_OK)

        verify { fetchKeysFlow.onActivityResult(unexpectedRequestCode, Activity.RESULT_OK) }

        confirmVerified(fetchKeysFlow, submitEpidemiologyDataForTestResult, navigationObserver)
    }

    @Test
    fun `onActivityResult after key submission with result canceled`() {
        testSubject.onActivityResult(ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS, Activity.RESULT_CANCELED)

        verify {
            fetchKeysFlow.onActivityResult(
                ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS,
                Activity.RESULT_CANCELED
            )
        }

        confirmVerified(fetchKeysFlow, submitEpidemiologyDataForTestResult, navigationObserver)
    }

    @Test
    fun `onActivityResult after successful key submission`() {
        testSubject.onActivityResult(ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS, Activity.RESULT_OK)

        verifyOrder {
            fetchKeysFlow.onActivityResult(ShareKeysInformationActivity.REQUEST_CODE_SUBMIT_KEYS, Activity.RESULT_OK)
            keySharingInfoProvider.reset()
            submitEpidemiologyDataForTestResult(keySharingInfo)
            navigationObserver.onChanged(ShareKeysResultActivity)
        }

        confirmVerified(fetchKeysFlow, submitEpidemiologyDataForTestResult, navigationObserver)
    }
}

package uk.nhs.nhsx.covid19.android.app.exposure

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeKeys
import java.time.Instant

class ShareKeysInformationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fetchTemporaryExposureKeys = mockk<FetchTemporaryExposureKeys>(relaxed = true)
    private val submitFakeKeys = mockk<SubmitFakeKeys>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)

    private val testSubject =
        ShareKeysInformationViewModel(fetchTemporaryExposureKeys, submitFakeKeys, stateMachine)

    private val fetchExposureKeysObserver = mockk<Observer<TemporaryExposureKeysFetchResult>>(relaxed = true)

    private val exposureKeys = listOf(
        NHSTemporaryExposureKey("key1", 1),
        NHSTemporaryExposureKey("key2", 2)
    )

    @Test
    fun `fetching keys returns list of exposure keys on success`() = runBlocking {
        coEvery { fetchTemporaryExposureKeys.invoke() } returns Success(exposureKeys)

        testSubject.fetchKeysResult().observeForever(fetchExposureKeysObserver)

        testSubject.fetchKeys()

        coVerify { fetchTemporaryExposureKeys.invoke() }
        verify { fetchExposureKeysObserver.onChanged(Success(exposureKeys)) }
    }

    @Test
    fun `invoke fake key submission when keys are not submitted`() = runBlocking {
        testSubject.onKeysNotSubmitted()

        coVerify { submitFakeKeys.invoke() }
    }

    @Test
    fun `acknowledging test results triggers state machine acknowledge event`() {
        testSubject.testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "a",
            testEndDate = Instant.now(),
            testResult = POSITIVE,
            acknowledgedDate = Instant.now()
        )

        testSubject.acknowledgeTestResult()

        coVerify {
            stateMachine.processEvent(
                OnTestResultAcknowledge(testSubject.testResult, removeTestResult = false)
            )
        }
    }
}

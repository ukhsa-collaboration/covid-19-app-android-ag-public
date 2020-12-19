package uk.nhs.nhsx.covid19.android.app.exposure

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeKeys
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ShareKeysInformationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fetchTemporaryExposureKeys = mockk<FetchTemporaryExposureKeys>(relaxed = true)
    private val submitFakeKeys = mockk<SubmitFakeKeys>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxed = true)
    private val submitEpidemiologyData = mockk<SubmitEpidemiologyData>(relaxed = true)
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val onsetDateBasedOnTestEndDate = LocalDate.parse("2020-05-18")

    private val testSubject =
        ShareKeysInformationViewModel(
            fetchTemporaryExposureKeys,
            submitFakeKeys,
            stateMachine,
            epidemiologyEventProvider,
            submitEpidemiologyData,
            submitFakeExposureWindows,
            fixedClock
        )

    private val fetchExposureKeysObserver = mockk<Observer<TemporaryExposureKeysFetchResult>>(relaxed = true)

    private val exposureKeys = listOf(
        NHSTemporaryExposureKey("key1", 1),
        NHSTemporaryExposureKey("key2", 2)
    )

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `fetching keys returns list of exposure keys on success`() = runBlocking {
        coEvery { fetchTemporaryExposureKeys.invoke(onsetDateBasedOnTestEndDate) } returns Success(exposureKeys)
        testSubject.testResult = receivedTestResult

        testSubject.fetchKeysResult().observeForever(fetchExposureKeysObserver)

        testSubject.fetchKeys()

        coVerify { fetchTemporaryExposureKeys.invoke(onsetDateBasedOnTestEndDate) }
        verify { fetchExposureKeysObserver.onChanged(Success(exposureKeys)) }
    }

    @Test
    fun `when key submission denied submit fake keys and acknowledge test result`() = runBlocking {
        testSubject.testResult = receivedTestResult

        testSubject.onSubmitKeysDenied()

        coVerify { submitFakeKeys.invoke() }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }
        verify {
            stateMachine.processEvent(
                OnTestResultAcknowledge(testSubject.testResult, removeTestResult = false)
            )
        }
    }

    @Test
    fun `when key submission succeeded and store exposure keys feature flag disabled only acknowledge test result`() {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)
        testSubject.testResult = receivedTestResult

        testSubject.onSubmitKeysSuccess()

        verify {
            stateMachine.processEvent(
                OnTestResultAcknowledge(testSubject.testResult, removeTestResult = false)
            )
        }
        verify(exactly = 0) { submitEpidemiologyData.invoke(any(), any()) }
    }

    @Test
    fun `when key submission succeeded and store exposure keys feature flag enabled acknowledge test result and submit epidemiology events`() {
        testSubject.testResult = receivedTestResult

        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

        every { epidemiologyEventProvider.epidemiologyEvents } returns listOf()

        testSubject.onSubmitKeysSuccess()

        verify {
            stateMachine.processEvent(
                OnTestResultAcknowledge(testSubject.testResult, removeTestResult = false)
            )
        }
        verify { submitEpidemiologyData.invoke(listOf(), epidemiologyEventType = EXPOSURE_WINDOW_POSITIVE_TEST) }
    }

    private val receivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "a",
        testEndDate = fixedClock.instant(),
        testResult = POSITIVE,
        acknowledgedDate = fixedClock.instant()
    )
}

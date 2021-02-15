package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultErrorType.UNEXPECTED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Error
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Progress
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultViewState.Valid

class LinkTestResultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val ctaTokenValidator = mockk<CtaTokenValidator>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)

    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val testSubject =
        LinkTestResultViewModel(ctaTokenValidator, isolationStateMachine, analyticsEventProcessor)

    private val linkTestResultObserver = mockk<Observer<LinkTestResultViewState>>(relaxed = true)

    @Test
    fun `successful cta token validation`() = runBlocking {
        testSubject.viewState().observeForever(linkTestResultObserver)
        val testResultResponse = setResult(NEGATIVE, LAB_RESULT)

        testSubject.validate("ctaToken")

        val event = OnTestResult(
            testResult = ReceivedTestResult(
                testResultResponse.diagnosisKeySubmissionToken,
                testResultResponse.testEndDate,
                testResultResponse.testResult,
                testResultResponse.testKit,
                testResultResponse.diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest = false
            ),
            showNotification = false
        )

        verify { isolationStateMachine.processEvent(event) }

        verifyOrder {
            linkTestResultObserver.onChanged(Progress)
            linkTestResultObserver.onChanged(Valid)
        }
    }

    @Test
    fun `cta token invalid`() = runBlocking {
        testSubject.viewState().observeForever(linkTestResultObserver)

        coEvery { ctaTokenValidator.validate(any()) } returns Failure(INVALID)

        testSubject.validate("ctaToken")

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }

        verifyOrder {
            linkTestResultObserver.onChanged(Progress)
            linkTestResultObserver.onChanged(Error(INVALID))
        }
    }

    @Test
    fun `cta token validation without internet connection`() = runBlocking {
        testSubject.viewState().observeForever(linkTestResultObserver)

        coEvery { ctaTokenValidator.validate(any()) } returns Failure(NO_CONNECTION)

        testSubject.validate("ctaToken")

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }

        verifyOrder {
            linkTestResultObserver.onChanged(Progress)
            linkTestResultObserver.onChanged(Error(NO_CONNECTION))
        }
    }

    @Test
    fun `cta token validation returns unexpected error`() = runBlocking {
        testSubject.viewState().observeForever(linkTestResultObserver)

        coEvery { ctaTokenValidator.validate(any()) } returns Failure(UNEXPECTED)

        testSubject.validate("ctaToken")

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }

        verifyOrder {
            linkTestResultObserver.onChanged(Progress)
            linkTestResultObserver.onChanged(Error(UNEXPECTED))
        }
    }

    @Test
    fun `track analytics events on negative PCR result`() = runBlocking {
        setResult(NEGATIVE, LAB_RESULT)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(NegativeResultReceived)
            analyticsEventProcessor.track(ResultReceived(NEGATIVE, LAB_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on positive PCR result`() = runBlocking {
        setResult(POSITIVE, LAB_RESULT)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(PositiveResultReceived)
            analyticsEventProcessor.track(ResultReceived(POSITIVE, LAB_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on void PCR result`() = runBlocking {
        setResult(VOID, LAB_RESULT)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(VoidResultReceived)
            analyticsEventProcessor.track(ResultReceived(VOID, LAB_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on negative assisted LFD result`() = runBlocking {
        setResult(NEGATIVE, RAPID_RESULT)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(NegativeResultReceived)
            analyticsEventProcessor.track(ResultReceived(NEGATIVE, RAPID_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on negative unassisted LFD result`() = runBlocking {
        setResult(NEGATIVE, RAPID_SELF_REPORTED)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(NegativeResultReceived)
            analyticsEventProcessor.track(ResultReceived(NEGATIVE, RAPID_SELF_REPORTED, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on positive assisted LFD result`() = runBlocking {
        setResult(POSITIVE, RAPID_RESULT)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(PositiveResultReceived)
            analyticsEventProcessor.track(ResultReceived(POSITIVE, RAPID_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on positive unassisted LFD result`() = runBlocking {
        setResult(POSITIVE, RAPID_SELF_REPORTED)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(PositiveResultReceived)
            analyticsEventProcessor.track(ResultReceived(POSITIVE, RAPID_SELF_REPORTED, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on void assisted LFD result`() = runBlocking {
        setResult(VOID, RAPID_RESULT)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(VoidResultReceived)
            analyticsEventProcessor.track(ResultReceived(VOID, RAPID_RESULT, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on void unassisted LFD result`() = runBlocking {
        setResult(VOID, RAPID_SELF_REPORTED)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(VoidResultReceived)
            analyticsEventProcessor.track(ResultReceived(VOID, RAPID_SELF_REPORTED, OUTSIDE_APP))
        }
    }

    private fun setResult(
        result: VirologyTestResult,
        testKitType: VirologyTestKitType,
        diagnosisKeySubmissionToken: String = "submissionToken",
        testEndDate: Instant = Instant.now(),
        diagnosisKeySubmissionSupported: Boolean = true,
        requiresConfirmatoryTest: Boolean = false
    ): VirologyCtaExchangeResponse {
        val testResultResponse =
            VirologyCtaExchangeResponse(
                diagnosisKeySubmissionToken,
                testEndDate,
                result,
                testKitType,
                diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest
            )
        coEvery { ctaTokenValidator.validate(any()) } returns Success(testResultResponse)
        return testResultResponse
    }
}

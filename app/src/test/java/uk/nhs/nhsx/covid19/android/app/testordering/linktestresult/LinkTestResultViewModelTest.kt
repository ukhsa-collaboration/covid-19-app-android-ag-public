package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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
import java.time.Instant

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
        val testResultResponse = setResult(NEGATIVE)

        testSubject.validate("ctaToken")

        val event = OnTestResult(
            testResult = ReceivedTestResult(
                testResultResponse.diagnosisKeySubmissionToken,
                testResultResponse.testEndDate,
                testResultResponse.testResult
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
    fun `track analytics events on negative result`() = runBlocking {
        setResult(NEGATIVE)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(NegativeResultReceived)
            analyticsEventProcessor.track(ResultReceived(NEGATIVE, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on positive result`() = runBlocking {
        setResult(POSITIVE)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(PositiveResultReceived)
            analyticsEventProcessor.track(ResultReceived(POSITIVE, OUTSIDE_APP))
        }
    }

    @Test
    fun `track analytics events on void result`() = runBlocking {
        setResult(VOID)

        testSubject.validate("ctaToken")

        coVerifyAll {
            analyticsEventProcessor.track(VoidResultReceived)
            analyticsEventProcessor.track(ResultReceived(VOID, OUTSIDE_APP))
        }
    }

    private fun setResult(
        result: VirologyTestResult,
        diagnosisKeySubmissionToken: String = "submissionToken",
        testEndDate: Instant = Instant.now()
    ): VirologyCtaExchangeResponse {
        val testResultResponse =
            VirologyCtaExchangeResponse(diagnosisKeySubmissionToken, testEndDate, result)
        coEvery { ctaTokenValidator.validate(any()) } returns Success(testResultResponse)
        return testResultResponse
    }
}

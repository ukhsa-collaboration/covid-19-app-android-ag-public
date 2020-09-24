package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
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

    private val testSubject = LinkTestResultViewModel(ctaTokenValidator, isolationStateMachine)

    private val linkTestResultObserver = mockk<Observer<LinkTestResultViewState>>(relaxed = true)

    @Test
    fun `successful cta token validation`() = runBlocking {
        testSubject.viewState().observeForever(linkTestResultObserver)

        val testResultResponse =
            VirologyCtaExchangeResponse("submissionToken", Instant.now(), NEGATIVE)
        coEvery { ctaTokenValidator.validate(any()) } returns Success(testResultResponse)

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
}

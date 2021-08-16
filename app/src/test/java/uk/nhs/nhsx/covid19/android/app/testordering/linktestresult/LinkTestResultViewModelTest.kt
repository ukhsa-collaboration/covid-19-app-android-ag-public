package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResult
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Failure
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.Success
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.CtaTokenValidationResult.UnparsableTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.CtaTokenValidator.ValidationErrorType
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.ErrorState
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.INVALID
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.NO_CONNECTION
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultError.UNEXPECTED
import uk.nhs.nhsx.covid19.android.app.testordering.linktestresult.LinkTestResultViewModel.LinkTestResultState
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class LinkTestResultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val ctaTokenValidator = mockk<CtaTokenValidator>(relaxed = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val linkTestResultOnsetDateNeededChecker = mockk<LinkTestResultOnsetDateNeededChecker>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val receivedUnknownTestResultProvider = mockk<ReceivedUnknownTestResultProvider>(relaxUnitFun = true)
    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = LinkTestResultViewModel(
        ctaTokenValidator,
        isolationStateMachine,
        linkTestResultOnsetDateNeededChecker,
        receivedUnknownTestResultProvider
    )

    private val viewStateObserver = mockk<Observer<LinkTestResultState>>(relaxed = true)
    private val validationOnsetDateNeeded = mockk<Observer<ReceivedTestResult>>(relaxed = true)
    private val validationCompletedObserver = mockk<Observer<Unit>>(relaxed = true)

    @Before
    fun setUp() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.validationOnsetDateNeeded().observeForever(validationOnsetDateNeeded)
        testSubject.validationCompleted().observeForever(validationCompletedObserver)

        setIsolationState(indexAndContactCaseIsolation)
        testSubject.fetchInitialViewState()
    }

    @Test
    fun `continue button clicked with CTA token entered should return start validation`() =
        runBlocking {
            setResult(POSITIVE, LAB_RESULT)

            testSubject.ctaToken = "test"
            testSubject.onContinueButtonClicked()

            verifyOrder {
                viewStateObserver.onChanged(LinkTestResultState())
                viewStateObserver.onChanged(
                    LinkTestResultState(showValidationProgress = true)
                )
            }
            coVerify { ctaTokenValidator.validate("test") }
        }

    @Test
    fun `continue button should start validation`() =
        runBlocking {
            setIsolationState(indexAndContactCaseIsolation)
            setResult(POSITIVE, LAB_RESULT)

            testSubject.ctaToken = "test"
            testSubject.fetchInitialViewState()
            testSubject.onContinueButtonClicked()

            verify { viewStateObserver.onChanged(LinkTestResultState(showValidationProgress = true)) }
            coVerify { ctaTokenValidator.validate(any()) }
        }

    @Test
    fun `successful cta token validation, onset date needed`() = runBlocking {
        val testResultResponse = setResult(
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true,
            confirmatoryDayLimit = 2
        )

        val testResult = ReceivedTestResult(
            testResultResponse.diagnosisKeySubmissionToken,
            testResultResponse.testEndDate,
            testResultResponse.testResult,
            testResultResponse.testKit,
            testResultResponse.diagnosisKeySubmissionSupported,
            requiresConfirmatoryTest = testResultResponse.requiresConfirmatoryTest,
            confirmatoryDayLimit = testResultResponse.confirmatoryDayLimit
        )

        every { linkTestResultOnsetDateNeededChecker.isInterestedInAskingForSymptomsOnsetDay(testResult) } returns true

        testSubject.ctaToken = "ctaToken"
        testSubject.onContinueButtonClicked()

        val event = OnTestResult(
            testResult = testResult,
            showNotification = false,
            testOrderType = OUTSIDE_APP,
        )

        verify { isolationStateMachine.processEvent(event) }

        verifyOrder {
            viewStateObserver.onChanged(LinkTestResultState(showValidationProgress = true))
            validationOnsetDateNeeded.onChanged(testResult)
        }
        verify(exactly = 0) { validationCompletedObserver.onChanged(any()) }
    }

    @Test
    fun `successful cta token validation, onset date not needed`() = runBlocking {
        val testResultResponse = setResult(NEGATIVE, LAB_RESULT)
        val testResult = ReceivedTestResult(
            testResultResponse.diagnosisKeySubmissionToken,
            testResultResponse.testEndDate,
            testResultResponse.testResult,
            testResultResponse.testKit,
            testResultResponse.diagnosisKeySubmissionSupported,
            requiresConfirmatoryTest = testResultResponse.requiresConfirmatoryTest
        )

        every { linkTestResultOnsetDateNeededChecker.isInterestedInAskingForSymptomsOnsetDay(testResult) } returns false

        testSubject.ctaToken = "ctaToken"
        testSubject.onContinueButtonClicked()

        val event = OnTestResult(
            testResult = testResult,
            showNotification = false,
            testOrderType = OUTSIDE_APP,
        )

        verify { isolationStateMachine.processEvent(event) }

        verifyOrder {
            viewStateObserver.onChanged(LinkTestResultState(showValidationProgress = true))
            validationCompletedObserver.onChanged(null)
        }
        verify(exactly = 0) { validationOnsetDateNeeded.onChanged(any()) }
    }

    @Test
    fun `cta token invalid`() = runBlocking {
        coEvery { ctaTokenValidator.validate(any()) } returns Failure(ValidationErrorType.INVALID)

        testSubject.ctaToken = "ctaToken"
        testSubject.onContinueButtonClicked()

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }

        verifyOrder {
            viewStateObserver.onChanged(LinkTestResultState(showValidationProgress = true))
            viewStateObserver.onChanged(
                LinkTestResultState(
                    showValidationProgress = false,
                    errorState = ErrorState(INVALID)
                )
            )
        }
    }

    @Test
    fun `cta token validation without internet connection`() = runBlocking {
        coEvery { ctaTokenValidator.validate(any()) } returns Failure(ValidationErrorType.NO_CONNECTION)

        testSubject.ctaToken = "ctaToken"
        testSubject.onContinueButtonClicked()

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }

        verifyOrder {
            viewStateObserver.onChanged(LinkTestResultState(showValidationProgress = true))
            viewStateObserver.onChanged(
                LinkTestResultState(
                    showValidationProgress = false,
                    errorState = ErrorState(NO_CONNECTION)
                )
            )
        }
    }

    @Test
    fun `cta token validation returns unknown test result`() = runBlocking {
        coEvery { ctaTokenValidator.validate(any()) } returns UnparsableTestResult

        testSubject.ctaToken = "ctaToken"
        testSubject.onContinueButtonClicked()

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }

        verifyOrder {
            receivedUnknownTestResultProvider setProperty "value" value true
            validationCompletedObserver.onChanged(any())
        }
    }

    @Test
    fun `cta token validation returns unexpected error`() = runBlocking {
        coEvery { ctaTokenValidator.validate(any()) } returns Failure(ValidationErrorType.UNEXPECTED)

        testSubject.ctaToken = "ctaToken"
        testSubject.onContinueButtonClicked()

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }

        verifyOrder {
            viewStateObserver.onChanged(LinkTestResultState(showValidationProgress = true))
            viewStateObserver.onChanged(
                LinkTestResultState(
                    showValidationProgress = false,
                    errorState = ErrorState(UNEXPECTED)
                )
            )
        }
    }

    private fun setResult(
        result: VirologyTestResult,
        testKitType: VirologyTestKitType,
        diagnosisKeySubmissionToken: String = "submissionToken",
        testEndDate: Instant = Instant.now(),
        diagnosisKeySubmissionSupported: Boolean = true,
        requiresConfirmatoryTest: Boolean = false,
        confirmatoryDayLimit: Int? = null
    ): VirologyCtaExchangeResponse {
        val testResultResponse =
            VirologyCtaExchangeResponse(
                diagnosisKeySubmissionToken,
                testEndDate,
                result,
                testKitType,
                diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest,
                confirmatoryDayLimit
            )
        coEvery { ctaTokenValidator.validate(any()) } returns Success(testResultResponse)
        return testResultResponse
    }

    private fun setIsolationState(isolationState: IsolationState) {
        every { isolationStateMachine.readState() } returns isolationState
        every { isolationStateMachine.readLogicalState() } returns isolationState.asLogical()
    }

    private val indexAndContactCaseIsolation = IsolationState(
        isolationConfiguration = DurationDays(),
        contactCase = isolationHelper.contactCase(),
        indexInfo = isolationHelper.positiveTest(
            AcknowledgedTestResult(
                testEndDate = LocalDate.now(fixedClock),
                acknowledgedDate = LocalDate.now(fixedClock),
                testResult = RelevantVirologyTestResult.POSITIVE,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                confirmedDate = null
            )
        )
    )
}

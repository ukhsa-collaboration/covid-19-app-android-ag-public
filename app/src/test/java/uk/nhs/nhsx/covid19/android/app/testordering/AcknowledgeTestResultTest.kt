package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitEpidemiologyDataForTestResult
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitObfuscationData
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.FoundTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.NoTestResult

class AcknowledgeTestResultTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val submitObfuscationData = mockk<SubmitObfuscationData>(relaxUnitFun = true)
    private val submitEmptyData = mockk<SubmitEmptyData>(relaxUnitFun = true)
    private val submitEpidemiologyDataForTestResult = mockk<SubmitEpidemiologyDataForTestResult>(relaxUnitFun = true)
    private val isKeySubmissionSupported = mockk<IsKeySubmissionSupported>()
    private val getHighestPriorityTestResult = mockk<GetHighestPriorityTestResult>()

    private val acknowledgeTestResult = AcknowledgeTestResult(
        isolationStateMachine,
        submitObfuscationData,
        submitEmptyData,
        submitEpidemiologyDataForTestResult,
        isKeySubmissionSupported,
        getHighestPriorityTestResult
    )

    private val expectedTestResult = mockk<ReceivedTestResult>()
    private val expectedTestKitType = LAB_RESULT
    private val expectedRequiresConfirmatoryTest = false

    @Before
    fun setUp() {
        every { expectedTestResult.isPositive() } returns true
        every { expectedTestResult.testKitType } returns expectedTestKitType
        every { expectedTestResult.requiresConfirmatoryTest } returns expectedRequiresConfirmatoryTest
    }

    @After
    fun tearDown() {
        confirmVerificationForAllMocksComplete()
    }

    @Test
    fun `when no highest-priority unacknowledged test result was found then do nothing`() {
        every { getHighestPriorityTestResult() } returns NoTestResult

        acknowledgeTestResult()

        verify { getHighestPriorityTestResult() }
    }

    @Test
    fun `acknowledge positive test result with key submission supported`() {
        every { getHighestPriorityTestResult() } returns FoundTestResult(expectedTestResult)
        every { isKeySubmissionSupported(expectedTestResult) } returns true

        acknowledgeTestResult()

        verifyOrder {
            getHighestPriorityTestResult()
            submitEpidemiologyDataForTestResult(expectedTestKitType, expectedRequiresConfirmatoryTest)
            isKeySubmissionSupported(expectedTestResult)
            isolationStateMachine.processEvent(OnTestResultAcknowledge(expectedTestResult))
        }
    }

    @Test
    fun `acknowledge positive test result with key submission not supported`() {
        every { getHighestPriorityTestResult() } returns FoundTestResult(expectedTestResult)
        every { isKeySubmissionSupported(expectedTestResult) } returns false

        acknowledgeTestResult()

        verifyOrder {
            getHighestPriorityTestResult()
            submitEpidemiologyDataForTestResult(expectedTestKitType, expectedRequiresConfirmatoryTest)
            isKeySubmissionSupported(expectedTestResult)
            submitEmptyData()
            isolationStateMachine.processEvent(OnTestResultAcknowledge(expectedTestResult))
        }
    }

    @Test
    fun `acknowledge non-positive test result`() {
        every { expectedTestResult.isPositive() } returns false
        every { getHighestPriorityTestResult() } returns FoundTestResult(expectedTestResult)

        acknowledgeTestResult()

        verifyOrder {
            getHighestPriorityTestResult()
            submitObfuscationData()
            isolationStateMachine.processEvent(OnTestResultAcknowledge(expectedTestResult))
        }
    }

    @Test
    fun `invoke with test result parameter - acknowledge positive test result with key submission supported`() {

        every { isKeySubmissionSupported(expectedTestResult) } returns true

        acknowledgeTestResult(expectedTestResult)

        verifyOrder {
            submitEpidemiologyDataForTestResult(expectedTestKitType, expectedRequiresConfirmatoryTest)
            isKeySubmissionSupported(expectedTestResult)
            isolationStateMachine.processEvent(OnTestResultAcknowledge(expectedTestResult))
        }
        verify(exactly = 0) { getHighestPriorityTestResult() }
    }

    @Test
    fun `invoke with test result parameter - acknowledge positive test result with key submission not supported`() {
        every { isKeySubmissionSupported(expectedTestResult) } returns false

        acknowledgeTestResult(expectedTestResult)

        verifyOrder {
            submitEpidemiologyDataForTestResult(expectedTestKitType, expectedRequiresConfirmatoryTest)
            isKeySubmissionSupported(expectedTestResult)
            submitEmptyData()
            isolationStateMachine.processEvent(OnTestResultAcknowledge(expectedTestResult))
        }
        verify(exactly = 0) { getHighestPriorityTestResult() }
    }

    @Test
    fun `invoke with test result parameter - acknowledge non-positive test result`() {
        every { expectedTestResult.isPositive() } returns false

        acknowledgeTestResult(expectedTestResult)

        verifyOrder {
            submitObfuscationData()
            isolationStateMachine.processEvent(OnTestResultAcknowledge(expectedTestResult))
        }
        verify(exactly = 0) { getHighestPriorityTestResult() }
    }

    private fun confirmVerificationForAllMocksComplete() {
        confirmVerified(
            isolationStateMachine,
            submitObfuscationData,
            submitEmptyData,
            submitEpidemiologyDataForTestResult,
            isKeySubmissionSupported,
            getHighestPriorityTestResult
        )
    }
}

package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertTrue

class LinkTestResultOnsetDateNeededCheckerTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = LinkTestResultOnsetDateNeededChecker(
        isolationStateMachine
    )

    // AC1 - test is LAB_RESULT, positive and does not require confirmation
    // AC2 - not considered symptomatic
    // AC3 - must not have a previous positive test result

    @Test
    fun `onset date needed when AC1 true, AC2 true due to no current index case, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.contactCase().asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to no previous index case, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.contactCase(expired = true).asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    // Not testable because it's impossible:
    //   onset date needed when AC1 true, AC2 true due to index case no self assessment, AC3 true
    // An ongoing (not expired) index case must have either self assessment or a positive test

    @Test
    fun `onset date needed when AC1 true, AC2 true due to previous index case no self assessment, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3, simulating having received a positive indicative and then a negative
        val negativeTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.NEGATIVE,
            isConfirmed = true
        )
        val currentState = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(LocalDate.now(fixedClock).minusDays(2)),
                testResult = negativeTestResult,
                expiryDate = negativeTestResult.testEndDate
            )
        ).asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to previous index case with self assessment and negative test, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val negativeTestResult = acknowledgedTestResult(result = RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        val currentState = isolationHelper.selfAssessment(testResult = negativeTestResult).copy(
            expiryDate = negativeTestResult.testEndDate
        ).asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 true, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.positiveTest(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = true,
                testEndDate = LocalDate.now(fixedClock).minusDays(12)
            )
        ).asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 false, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment().asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 false, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment(
            expired = true,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = true,
                testEndDate = LocalDate.now(fixedClock).minusDays(12)
            )
        ).asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false due to not LAB_RESULT test, AC2 true, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = RAPID_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.contactCase().asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false due to negative test, AC2 true, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = NEGATIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.positiveTest(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = true,
                testEndDate = LocalDate.now(fixedClock).minusDays(12)
            )
        ).asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false due to requires confirmatory test, AC2 true, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = true
        )

        // AC2 + AC3
        val currentState = isolationHelper.neverInIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false, AC2 true, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = VOID,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = true
        )

        // AC2 + AC3
        val currentState = isolationHelper.neverInIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false, AC2 false, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = true
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment().asIsolation().asLogical()
        every { isolationStateMachine.readLogicalState() } returns currentState

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    private fun receivedTestResult(
        testResult: VirologyTestResult,
        testKit: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean
    ) =
        ReceivedTestResult(
            diagnosisKeySubmissionToken = "submissionToken",
            testEndDate = Instant.now(fixedClock),
            testResult = testResult,
            testKitType = testKit,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = requiresConfirmatoryTest
        )

    private fun acknowledgedTestResult(
        result: RelevantVirologyTestResult,
        isConfirmed: Boolean,
        testEndDate: LocalDate = LocalDate.now(fixedClock)
    ): AcknowledgedTestResult =
        AcknowledgedTestResult(
            testEndDate = testEndDate,
            testResult = result,
            acknowledgedDate = testEndDate,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = !isConfirmed,
            confirmedDate = null
        )
}

package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.addTestResult
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationStateMachineSetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertTrue

class LinkTestResultOnsetDateNeededCheckerTest : IsolationStateMachineSetupHelper {

    override val isolationStateMachine = mockk<IsolationStateMachine>()
    override val clock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)!!
    private val isolationHelper = IsolationHelper(clock)

    private val testSubject = LinkTestResultOnsetDateNeededChecker(
        isolationStateMachine,
        clock
    )

    // AC1 - test is positive and does not require confirmation
    // AC2 - not currently in active index isolation
    // AC3 - not in active index isolation at time of test

    @Test
    fun `onset date needed when AC1 true, AC2 true due to no memory of isolation, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.neverInIsolation()
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to no current index case, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.contact().asIsolation()
        givenIsolationState(currentState)

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
        val currentState = isolationHelper.contact(expired = true).asIsolation()
        givenIsolationState(currentState)

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
        val currentState = isolationHelper.selfAssessment()
            .asIsolation()
            .addTestResult(negativeTestResult)
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true due to assisted LFD , AC2 true, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = RAPID_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.contact().asIsolation()
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true due to unassisted LFD, AC2 true, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = RAPID_SELF_REPORTED,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.contact().asIsolation()
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true, AC3 true, but old test result is present`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = true,
            testEndDate = LocalDate.now(clock).minusDays(12)
        ).asIsolation()
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 false due to active index isolation with test, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = true,
            testEndDate = LocalDate.now(clock).minusDays(10)
        ).asIsolation()
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 false due to active symptoms isolation, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment().asIsolation()
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true but old symptoms and test present, AC3 true`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment(
            expired = true,
        ).asIsolation().addTestResult(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = true,
                testEndDate = LocalDate.now(clock).minusDays(12)
            )
        )
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 false due to active isolation with symptoms and test, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment(
            onsetDate = LocalDate.now(clock).minusDays(10),
        ).asIsolation().addTestResult(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = true,
                testEndDate = LocalDate.now(clock).minusDays(10)
            )
        )
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 true, AC3 false due to new test inside old isolation`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false,
            testEndDate = LocalDate.now(clock).minusDays(2)
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment(
            onsetDate = LocalDate.now(clock).minusDays(12),
        ).asIsolation().addTestResult(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = true,
                testEndDate = LocalDate.now(clock).minusDays(12)
            )
        )
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false due to negative test, AC2 true, AC3 true but old test present`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = NEGATIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2 + AC3
        val currentState = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = true,
            testEndDate = LocalDate.now(clock).minusDays(12)
        ).asIsolation()
        givenIsolationState(currentState)

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
        val currentState = isolationHelper.neverInIsolation()
        givenIsolationState(currentState)

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
        val currentState = isolationHelper.neverInIsolation()
        givenIsolationState(currentState)

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false, AC2 false due to active symptoms isolation, AC3 false`() {
        // AC1
        val testResult = receivedTestResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = true
        )

        // AC2 + AC3
        val currentState = isolationHelper.selfAssessment().asIsolation()
        givenIsolationState(currentState)

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
            testEndDate = Instant.now(clock),
            testResult = testResult,
            testKitType = testKit,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = requiresConfirmatoryTest
        )

    private fun receivedTestResult(
        testResult: VirologyTestResult,
        testKit: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean,
        testEndDate: LocalDate
    ) =
        ReceivedTestResult(
            diagnosisKeySubmissionToken = "submissionToken",
            testEndDate = testEndDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            testResult = testResult,
            testKitType = testKit,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = requiresConfirmatoryTest
        )

    private fun acknowledgedTestResult(
        result: RelevantVirologyTestResult,
        isConfirmed: Boolean,
        testEndDate: LocalDate = LocalDate.now(clock)
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

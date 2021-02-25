package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertTrue

class LinkTestResultOnsetDateNeededCheckerTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>()

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = LinkTestResultOnsetDateNeededChecker(
        isolationStateMachine,
        relevantTestResultProvider
    )

    // AC1 - test is LAB_RESULT, positive and does not require confirmation
    // AC2 - not considered symptomatic
    // AC3 - must not have a previous positive test result

    @Before
    fun setUp() {
        mockkStatic("uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachineKt")
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to no current index case, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = isolationStateContactCaseOnly
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to no previous index case, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = Default(previousIsolation = isolationStateContactCaseOnly)
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to index case no self assessment, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = isolationStateIndexCaseOnlyWithoutSelfAssessment
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to previous index case no self assessment, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = Default(previousIsolation = isolationStateIndexCaseOnlyWithoutSelfAssessment)
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to index case with self assessment and negative test, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = isolationStateIndexCaseOnlyWithSelfAssessment
        every { isolationStateMachine.readState() } returns currentState
        every { relevantTestResultProvider.isTestResultNegative() } returns true

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date needed when AC1 true, AC2 true due to previous index case with self assessment and negative test, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = Default(previousIsolation = isolationStateIndexCaseOnlyWithSelfAssessment)
        every { isolationStateMachine.readState() } returns currentState
        every { relevantTestResultProvider.isTestResultNegative() } returns true

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertTrue(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 true, AC3 false`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = Default()
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns true

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 false, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = isolationStateIndexCaseOnlyWithSelfAssessment
        every { isolationStateMachine.readState() } returns currentState
        every { relevantTestResultProvider.isTestResultNegative() } returns false

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 true, AC2 false, AC3 false`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = Default(previousIsolation = isolationStateIndexCaseOnlyWithSelfAssessment)
        every { isolationStateMachine.readState() } returns currentState
        every { relevantTestResultProvider.isTestResultNegative() } returns false

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns true

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false due to not LAB_RESULT test, AC2 true, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = RAPID_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = isolationStateContactCaseOnly
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false due to negative test, AC2 true, AC3 false`() {
        // AC1
        val testResult = setResult(
            testResult = NEGATIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        // AC2
        val currentState = Default()
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns true

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false due to requires confirmatory test, AC2 true, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = true
        )

        // AC2
        val currentState = Default()
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false, AC2 true, AC3 false`() {
        // AC1
        val testResult = setResult(
            testResult = VOID,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = true
        )

        // AC2
        val currentState = Default()
        every { isolationStateMachine.readState() } returns currentState

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    @Test
    fun `onset date not needed when AC1 false, AC2 false, AC3 true`() {
        // AC1
        val testResult = setResult(
            testResult = POSITIVE,
            testKit = LAB_RESULT,
            requiresConfirmatoryTest = true
        )

        // AC2
        val currentState = isolationStateIndexCaseOnlyWithSelfAssessment
        every { isolationStateMachine.readState() } returns currentState
        every { relevantTestResultProvider.isTestResultNegative() } returns false

        // AC3
        every { relevantTestResultProvider.isTestResultPositive() } returns false

        val actual = testSubject.isInterestedInAskingForSymptomsOnsetDay(testResult)

        assertFalse(actual)
    }

    private fun setResult(
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

    private val symptomsOnsetDate = LocalDate.parse("2020-07-20")

    private val isolationStateContactCaseOnly = Isolation(
        isolationStart = Instant.now(fixedClock),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = Instant.now(fixedClock),
            notificationDate = Instant.now(fixedClock),
            expiryDate = LocalDate.now(fixedClock).plus(11, DAYS)
        )
    )

    private val isolationStateIndexCaseOnlyWithSelfAssessment = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(
            symptomsOnsetDate = LocalDate.now(fixedClock),
            expiryDate = symptomsOnsetDate.plus(7, DAYS),
            selfAssessment = true
        )
    )

    private val isolationStateIndexCaseOnlyWithoutSelfAssessment = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(
            symptomsOnsetDate = LocalDate.now(fixedClock),
            expiryDate = symptomsOnsetDate.plus(7, DAYS),
            selfAssessment = false
        )
    )
}

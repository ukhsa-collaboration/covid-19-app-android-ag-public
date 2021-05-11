package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import uk.nhs.nhsx.covid19.android.app.testordering.toRelevantVirologyTestResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestResultIsolationHandlerTest {

    private val calculateKeySubmissionDateRange = mockk<CalculateKeySubmissionDateRange>(relaxUnitFun = true)
    private val createSelfAssessmentIndexCase = mockk<CreateSelfAssessmentIndexCase>()
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)

    private val testSubject = TestResultIsolationHandler(
        calculateKeySubmissionDateRange,
        createSelfAssessmentIndexCase,
        fixedClock
    )

    private val positiveTestResultIndicative = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = RAPID_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = true
    )

    private val positiveTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    private val negativeTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = NEGATIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    private val voidTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = VOID,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    private val isolationConfiguration = DurationDays()

    @Before
    fun setUp() {
        val submissionDateRange = mockk<SubmissionDateRange>()
        every { calculateKeySubmissionDateRange(any(), any()) } returns submissionDateRange
        every { submissionDateRange.containsAtLeastOneDay() } returns true
    }

    // --- Positive, arriving in order

    @Test
    fun `when in isolation as index case with self-assessment, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            isolationSelfAssessment().asLogical(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case without self-assessment, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            isolationPositiveTest(positiveTestResultConfirmed.toAcknowledgedTestResult()).asLogical(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index and contact case, positive indicative test result is ignored`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with previous positive confirmed test result from current isolation, positive confirmed test result is ignored`() {
        val state = isolationPositiveTest(
            acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)
        ).asLogical()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(DoNotTransition(preventKeySubmission = false, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case with self-assessment, positive confirmed test result is stored`() {
        val state = isolationSelfAssessment()
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResultToIndexCase(
            testResult = positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index and contact case, positive confirmed test result removes contact case`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state
            .copy(contactCase = null)
            .addTestResultToIndexCase(positiveTestResultConfirmed.toAcknowledgedTestResult())
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as contact case, positive indicative test result adds index case to isolation`() {
        val testResult = positiveTestResultIndicative.copy(testEndDate = Instant.parse("2020-08-02T12:00:00Z"))

        val state = isolationContactCase()
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(testEndDate = LocalDate.parse("2020-08-02")),
                testResult = testResult.toAcknowledgedTestResult(),
                expiryDate = LocalDate.parse("2020-08-13")
            )
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case, positive confirmed test result adds index case to isolation and removes contact case`() {
        val state = isolationContactCase()
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case, with relevant positive indicative, positive confirmed confirms existing indicative test result`() {
        val relevantTestResult = acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)
        val state = isolationPositiveTest(relevantTestResult)

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResultToIndexCase(
            testResult = relevantTestResult.copy(confirmedDate = positiveTestResultConfirmed.testEndDay(fixedClock))
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, positive indicative test result triggers isolation`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            neverIsolating().asLogical(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = positiveTestResultIndicative.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
        val transition = result as Transition
        assertTrue(transition.newState.asLogical().isActiveIsolation(fixedClock))
    }

    @Test
    fun `when not in isolation, with expired index case isolation, positive indicative test result triggers isolation`() {
        val state = isolationSelfAssessment(
            selfAssessmentDate = indexCaseStartDate.minus(13, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultIndicative.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, without previous test result, positive indicative test result triggers isolation`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            neverIsolating().asLogical(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultIndicative.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with relevant negative, positive confirmed test result triggers isolation`() {
        val state = neverIsolatingWithNegativeTest(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired index case, with relevant negative, positive confirmed test result triggers isolation`() {
        val state = isolationSelfAssessment(
            selfAssessmentDate = indexCaseStartDate.minus(13, DAYS)
        ).addTestResultToIndexCase(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, with relevant negative, positive confirmed test result triggers isolation`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(encounterDate = encounterDate.minus(13, DAYS)),
            indexInfo = negativeTest(acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true))
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, with relevant negative, positive confirmed test result triggers isolation, test result with explicit onset date`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(encounterDate = encounterDate.minus(13, DAYS)),
            indexInfo = negativeTest(acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true))
        )
        val testResult = positiveTestResultConfirmed.copy(
            symptomsOnsetDate = SymptomsDate(
                explicitDate = LocalDate.parse("2020-08-01")
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = LocalDate.now(fixedClock),
                    onsetDate = LocalDate.parse("2020-08-01")
                ),
                testResult = testResult.toAcknowledgedTestResult(),
                expiryDate = LocalDate.parse("2020-08-12")
            )
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, with relevant negative, positive confirmed test result triggers isolation, test result with cannot remember onset date`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(encounterDate = encounterDate.minus(13, DAYS)),
            indexInfo = negativeTest(acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true))
        )
        val testResult = positiveTestResultConfirmed.copy(
            symptomsOnsetDate = SymptomsDate(
                explicitDate = null
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = LocalDate.now(fixedClock)
                ),
                testResult = testResult.toAcknowledgedTestResult(),
                expiryDate = LocalDate.parse("2020-08-05")
            )
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired index case, without relevant negative, positive confirmed test result is stored`() {
        val state = isolationSelfAssessment(selfAssessmentDate = indexCaseStartDate.minus(13, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResultToIndexCase(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, without relevant negative, positive confirmed test result triggers isolation`() {
        val state = isolationContactCase(
            encounterDate = encounterDate.minus(13, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = positiveTestResultConfirmed.testKitType,
            requiresConfirmatoryTest = positiveTestResultConfirmed.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    // --- Positive, arriving out of order
    // -- Tests that are "way too old", i.e., would expire before the start of an existing (active or expired) isolation

    @Test
    fun `when in isolation as a contact case, positive confirmed test result that is way too old is ignored`() {
        `when has contact case, positive test result that is way too old is ignored`(
            isolationActive = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as a contact case, positive indicative test result that is way too old is ignored`() {
        `when has contact case, positive test result that is way too old is ignored`(
            isolationActive = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with expired contact case, positive confirmed test result that is way too old is ignored`() {
        `when has contact case, positive test result that is way too old is ignored`(
            isolationActive = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with expired contact case, positive indicative test result that is way too old is ignored`() {
        `when has contact case, positive test result that is way too old is ignored`(
            isolationActive = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has contact case, positive test result that is way too old is ignored`(
        isolationActive: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val state = isolationContactCase(
            encounterDate =
                if (isolationActive) encounterDate
                else encounterDate.minus(13, DAYS)
        )

        val receivedTestResult = positiveTestResult(receivedTestConfirmed)
            .copy(testEndDate = state.contactCase!!.startDate.minus(11, DAYS).toInstant())

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = true, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as an index case with self-assessment, positive confirmed test result that is way too old is ignored`() {
        `when has index case with self-assessment, positive test result that is way too old is ignored`(
            isolationActive = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as an index case with self-assessment, positive indicative test result that is way too old is ignored`() {
        `when has index case with self-assessment, positive test result that is way too old is ignored`(
            isolationActive = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, positive confirmed test result that is way too old is ignored`() {
        `when has index case with self-assessment, positive test result that is way too old is ignored`(
            isolationActive = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, positive indicative test result that is way too old is ignored`() {
        `when has index case with self-assessment, positive test result that is way too old is ignored`(
            isolationActive = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has index case with self-assessment, positive test result that is way too old is ignored`(
        isolationActive: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val state = isolationSelfAssessment(
            selfAssessmentDate =
                if (isolationActive) indexCaseStartDate
                else indexCaseStartDate.minus(13, DAYS)
        )

        val receivedTestResult = positiveTestResult(receivedTestConfirmed)
            .copy(testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(11, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )
        val expectedKeySharingInfo =
            if (receivedTestResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock),
                    testKitType = receivedTestResult.testKitType,
                    requiresConfirmatoryTest = receivedTestResult.requiresConfirmatoryTest
                )
            else null
        assertEquals(DoNotTransition(preventKeySubmission = true, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive confirmed, positive confirmed test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive indicative, positive confirmed test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive confirmed, positive indicative test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive indicative, positive indicative test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with expired index case without self-assessment, with relevant positive confirmed, positive confirmed test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with expired index case without self-assessment, with relevant positive indicative, positive confirmed test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with expired index case without self-assessment, with relevant positive confirmed, positive indicative test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with expired index case without self-assessment, with relevant positive indicative, positive indicative test result that is way too old is ignored`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has index case without self-assessment, with relevant positive, positive test result that is way too old is ignored`(
        isolationActive: Boolean,
        relevantTestConfirmed: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)

        val state = isolationPositiveTest(
            testResult = acknowledgedTestResult(
                RelevantVirologyTestResult.POSITIVE,
                relevantTestConfirmed,
                relevantTestDate
            )
        )

        val receivedTestResult = positiveTestResult(receivedTestConfirmed)
            .copy(testEndDate = relevantTestDate.minus(11, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )
        val expectedKeySharingInfo =
            if (receivedTestResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock),
                    testKitType = receivedTestResult.testKitType,
                    requiresConfirmatoryTest = receivedTestResult.requiresConfirmatoryTest
                )
            else null
        assertEquals(DoNotTransition(preventKeySubmission = true, expectedKeySharingInfo), result)
    }

    // -- Positive tests that are older than symptoms
    // - Index case only with self-assessment, without relevant, just replace index case

    @Test
    fun `when in isolation as index case with self-assessment, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
            isolationActive = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
            isolationActive = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
        isolationActive: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val state = isolationSelfAssessment(
            selfAssessmentDate =
                if (isolationActive) indexCaseStartDate
                else indexCaseStartDate.minus(13, DAYS)
        )

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock),
                    testKitType = testResult.testKitType,
                    requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
                )
            else null
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    // - Index case only with self-assessment, with relevant positive, replace index case and possibly confirm

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive confirmed, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive indicative, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive indicative, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    private fun `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
        isolationActive: Boolean,
        relevantTestConfirmed: Boolean,
        receivedTestConfirmed: Boolean,
        shouldConfirm: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = relevantTestDate.minus(4, DAYS).toLocalDate(),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = relevantTestDate
            )
        )

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult =
            if (shouldConfirm) testResult.toAcknowledgedTestResult(confirmedDate = relevantTestDate.toLocalDate())
            else testResult.toAcknowledgedTestResult()

        val expectedState = isolationPositiveTest(
            testResult = expectedTestResult
        )
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock),
                    testKitType = testResult.testKitType,
                    requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
                )
            else null
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    // -- Positive tests that are older than a previous positive
    // - Index case only without self-assessment, with relevant positive newer than received, replace index case and possibly confirm

    @Test
    fun `when in isolation as index case without self-assessment, with relevant positive confirmed, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case without self-assessment, with relevant positive confirmed, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `when in isolation as index case without self-assessment, with relevant positive indicative, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case without self-assessment, with relevant positive indicative, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case without self-assessment, with relevant positive confirmed, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case without self-assessment, with relevant positive confirmed, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `not in isolation, with expired index case without self-assessment, with relevant positive indicative, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case without self-assessment, with relevant positive indicative, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    private fun `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
        isolationActive: Boolean,
        relevantTestConfirmed: Boolean,
        receivedTestConfirmed: Boolean,
        shouldConfirm: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)

        val state = isolationPositiveTest(
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = relevantTestDate
            )
        )

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult =
            if (shouldConfirm) testResult.toAcknowledgedTestResult(confirmedDate = relevantTestDate.toLocalDate())
            else testResult.toAcknowledgedTestResult()

        val expectedState = isolationPositiveTest(
            testResult = expectedTestResult
        )
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock),
                    testKitType = testResult.testKitType,
                    requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
                )
            else null
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    // - Index case only with self-assessment, with relevant positive newer than received, do not transition, possibly confirm

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive indicative test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive confirmed test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive indicative test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive confirmed, positive indicative test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive indicative, positive confirmed test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive indicative, positive indicative test result older than relevant test result is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    private fun `when has index case with self-assessment, with relevant positive, positive test result older than relevant is stored`(
        isolationActive: Boolean,
        relevantTestConfirmed: Boolean,
        receivedTestConfirmed: Boolean,
        shouldConfirm: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = relevantTestDate.minus(2, DAYS).toLocalDate(),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = relevantTestDate
            )
        )

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult =
            if (shouldConfirm) testResult.toAcknowledgedTestResult(confirmedDate = relevantTestDate.toLocalDate())
            else testResult.toAcknowledgedTestResult()

        val expectedState = state.addTestResultToIndexCase(expectedTestResult)
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock),
                    testKitType = testResult.testKitType,
                    requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
                )
            else null
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    // -- Positive confirmed tests that are older than a previous negative

    @Test
    fun `when not in isolation, with expired index case with self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation is stored`() {
        val relevantTestDate = testEndDate.minus(4, DAYS)

        val selfAssessmentDate = relevantTestDate.minus(2, DAYS).toLocalDate()
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = selfAssessmentDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true,
                testEndDate = relevantTestDate
            ),
            testExpiresIndexCase = true
        )

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        every {
            createSelfAssessmentIndexCase(
                state.asLogical(),
                (state.indexInfo as IndexCase).isolationTrigger as SelfAssessment
            )
        } returns selfAssessment(selfAssessmentDate)

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationSelfAssessmentAndTest(
            selfAssessmentDate = selfAssessmentDate,
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired index case without self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces index case`() {
        val relevantTestDate = testEndDate.minus(5, DAYS)
        val positiveTestEndDate = relevantTestDate.minus(4, DAYS)

        // This simulates the case where we first receive a positive indicative and then a negative test
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(positiveTestEndDate.toLocalDate()),
                testResult = acknowledgedTestResult(
                    result = RelevantVirologyTestResult.NEGATIVE,
                    isConfirmed = true,
                    testEndDate = relevantTestDate
                ),
                expiryDate = relevantTestDate.toLocalDate()
            )
        )

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    // -- Positive indicative tests that are older than a previous negative

    @Test
    fun `when not in isolation, with expired index case without self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`() {
        val relevantTestDate = testEndDate.minus(5, DAYS)
        val positiveTestEndDate = relevantTestDate.minus(4, DAYS)

        // This simulates the case where we first receive a positive indicative and then a negative test
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(positiveTestEndDate.toLocalDate()),
                testResult = acknowledgedTestResult(
                    result = RelevantVirologyTestResult.NEGATIVE,
                    isConfirmed = true,
                    testEndDate = relevantTestDate
                ),
                expiryDate = relevantTestDate.toLocalDate()
            )
        )

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = true, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`() {
        val relevantTestDate = testEndDate.minus(4, DAYS)

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = relevantTestDate.minus(2, DAYS).toLocalDate(),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true,
                testEndDate = relevantTestDate
            ),
            testExpiresIndexCase = true
        )

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = true, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces isolation`() {
        `when has contact case, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces isolation`(
            isolationActive = true
        )
    }

    @Test
    fun `when not in isolation, with contact case, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces isolation`() {
        `when has contact case, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces isolation`(
            isolationActive = false
        )
    }

    private fun `when has contact case, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces isolation`(
        isolationActive: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)

        val state = isolationContactCase(
            encounterDate = relevantTestDate.minus(2, DAYS).toLocalDate()
        ).copy(
            indexInfo = NegativeTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.NEGATIVE,
                    isConfirmed = true,
                    testEndDate = relevantTestDate
                )
            )
        )

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`() {
        `when has contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`(
            isolationActive = true
        )
    }

    @Test
    fun `when not in isolation, with contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`() {
        `when has contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`(
            isolationActive = false
        )
    }

    private fun `when has contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`(
        isolationActive: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)

        val state = isolationContactCase(
            encounterDate = relevantTestDate.minus(2, DAYS).toLocalDate()
        ).copy(
            indexInfo = NegativeTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.NEGATIVE,
                    isConfirmed = true,
                    testEndDate = relevantTestDate
                )
            )
        )

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = true, keySharingInfo = null), result)
    }

    // -- No memory of previous isolation

    @Test
    fun `when not in isolation, expired positive confirmed test result stores expired index isolation`() {
        `when not in isolation, expired positive test result stores expired index isolation`(
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, expired positive indicative test result stores expired index isolation`() {
        `when not in isolation, expired positive test result stores expired index isolation`(
            receivedTestConfirmed = false
        )
    }

    private fun `when not in isolation, expired positive test result stores expired index isolation`(receivedTestConfirmed: Boolean) {
        val state = neverIsolating()

        val receivedTestResult = positiveTestResult(confirmed = receivedTestConfirmed)
            .copy(testEndDate = now.minus(20, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(receivedTestResult.toAcknowledgedTestResult())
        val expectedKeySharingInfo =
            if (receivedTestResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock),
                    testKitType = receivedTestResult.testKitType,
                    requiresConfirmatoryTest = receivedTestResult.requiresConfirmatoryTest
                )
            else null
        assertEquals(Transition(expectedState, expectedKeySharingInfo), result)
        assertFalse((result as Transition).newState.asLogical().isActiveIsolation(fixedClock))
    }

    // --- Negative, arriving in order

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, new negative confirmed test result replaces index case`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = positiveTest(
                testResult = acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = false
                )
            )
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = Instant.now(fixedClock).plus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            indexInfo = negativeTest(testResult = testResult.toAcknowledgedTestResult())
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, new negative confirmed test result replaces index case`() {
        val state = isolationPositiveTest(
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            indexInfo = negativeTest(testResult = negativeTestResultConfirmed.toAcknowledgedTestResult())
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case, with expired index case, with relevant positive unconfirmed, new negative confirmed test result replaces index case`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = positiveTest(
                testResult = acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = false,
                    testEndDate = testEndDate.minus(12, DAYS)
                )
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            indexInfo = negativeTest(testResult = negativeTestResultConfirmed.toAcknowledgedTestResult())
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation, with relevant positive confirmed, new negative confirmed test result is ignored`() {
        val state = isolationPositiveTest(
            acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, without relevant positive confirmed, new negative confirmed test result expires index case`() {
        val state = isolationSelfAssessment()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            indexInfo = (state.indexInfo as IndexCase).copy(
                testResult = negativeTestResultConfirmed.toAcknowledgedTestResult(),
                expiryDate = LocalDateTime.ofInstant(negativeTestResultConfirmed.testEndDate, fixedClock.zone).toLocalDate()
            )
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact and index case, without relevant positive confirmed, new negative confirmed test result expires index case`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            indexInfo = (state.indexInfo as IndexCase).copy(
                testResult = negativeTestResultConfirmed.toAcknowledgedTestResult(),
                expiryDate = negativeTestResultConfirmed.testEndDate.toLocalDate()
            )
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case only, without relevant positive confirmed, new negative confirmed test result is stored`() {
        val state = isolationContactCase()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            indexInfo = negativeTest(negativeTestResultConfirmed.toAcknowledgedTestResult())
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant negative, new negative confirmed test result is ignored`() {
        val state = isolationSelfAssessmentAndTest(
            testResult = acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired index case, new negative confirmed test result is stored`() {
        val state = isolationSelfAssessment(
            selfAssessmentDate = indexCaseStartDate.minus(13, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResultToIndexCase(
            negativeTestResultConfirmed.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired index case, with relevant negative, negative confirmed test result is ignored`() {
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = indexCaseStartDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true
            ),
            testExpiresIndexCase = true
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    // --- Negative, arriving out of order
    // -- Negative older than relevant test

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, negative confirmed test result older than relevant test is ignored`() {
        val relevantTestDate = Instant.now(fixedClock).plus(2, DAYS)

        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = positiveTest(
                testResult = acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = false,
                    testEndDate = relevantTestDate
                )
            )
        )

        val testResult = negativeTestResultConfirmed.copy(testEndDate = relevantTestDate.minus(1, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, negative confirmed test result older than relevant test is ignored`() {
        val relevantTestDate = Instant.now(fixedClock).plus(2, DAYS)

        val state = isolationPositiveTest(
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false,
                testEndDate = relevantTestDate
            )
        )

        val testResult = negativeTestResultConfirmed.copy(testEndDate = relevantTestDate.minus(1, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    // -- Negative older than symptoms

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, negative confirmed test result older than symptoms onset is ignored`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = selfAssessmentAndTest(
                testResult = acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = false
                )
            )
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, negative confirmed test result older than symptoms onset date is ignored`() {
        val state = isolationSelfAssessmentAndTest(
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false
            )
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, without relevant positive confirmed, negative confirmed test result older than symptoms onset date is ignored`() {
        val state = isolationSelfAssessment()

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact and index case, without relevant positive confirmed, negative confirmed test result older than symptoms onset date is ignored`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = selfAssessment()
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired index case, negative confirmed test result older than symptoms onset date is ignored`() {
        val state = isolationSelfAssessment(
            selfAssessmentDate = indexCaseStartDate.minus(13, DAYS)
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    // --- Void

    @Test
    fun `when in isolation as index case only, void confirmed test result is ignored`() {
        val state = isolationSelfAssessment()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case only, void confirmed test result is ignored`() {
        val state = isolationContactCase()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact and index case, void confirmed test result is ignored`() {
        val state = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(),
            indexInfo = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, void confirmed test result is ignored`() {
        val state = neverIsolating()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state.asLogical(),
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    private fun positiveTestResult(confirmed: Boolean): ReceivedTestResult =
        if (confirmed) positiveTestResultConfirmed
        else positiveTestResultIndicative

    private fun acknowledgedTestResult(
        result: RelevantVirologyTestResult,
        isConfirmed: Boolean,
        testEndDate: Instant = Companion.testEndDate
    ): AcknowledgedTestResult =
        AcknowledgedTestResult(
            testEndDate = testEndDate.toLocalDate(),
            testResult = result,
            acknowledgedDate = testEndDate.toLocalDate(),
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = !isConfirmed,
            confirmedDate = null
        )

    private fun IsolationState.selfAssessmentSymptomsOnsetInstant(): Instant =
        ((indexInfo as IndexCase).isolationTrigger as SelfAssessment)
            .assumedOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC)

    private fun neverIsolating(): IsolationState =
        IsolationState(isolationConfiguration = DurationDays())

    private fun neverIsolatingWithNegativeTest(testResult: AcknowledgedTestResult): IsolationState =
        IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = negativeTest(testResult)
        )

    private fun negativeTest(testResult: AcknowledgedTestResult): NegativeTest {
        if (testResult.testResult != RelevantVirologyTestResult.NEGATIVE) {
            throw IllegalArgumentException("This function can only be called with a negative test result")
        }
        return NegativeTest(testResult)
    }

    private fun isolationSelfAssessment(
        selfAssessmentDate: LocalDate = indexCaseStartDate,
    ): IsolationState =
        IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = selfAssessment(selfAssessmentDate)
        )

    private fun selfAssessment(
        selfAssessmentDate: LocalDate = indexCaseStartDate,
    ): IndexCase =
        IndexCase(
            isolationTrigger = SelfAssessment(selfAssessmentDate),
            expiryDate = selfAssessmentDate.plusDays(9)
        )

    private fun isolationPositiveTest(
        testResult: AcknowledgedTestResult,
    ): IsolationState =
        IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = positiveTest(testResult)
        )

    private fun positiveTest(
        testResult: AcknowledgedTestResult,
    ): IndexCase =
        IndexCase(
            isolationTrigger = PositiveTestResult(testResult.testEndDate),
            testResult = testResult,
            expiryDate = testResult.testEndDate.plusDays(11)
        )

    private fun selfAssessmentAndTest(
        selfAssessmentDate: LocalDate = indexCaseStartDate,
        testResult: AcknowledgedTestResult,
        testExpiresIndexCase: Boolean = false
    ): IndexCase {
        val indexCase = selfAssessment(selfAssessmentDate).addTestResult(testResult)
        return if (testExpiresIndexCase) indexCase.copy(expiryDate = testResult.testEndDate)
        else indexCase
    }

    private fun isolationSelfAssessmentAndTest(
        selfAssessmentDate: LocalDate = indexCaseStartDate,
        testResult: AcknowledgedTestResult,
        testExpiresIndexCase: Boolean = false
    ): IsolationState =
        IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = selfAssessmentAndTest(selfAssessmentDate, testResult, testExpiresIndexCase)
        )

    private fun isolationContactCase(
        encounterDate: LocalDate = Companion.encounterDate
    ): IsolationState =
        IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = contactCase(encounterDate)
        )

    private fun contactCase(
        encounterDate: LocalDate = Companion.encounterDate
    ): ContactCase =
        ContactCase(
            exposureDate = encounterDate,
            notificationDate = encounterDate,
            expiryDate = encounterDate.plus(11, DAYS)
        )

    private fun ReceivedTestResult.toAcknowledgedTestResult(
        confirmedDate: LocalDate? = null
    ): AcknowledgedTestResult {
        val result = testResult.toRelevantVirologyTestResult()
        if (result == null) {
            throw IllegalArgumentException("This function cannot be called with a $result test result")
        }
        return AcknowledgedTestResult(
            testEndDay(fixedClock),
            result,
            testKitType,
            acknowledgedDate = LocalDate.now(fixedClock),
            requiresConfirmatoryTest,
            confirmedDate
        )
    }

    private fun LocalDate.toInstant(): Instant =
        atStartOfDay(fixedClock.zone).toInstant()

    private fun Instant.toLocalDate(): LocalDate =
        LocalDateTime.ofInstant(this, fixedClock.zone).toLocalDate()

    companion object {
        val now: Instant = Instant.parse("2020-07-26T12:00:00Z")!!
        val testEndDate: Instant = Instant.parse("2020-07-25T12:00:00Z")!!
        val indexCaseStartDate: LocalDate = LocalDate.parse("2020-07-20")!!
        val encounterDate: LocalDate = LocalDateTime.ofInstant(now.minus(3, DAYS), ZoneOffset.UTC).toLocalDate()
    }
}

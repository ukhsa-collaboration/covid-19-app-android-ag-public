package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.isolation.createIsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import uk.nhs.nhsx.covid19.android.app.testordering.toRelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals

class TestResultIsolationHandlerTest {

    private val calculateKeySubmissionDateRange = mockk<CalculateKeySubmissionDateRange>(relaxUnitFun = true)

    private val now: Instant = Instant.parse("2020-07-26T12:00:00Z")!!
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)
    private val testEndDate: Instant = Instant.parse("2020-07-25T12:00:00Z")!!
    private val indexCaseStartDate: LocalDate = LocalDate.parse("2020-07-20")!!
    private val encounterDate: LocalDate = now.minus(3, DAYS).toLocalDate(fixedClock.zone)

    private val testSubject = TestResultIsolationHandler(
        calculateKeySubmissionDateRange,
        WouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(
            CalculateIndexExpiryDate(fixedClock),
            fixedClock
        ),
        createIsolationLogicalState(fixedClock),
        fixedClock
    )

    private val positiveTestResultIndicative = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = RAPID_RESULT,
        diagnosisKeySubmissionSupported = false,
        requiresConfirmatoryTest = true,
        confirmatoryDayLimit = 2,
        shouldOfferFollowUpTest = true
    )

    private val positiveTestResultIndicativeWithKeySharingSupported = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = RAPID_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = true,
        confirmatoryDayLimit = 2,
        shouldOfferFollowUpTest = true
    )

    private val positiveTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false,
        shouldOfferFollowUpTest = false
    )

    private val negativeTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = NEGATIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = false,
        requiresConfirmatoryTest = false,
        shouldOfferFollowUpTest = false
    )

    private val voidTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = VOID,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = false,
        requiresConfirmatoryTest = false,
        shouldOfferFollowUpTest = false
    )

    private val plodTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = PLOD,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = false,
        requiresConfirmatoryTest = false,
        shouldOfferFollowUpTest = false
    )

    private val isolationConfiguration = IsolationConfiguration()

    @Before
    fun setUp() {
        val submissionDateRange = mockk<SubmissionDateRange>()
        every { calculateKeySubmissionDateRange(any(), any()) } returns submissionDateRange
        every { submissionDateRange.containsAtLeastOneDay() } returns true
    }

    //region --- Positive, arriving in order

    @Test
    fun `when in isolation as index case with self-assessment, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            isolationSelfAssessment(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case without self-assessment, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            isolationPositiveTest(positiveTestResultConfirmed.toAcknowledgedTestResult()),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index and contact case, positive indicative test result is ignored`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            selfAssessment = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with previous positive confirmed test result from current isolation, positive confirmed test result is ignored`() {
        val state = isolationPositiveTest(
            acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(DoNotTransition(preventKeySubmission = false, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case with self-assessment, positive confirmed test result is stored`() {
        val state = isolationSelfAssessment()
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index and contact case, positive confirmed test result removes contact case`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            selfAssessment = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state
            .copy(contact = null)
            .addTestResult(positiveTestResultConfirmed.toAcknowledgedTestResult())
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as contact case, positive indicative test result adds index case to isolation`() {
        val testResult = positiveTestResultIndicative.copy(testEndDate = Instant.parse("2020-08-02T12:00:00Z"))

        val state = isolationContactCase()
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            testResult = testResult.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case, positive confirmed test result adds index case to isolation and removes contact case`() {
        val state = isolationContactCase()
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case, with relevant positive indicative, positive confirmed confirms existing indicative test result`() {
        val relevantTestResult =
            acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)
        val state = isolationPositiveTest(relevantTestResult)

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val confirmedDate = positiveTestResultConfirmed.testEndDate(fixedClock)
        val expectedState = state.addTestResult(
            testResult = relevantTestResult.copy(confirmedDate = confirmedDate, confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED)
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, positive indicative test result triggers isolation`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            neverIsolating(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = positiveTestResultIndicative.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, positive indicative test result with key sharing supported triggers isolation and adds KeySharingInfo`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            neverIsolating(),
            positiveTestResultIndicativeWithKeySharingSupported,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = positiveTestResultIndicativeWithKeySharingSupported.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultIndicativeWithKeySharingSupported.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired index case isolation, positive indicative test result triggers isolation`() {
        val state = isolationSelfAssessment(
            selfAssessmentDate = indexCaseStartDate.minus(13, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultIndicative.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, without previous test result, positive indicative test result triggers isolation`() {
        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            neverIsolating(),
            positiveTestResultIndicative,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultIndicative.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
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
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired index case, with relevant negative, positive confirmed test result triggers isolation`() {
        val state = isolationSelfAssessment(
            selfAssessmentDate = indexCaseStartDate.minus(13, DAYS)
        ).addTestResult(
            acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, with relevant negative, positive confirmed test result triggers isolation`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(encounterDate = encounterDate.minus(13, DAYS)),
            testResult = acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, with relevant negative, positive confirmed test result triggers isolation, test result with explicit onset date`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(encounterDate = encounterDate.minus(13, DAYS)),
            testResult = acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        )
        val testResult = positiveTestResultConfirmed.copy(
            symptomsOnsetDate = SymptomsDate(
                explicitDate = LocalDate.parse("2020-08-01")
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                onsetDate = LocalDate.parse("2020-08-01")
            ),
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, with relevant negative, positive confirmed test result triggers isolation, test result with cannot remember onset date`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(encounterDate = encounterDate.minus(13, DAYS)),
            testResult = acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        )
        val testResult = positiveTestResultConfirmed.copy(
            symptomsOnsetDate = SymptomsDate(
                explicitDate = null
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired index case, without relevant negative, positive confirmed test result is stored`() {
        val state = isolationSelfAssessment(selfAssessmentDate = indexCaseStartDate.minus(13, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when not in isolation, with expired contact case, without relevant negative, positive confirmed test result triggers isolation`() {
        val state = isolationContactCase(
            encounterDate = encounterDate.minus(13, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            positiveTestResultConfirmed.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion

    //region --- Positive, arriving out of order
    //region -- Tests that are "way too old", i.e., would expire before the start of an existing (active or expired) isolation

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
            .copy(testEndDate = state.contact!!.notificationDate.minus(11, DAYS).toInstant())

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
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
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = true, keySharingInfo = null), result)
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
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = true, keySharingInfo = null), result)
    }

    //endregion

    //region -- Positive tests that are older than symptoms
    //region - Index case only with self-assessment, without relevant, store test result

    @Test
    fun `when in isolation as index case with self-assessment, positive confirmed test result older than symptoms is stored`() {
        `when has index case with self-assessment, positive test result older than symptoms is stored`(
            isolationActive = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, positive indicative test result older than symptoms is stored`() {
        `when has index case with self-assessment, positive test result older than symptoms is stored`(
            isolationActive = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, positive confirmed test result older than symptoms is stored`() {
        `when has index case with self-assessment, positive test result older than symptoms is stored`(
            isolationActive = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, positive indicative test result older than symptoms is stored`() {
        `when has index case with self-assessment, positive test result older than symptoms is stored`(
            isolationActive = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has index case with self-assessment, positive test result older than symptoms is stored`(
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
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock)
                )
            else null
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion

    //region - Index case only with self-assessment, with relevant positive, store test result and possibly confirm

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive indicative test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive confirmed test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive indicative test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive confirmed, positive indicative test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive indicative, positive confirmed test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with expired index case with self-assessment, with relevant positive indicative, positive indicative test result older than symptoms is stored`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    private fun `when has index case with self-assessment, with relevant positive, positive test result older than symptoms is stored`(
        isolationActive: Boolean,
        relevantTestConfirmed: Boolean,
        receivedTestConfirmed: Boolean,
        shouldConfirm: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = relevantTestDate.minus(4, DAYS).toLocalDate(fixedClock.zone),
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
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult =
            if (shouldConfirm)
                testResult.toAcknowledgedTestResult(
                    confirmedDate = relevantTestDate.toLocalDate(fixedClock.zone)
                )
            else testResult.toAcknowledgedTestResult()

        val expectedState = state.addTestResult(
            testResult = expectedTestResult
        )
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock)
                )
            else null
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion
    //endregion

    //region -- Positive tests that are older than a previous positive
    //region - Index case only without self-assessment, with relevant positive newer than received, replace index case and possibly confirm

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
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult =
            if (shouldConfirm)
                testResult.toAcknowledgedTestResult(
                    confirmedDate = relevantTestDate.toLocalDate(fixedClock.zone)
                )
            else testResult.toAcknowledgedTestResult()

        val expectedState = isolationPositiveTest(
            testResult = expectedTestResult
        )
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock)
                )
            else null
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion

    //region - Index case only with self-assessment, with relevant positive newer than received, do not transition, possibly confirm

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
            selfAssessmentDate = relevantTestDate.minus(2, DAYS).toLocalDate(fixedClock.zone),
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
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult =
            if (shouldConfirm)
                testResult.toAcknowledgedTestResult(
                    confirmedDate = relevantTestDate.toLocalDate(fixedClock.zone)
                )
            else testResult.toAcknowledgedTestResult()

        val expectedState = state.addTestResult(expectedTestResult)
        val expectedKeySharingInfo =
            if (testResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock)
                )
            else null
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion
    //endregion

    //region -- Positive confirmed tests that are older than a previous negative

    @Test
    fun `when not in isolation, with expired index case with self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation is stored`() {
        val relevantTestDate = testEndDate.minus(4, DAYS)

        val selfAssessmentDate = relevantTestDate.minus(2, DAYS).toLocalDate(fixedClock.zone)
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = selfAssessmentDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true,
                testEndDate = relevantTestDate
            )
        )

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationSelfAssessmentAndTest(
            selfAssessmentDate = selfAssessmentDate,
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion

    //region -- Positive indicative tests that are older than a previous negative

    @Test
    fun `when not isolating with negative confirmed test, new positive indicative test result with confirmatory limit of -1 and older than negative, triggers isolation and completes positive indicative test`() {
        `when not isolating with negative confirmed test, new positive indicative test result outside confirmatory day limit and older than negative, triggers isolation and completes positive indicative test`(
            confirmatoryDayLimit = -1,
            testEndDate = testEndDate.minus(3, DAYS)
        )
    }

    @Test
    fun `when not isolating with negative confirmed test, new positive indicative test result outside confirmatory day limit and older than negative, triggers isolation and completes positive indicative test`() {
        `when not isolating with negative confirmed test, new positive indicative test result outside confirmatory day limit and older than negative, triggers isolation and completes positive indicative test`(
            confirmatoryDayLimit = 2,
            testEndDate = testEndDate.minus(3, DAYS)
        )
    }

    private fun `when not isolating with negative confirmed test, new positive indicative test result outside confirmatory day limit and older than negative, triggers isolation and completes positive indicative test`(
        confirmatoryDayLimit: Int,
        testEndDate: Instant
    ) {
        val negativeTestResult = negativeTestResultConfirmed.toAcknowledgedTestResult()
        val state = neverIsolatingWithNegativeTest(
            testResult = negativeTestResult,
        )

        val testResult = positiveTestResultIndicative.copy(
            confirmatoryDayLimit = confirmatoryDayLimit,
            testEndDate = testEndDate
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult = testResult.toAcknowledgedTestResult().copy(confirmedDate = negativeTestResult.testEndDate, confirmatoryTestCompletionStatus = COMPLETED)
        val expectedState = state.copy(
            testResult = expectedTestResult
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired index case and negative test, new positive indicative test result older than symptoms and outside confirmatory day limit, triggers isolation and completes positive indicative test`() {
        val negativeTestResult = negativeTestResultConfirmed.toAcknowledgedTestResult()
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = indexCaseStartDate,
            onsetDate = indexCaseStartDate,
            testResult = negativeTestResult
        )

        val testResult = positiveTestResultIndicative.copy(
            confirmatoryDayLimit = -1,
            testEndDate = indexCaseStartDate.minus(3, DAYS).toInstant()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult = testResult.toAcknowledgedTestResult()
            .copy(
                confirmedDate = negativeTestResult.testEndDate,
                confirmatoryTestCompletionStatus = COMPLETED
            )
        val expectedState = state.copy(
            testResult = expectedTestResult,
            selfAssessment = null
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired index case with self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation is ignored`() {
        val relevantTestDate = testEndDate.minus(4, DAYS)

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = relevantTestDate.minus(2, DAYS).toLocalDate(fixedClock.zone),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true,
                testEndDate = relevantTestDate
            )
        )

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
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
            encounterDate = relevantTestDate.minus(2, DAYS).toLocalDate(fixedClock.zone)
        ).copy(
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true,
                testEndDate = relevantTestDate
            )
        )

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = testResult.toAcknowledgedTestResult()
        )
        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
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
            encounterDate = relevantTestDate.minus(2, DAYS).toLocalDate(fixedClock.zone)
        ).copy(
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true,
                testEndDate = relevantTestDate
            )
        )

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = true, keySharingInfo = null), result)
    }

    //endregion

    //region -- No memory of previous isolation

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

    private fun `when not in isolation, expired positive test result stores expired index isolation`(
        receivedTestConfirmed: Boolean
    ) {
        val state = neverIsolating()

        val receivedTestResult = positiveTestResult(confirmed = receivedTestConfirmed)
            .copy(testEndDate = now.minus(20, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(receivedTestResult.toAcknowledgedTestResult())
        val expectedKeySharingInfo =
            if (receivedTestResult.isConfirmed())
                KeySharingInfo(
                    diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
                    acknowledgedDate = Instant.now(fixedClock)
                )
            else null
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion
    //endregion

    //region --- Negative, arriving in order

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, new negative confirmed test result within prescribed day limit replaces index case`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false
            )
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = Instant.now(fixedClock).plus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            testResult = testResult.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, new negative confirmed test result within prescribed day limit replaces index case`() {
        val state = isolationPositiveTest(
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            selfAssessment = null,
            testResult = negativeTestResultConfirmed.toAcknowledgedTestResult(),
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, new negative confirmed test result outside prescribed day limit updates index case's completed date`() {
        val previousTestResult = positiveTestResultIndicative.toAcknowledgedTestResult()
        val state = isolationPositiveTest(
            testResult = previousTestResult
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = Instant.now(fixedClock)
                .plus(positiveTestResultIndicative.confirmatoryDayLimit!!.toLong(), DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult = previousTestResult.copy(
            confirmedDate = testResult.testEndDate.toLocalDate(fixedClock.zone),
            confirmatoryTestCompletionStatus = COMPLETED
        )
        val expectedState = state.copy(
            testResult = expectedTestResult
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired unconfirmed positive test, new negative confirmed test result outside prescribed day limit updates index case's completed date`() {
        val previousTestResult = positiveTestResultIndicative
            .copy(testEndDate = testEndDate.minus(13, DAYS))
            .toAcknowledgedTestResult()
        val state = isolationPositiveTest(
            testResult = previousTestResult
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = Instant.now(fixedClock)
                .minus(11, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult = previousTestResult.copy(
            confirmedDate = testResult.testEndDate.toLocalDate(fixedClock.zone),
            confirmatoryTestCompletionStatus = COMPLETED
        )
        val expectedState = state.copy(
            testResult = expectedTestResult
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed with confirmatory day limit of -1, new negative confirmed test result updates index case's completed date`() {
        val previousTestResult = positiveTestResultIndicative.toAcknowledgedTestResult().copy(confirmatoryDayLimit = -1)
        val state = isolationPositiveTest(
            testResult = previousTestResult
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = Instant.now(fixedClock)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedTestResult = previousTestResult.copy(
            confirmedDate = testResult.testEndDate.toLocalDate(fixedClock.zone),
            confirmatoryTestCompletionStatus = COMPLETED
        )
        val expectedState = state.copy(
            testResult = expectedTestResult
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case, with expired index case, with relevant positive unconfirmed, new negative confirmed test result replaces index case`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false,
                testEndDate = testEndDate.minus(12, DAYS)
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            testResult = negativeTestResultConfirmed.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation, with relevant positive confirmed, new negative confirmed test result is ignored`() {
        val state = isolationPositiveTest(
            acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, without relevant positive confirmed, new negative confirmed test result is stored`() {
        val state = isolationSelfAssessment()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            testResult = negativeTestResultConfirmed.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact and index case, without relevant positive confirmed, new negative confirmed test result is stored`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            selfAssessment = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            testResult = negativeTestResultConfirmed.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case only, without relevant positive confirmed, new negative confirmed test result is stored`() {
        val state = isolationContactCase()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.copy(
            testResult = negativeTestResultConfirmed.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant negative, new negative confirmed test result is ignored`() {
        val state = isolationSelfAssessmentAndTest(
            testResult = acknowledgedTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
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
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            negativeTestResultConfirmed.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, with expired index case, with relevant negative, negative confirmed test result is ignored`() {
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = indexCaseStartDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.NEGATIVE,
                isConfirmed = true
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    //endregion

    //region --- Negative, arriving out of order
    //region -- Negative older than relevant test

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, negative confirmed test result older than relevant test is ignored`() {
        val relevantTestDate = Instant.now(fixedClock).plus(2, DAYS)

        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false,
                testEndDate = relevantTestDate
            )
        )

        val testResult = negativeTestResultConfirmed.copy(testEndDate = relevantTestDate.minus(1, DAYS))

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
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
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    //endregion

    //region -- Negative older than symptoms

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, negative confirmed test result older than symptoms onset is ignored`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            selfAssessment = selfAssessment(selfAssessmentDate = indexCaseStartDate),
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = false
            )
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
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
            state,
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
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact and index case, without relevant positive confirmed, negative confirmed test result older than symptoms onset date is ignored`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            selfAssessment = selfAssessment()
        )

        val testResult = negativeTestResultConfirmed.copy(
            testEndDate = state.selfAssessmentSymptomsOnsetInstant().minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
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
            state,
            testResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    //endregion
    //endregion

    //region --- Void

    @Test
    fun `when in isolation as index case only, void confirmed test result is ignored`() {
        val state = isolationSelfAssessment()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case only, void confirmed test result is ignored`() {
        val state = isolationContactCase()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact and index case, void confirmed test result is ignored`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            selfAssessment = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, void confirmed test result is ignored`() {
        val state = neverIsolating()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            voidTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    //endregion

    //region --- PLOD

    @Test
    fun `when in isolation as index case only, plod confirmed test result is ignored`() {
        val state = isolationSelfAssessment()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            plodTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact case only, plod confirmed test result is ignored`() {
        val state = isolationContactCase()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            plodTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as contact and index case, plod confirmed test result is ignored`() {
        val state = IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(),
            selfAssessment = selfAssessment()
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            plodTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when not in isolation, plod confirmed test result is ignored`() {
        val state = neverIsolating()

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            plodTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    //endregion

    //region --- Positive test followed by self-assessment, then test
    //region -- Positive test followed by self-assessment, then *positive* test

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, positive confirmed test result with date equal onset is stored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive confirmed test result is stored`(
            previousTestEndDate = testEndDate.minus(2, DAYS),
            symptomsOnsetDate = testEndDate.toLocalDate(fixedClock.zone),
            relevantTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, positive confirmed test result with date after onset is stored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive confirmed test result is stored`(
            previousTestEndDate = testEndDate.minus(2, DAYS),
            symptomsOnsetDate = testEndDate.minus(1, DAYS).toLocalDate(fixedClock.zone),
            relevantTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment, positive confirmed test result with date equal onset is stored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive confirmed test result is stored`(
            previousTestEndDate = testEndDate.minus(2, DAYS),
            symptomsOnsetDate = testEndDate.toLocalDate(fixedClock.zone),
            relevantTestConfirmed = false
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment, positive confirmed test result with date after onset is stored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive confirmed test result is stored`(
            previousTestEndDate = testEndDate.minus(2, DAYS),
            symptomsOnsetDate = testEndDate.minus(1, DAYS).toLocalDate(fixedClock.zone),
            relevantTestConfirmed = false
        )
    }

    private fun `when in isolation as index case, with positive followed by self-assessment, positive confirmed test result is stored`(
        previousTestEndDate: Instant,
        symptomsOnsetDate: LocalDate,
        relevantTestConfirmed: Boolean
    ) {
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = previousTestEndDate
            )
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = positiveTestResultConfirmed.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = positiveTestResultConfirmed.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment, positive confirmed test result with date before previous test is stored`() {
        val previousTestEndDate = testEndDate.minus(2, DAYS)
        val symptomsOnsetDate = testEndDate.minus(1, DAYS).toLocalDate(fixedClock.zone)
        val relevantTestConfirmed = false

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = previousTestEndDate
            )
        )

        val receivedTestResult = positiveTestResultConfirmed.copy(
            testEndDate = previousTestEndDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = receivedTestResult.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment, positive indicative test result with date before previous test is stored`() {
        val previousTestEndDate = testEndDate.minus(2, DAYS)
        val symptomsOnsetDate = testEndDate.minus(1, DAYS).toLocalDate(fixedClock.zone)
        val relevantTestConfirmed = false

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = previousTestEndDate
            )
        )

        val receivedTestResult = positiveTestResultIndicative.copy(
            testEndDate = previousTestEndDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = receivedTestResult.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment, positive confirmed test result with date after previous test and before onset confirms positive`() {
        val relevantTestEndDate = testEndDate.minus(4, DAYS)
        val receivedTestEndDate = relevantTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = receivedTestEndDate.plus(1, DAYS).toLocalDate(fixedClock.zone)
        val relevantTestConfirmed = false

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = relevantTestConfirmed,
            testEndDate = relevantTestEndDate
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val receivedTestResult = positiveTestResultConfirmed.copy(testEndDate = receivedTestEndDate)

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = relevantTestResult.copy(
                confirmedDate = receivedTestEndDate.toLocalDate(fixedClock.zone),
                confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
            )
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment, positive indicative test result with date after previous test and before onset is ignored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive indicative test result with date after previous test and before onset is ignored`(
            relevantTestConfirmed = false
        )
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, positive indicative test result with date after previous test and before onset is ignored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive indicative test result with date after previous test and before onset is ignored`(
            relevantTestConfirmed = true
        )
    }

    private fun `when in isolation as index case, with positive followed by self-assessment, positive indicative test result with date after previous test and before onset is ignored`(
        relevantTestConfirmed: Boolean
    ) {
        val relevantTestEndDate = testEndDate.minus(4, DAYS)
        val receivedTestEndDate = relevantTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = receivedTestEndDate.plus(1, DAYS).toLocalDate(fixedClock.zone)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = relevantTestConfirmed,
            testEndDate = relevantTestEndDate
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultIndicative.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, positive confirmed test result with date after previous test and before onset is ignored`() {
        val relevantTestEndDate = testEndDate.minus(4, DAYS)
        val receivedTestEndDate = relevantTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = receivedTestEndDate.plus(1, DAYS).toLocalDate(fixedClock.zone)
        val relevantTestConfirmed = true

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = relevantTestConfirmed,
            testEndDate = relevantTestEndDate
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val receivedTestResult = positiveTestResultConfirmed.copy(testEndDate = receivedTestEndDate)

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(DoNotTransition(preventKeySubmission = false, expectedKeySharingInfo), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment, positive indicative test result with date after onset is ignored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive indicative test result with date after onset is ignored`(
            relevantTestConfirmed = false
        )
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, positive indicative test result with date after onset is ignored`() {
        `when in isolation as index case, with positive followed by self-assessment, positive indicative test result with date after onset is ignored`(
            relevantTestConfirmed = true
        )
    }

    private fun `when in isolation as index case, with positive followed by self-assessment, positive indicative test result with date after onset is ignored`(
        relevantTestConfirmed: Boolean
    ) {
        val relevantTestEndDate = testEndDate.minus(4, DAYS)
        val symptomsOnsetInstant = relevantTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = symptomsOnsetInstant.plus(1, DAYS)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = relevantTestConfirmed,
            testEndDate = relevantTestEndDate
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            positiveTestResultIndicative.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, positive indicative test result with date before previous test is stored and confirmed`() {
        val previousTestEndDate = testEndDate.minus(2, DAYS)
        val symptomsOnsetDate = testEndDate.minus(1, DAYS).toLocalDate(fixedClock.zone)
        val relevantTestConfirmed = true

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = previousTestEndDate
            )
        )

        val receivedTestResult = positiveTestResultIndicative.copy(
            testEndDate = previousTestEndDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = receivedTestResult.toAcknowledgedTestResult(
                confirmedDate = previousTestEndDate.toLocalDate(fixedClock.zone)
            )
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, positive confirmed test result with date before previous test is stored`() {
        val previousTestEndDate = testEndDate.minus(2, DAYS)
        val symptomsOnsetDate = testEndDate.minus(1, DAYS).toLocalDate(fixedClock.zone)
        val relevantTestConfirmed = true

        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = acknowledgedTestResult(
                result = RelevantVirologyTestResult.POSITIVE,
                isConfirmed = relevantTestConfirmed,
                testEndDate = previousTestEndDate
            )
        )

        val receivedTestResult = positiveTestResultConfirmed.copy(
            testEndDate = previousTestEndDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = state.addTestResult(
            testResult = receivedTestResult.toAcknowledgedTestResult()
        )

        val expectedKeySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = receivedTestResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )
        assertEquals(Transition(expectedState.toIsolationInfo(), expectedKeySharingInfo), result)
    }

    //endregion

    //region -- Positive *confirmed* test followed by self-assessment, then *negative* test

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, negative test result with date equal onset deletes self-assessment`() {
        `when in isolation as index case, with positive confirmed followed by self-assessment, negative test result deletes self-assessment`(
            previousTestEndDate = testEndDate.minus(2, DAYS),
            symptomsOnsetDate = testEndDate.toLocalDate(fixedClock.zone)
        )
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, negative test result with date after onset deletes self-assessment`() {
        `when in isolation as index case, with positive confirmed followed by self-assessment, negative test result deletes self-assessment`(
            previousTestEndDate = testEndDate.minus(2, DAYS),
            symptomsOnsetDate = testEndDate.minus(1, DAYS).toLocalDate(fixedClock.zone)
        )
    }

    private fun `when in isolation as index case, with positive confirmed followed by self-assessment, negative test result deletes self-assessment`(
        previousTestEndDate: Instant,
        symptomsOnsetDate: LocalDate
    ) {
        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = true,
            testEndDate = previousTestEndDate
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(relevantTestResult)

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive confirmed followed by self-assessment, negative test result with date before onset is ignored`() {
        val previousTestEndDate = testEndDate.minus(2, DAYS)
        val receivedTestEndDate = previousTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = receivedTestEndDate.plus(1, DAYS).toLocalDate(fixedClock.zone)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = true,
            testEndDate = previousTestEndDate
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    //endregion

    //region -- Positive *indicative* test followed by self-assessment after day limit, then *negative* test

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result older than previous test and self-assessment is ignored`() {
        val confirmatoryDayLimit = 2
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetDate = previousTestEndDate.plus(confirmatoryDayLimit.toLong() + 1, DAYS).toLocalDate(fixedClock.zone)
        val receivedTestEndDate = previousTestEndDate.minus(1, DAYS)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = previousTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result before day limit deletes positive`() {
        `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result on or within day limit deletes positive`(
            dayLimitOffset = 1
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result on day limit deletes positive`() {
        `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result on or within day limit deletes positive`(
            dayLimitOffset = 0
        )
    }

    private fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result on or within day limit deletes positive`(
        dayLimitOffset: Int
    ) {
        val confirmatoryDayLimit = 2
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val receivedTestEndDate = previousTestEndDate.plus(confirmatoryDayLimit.toLong() - dayLimitOffset, DAYS)
        val symptomsOnsetDate = receivedTestEndDate.plus(1, DAYS).toLocalDate(fixedClock.zone)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = previousTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationSelfAssessment(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result after day limit but before onset completes positive`() {
        val confirmatoryDayLimit = 2
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val receivedTestEndDate = previousTestEndDate.plus(confirmatoryDayLimit.toLong() + 1, DAYS)
        val symptomsOnsetDate = receivedTestEndDate.plus(1, DAYS).toLocalDate(fixedClock.zone)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = previousTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult.copy(
                confirmedDate = receivedTestEndDate.toLocalDate(fixedClock.zone),
                confirmatoryTestCompletionStatus = COMPLETED
            )
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result past day limit and on onset completes positive and deletes symptoms`() {
        `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result past day limit and on or after onset completes positive and deletes symptoms`(
            testEndDateDaysAfterOnset = 0
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result past day limit and after onset completes positive and deletes symptoms`() {
        `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result past day limit and on or after onset completes positive and deletes symptoms`(
            testEndDateDaysAfterOnset = 1
        )
    }

    private fun `when in isolation as index case, with positive indicative followed by self-assessment after day limit, negative test result past day limit and on or after onset completes positive and deletes symptoms`(
        testEndDateDaysAfterOnset: Int
    ) {
        val confirmatoryDayLimit = 2
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetInstant = previousTestEndDate.plus(confirmatoryDayLimit.toLong() + 1, DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = symptomsOnsetInstant.plus(testEndDateDaysAfterOnset.toLong(), DAYS)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = previousTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = relevantTestResult.copy(
                confirmedDate = receivedTestEndDate.toLocalDate(fixedClock.zone),
                confirmatoryTestCompletionStatus = COMPLETED
            )
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    //endregion

    //region -- Positive *indicative* test followed by self-assessment within day limit (or there's no day limit), then *negative* test

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment on day limit, negative test result older than previous test and self-assessment is ignored`() {
        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result older than previous test and self-assessment is ignored`(
            symptomsOnsetDaysAfterPreviousTest = 4,
            confirmatoryDayLimit = 4
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment before day limit, negative test result older than previous test and self-assessment is ignored`() {
        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result older than previous test and self-assessment is ignored`(
            symptomsOnsetDaysAfterPreviousTest = 3,
            confirmatoryDayLimit = 4
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment without day limit, negative test result older than previous test and self-assessment is ignored`() {
        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result older than previous test and self-assessment is ignored`(
            symptomsOnsetDaysAfterPreviousTest = 4,
            confirmatoryDayLimit = null
        )
    }

    private fun `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result older than previous test and self-assessment is ignored`(
        symptomsOnsetDaysAfterPreviousTest: Int,
        confirmatoryDayLimit: Int?
    ) {
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetDate = previousTestEndDate.plus(symptomsOnsetDaysAfterPreviousTest.toLong(), DAYS).toLocalDate(fixedClock.zone)
        val receivedTestEndDate = previousTestEndDate.minus(1, DAYS)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = previousTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        assertEquals(DoNotTransition(preventKeySubmission = false, keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment on day limit, negative test result after positive and before onset deletes positive`() {
        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result after positive and before onset deletes positive`(
            symptomsOnsetDaysAfterPreviousTest = 4,
            confirmatoryDayLimit = 4
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment before day limit, negative test result after positive and before onset deletes positive`() {
        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result after positive and before onset deletes positive`(
            symptomsOnsetDaysAfterPreviousTest = 3,
            confirmatoryDayLimit = 4
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment without day limit, negative test result after positive and before onset deletes positive`() {
        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result after positive and before onset deletes positive`(
            symptomsOnsetDaysAfterPreviousTest = 4,
            confirmatoryDayLimit = null
        )
    }

    private fun `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result after positive and before onset deletes positive`(
        symptomsOnsetDaysAfterPreviousTest: Int,
        confirmatoryDayLimit: Int?
    ) {
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val receivedTestEndDate = previousTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = previousTestEndDate.plus(symptomsOnsetDaysAfterPreviousTest.toLong(), DAYS).toLocalDate(fixedClock.zone)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = previousTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate),
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationSelfAssessment(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment before day limit, negative test result equal onset overwrites positive`() {
        val confirmatoryDayLimit = 4
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetInstant = previousTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = symptomsOnsetInstant

        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result on or within day limit and equal or after onset overwrites positive`(
            confirmatoryDayLimit,
            previousTestEndDate,
            symptomsOnsetDate,
            receivedTestEndDate
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment before day limit, negative test result after onset and within day limit overwrites positive`() {
        val confirmatoryDayLimit = 4
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetInstant = previousTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = symptomsOnsetInstant.plus(1, DAYS)

        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result on or within day limit and equal or after onset overwrites positive`(
            confirmatoryDayLimit,
            previousTestEndDate,
            symptomsOnsetDate,
            receivedTestEndDate
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment before day limit, negative test result after onset and on day limit overwrites positive`() {
        val confirmatoryDayLimit = 4
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetInstant = previousTestEndDate.plus(1, DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = previousTestEndDate.plus(confirmatoryDayLimit.toLong(), DAYS)

        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result on or within day limit and equal or after onset overwrites positive`(
            confirmatoryDayLimit,
            previousTestEndDate,
            symptomsOnsetDate,
            receivedTestEndDate
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment on day limit, negative test result on day limit overwrites positive`() {
        val confirmatoryDayLimit = 4
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetInstant = previousTestEndDate.plus(confirmatoryDayLimit.toLong(), DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = symptomsOnsetInstant

        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result on or within day limit and equal or after onset overwrites positive`(
            confirmatoryDayLimit,
            previousTestEndDate,
            symptomsOnsetDate,
            receivedTestEndDate
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment without day limit, negative test result on onset overwrites positive`() {
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetInstant = previousTestEndDate.plus(4, DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = symptomsOnsetInstant

        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result on or within day limit and equal or after onset overwrites positive`(
            confirmatoryDayLimit = null,
            previousTestEndDate,
            symptomsOnsetDate,
            receivedTestEndDate
        )
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment without day limit, negative test result after onset overwrites positive`() {
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetInstant = previousTestEndDate.plus(4, DAYS)
        val symptomsOnsetDate = symptomsOnsetInstant.toLocalDate(fixedClock.zone)
        val receivedTestEndDate = symptomsOnsetInstant.plus(1, DAYS)

        `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result on or within day limit and equal or after onset overwrites positive`(
            confirmatoryDayLimit = null,
            previousTestEndDate,
            symptomsOnsetDate,
            receivedTestEndDate
        )
    }

    private fun `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result on or within day limit and equal or after onset overwrites positive`(
        confirmatoryDayLimit: Int?,
        relevantTestEndDate: Instant,
        symptomsOnsetDate: LocalDate,
        receivedTestEndDate: Instant
    ) {
        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = relevantTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val receivedTestResult = negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate)

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = receivedTestResult.toAcknowledgedTestResult()
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    @Test
    fun `when in isolation as index case, with positive indicative followed by self-assessment within day limit, negative test result after day limit completes positive and removes symptoms`() {
        val confirmatoryDayLimit = 4
        val previousTestEndDate = testEndDate.minus(5, DAYS)
        val symptomsOnsetDate = previousTestEndDate.plus(1, DAYS).toLocalDate(fixedClock.zone)
        val receivedTestEndDate = previousTestEndDate.plus(confirmatoryDayLimit.toLong() + 1, DAYS)

        val relevantTestResult = acknowledgedTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = previousTestEndDate,
            confirmatoryDayLimit = confirmatoryDayLimit
        )
        val state = isolationSelfAssessmentAndTest(
            selfAssessmentDate = symptomsOnsetDate,
            onsetDate = symptomsOnsetDate,
            testResult = relevantTestResult
        )

        val receivedTestResult = negativeTestResultConfirmed.copy(testEndDate = receivedTestEndDate)

        val result = testSubject.computeTransitionWithTestResultAcknowledgment(
            state,
            receivedTestResult,
            testAcknowledgedDate = Instant.now(fixedClock)
        )

        val expectedState = isolationPositiveTest(
            testResult = relevantTestResult.copy(
                confirmedDate = receivedTestEndDate.toLocalDate(fixedClock.zone),
                confirmatoryTestCompletionStatus = COMPLETED
            )
        )

        assertEquals(Transition(expectedState.toIsolationInfo(), keySharingInfo = null), result)
    }

    //endregion
    //endregion

    private fun positiveTestResult(confirmed: Boolean): ReceivedTestResult =
        if (confirmed) positiveTestResultConfirmed
        else positiveTestResultIndicative

    private fun acknowledgedTestResult(
        result: RelevantVirologyTestResult,
        isConfirmed: Boolean,
        testEndDate: Instant = this.testEndDate,
        confirmatoryDayLimit: Int? = null,
        shouldOfferFollowUpTest: Boolean = !isConfirmed
    ): AcknowledgedTestResult =
        AcknowledgedTestResult(
            testEndDate = testEndDate.toLocalDate(fixedClock.zone),
            testResult = result,
            acknowledgedDate = testEndDate.toLocalDate(fixedClock.zone),
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = !isConfirmed,
            confirmedDate = null,
            confirmatoryDayLimit = confirmatoryDayLimit,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest
        )

    private fun IsolationState.selfAssessmentSymptomsOnsetInstant(): Instant =
        selfAssessment!!.assumedOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC)

    private fun neverIsolating(): IsolationState =
        IsolationState(isolationConfiguration = IsolationConfiguration())

    private fun neverIsolatingWithNegativeTest(testResult: AcknowledgedTestResult): IsolationState {
        if (testResult.testResult != RelevantVirologyTestResult.NEGATIVE) {
            throw IllegalArgumentException("This function can only be called with a negative test result")
        }
        return IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            testResult = testResult
        )
    }

    private fun isolationSelfAssessment(
        selfAssessmentDate: LocalDate = indexCaseStartDate,
        onsetDate: LocalDate? = null
    ): IsolationState =
        IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            selfAssessment = selfAssessment(selfAssessmentDate, onsetDate)
        )

    private fun selfAssessment(
        selfAssessmentDate: LocalDate = indexCaseStartDate,
        onsetDate: LocalDate? = null
    ): SelfAssessment =
        SelfAssessment(selfAssessmentDate, onsetDate)

    private fun isolationPositiveTest(
        testResult: AcknowledgedTestResult,
    ): IsolationState {
        if (testResult.testResult != RelevantVirologyTestResult.POSITIVE) {
            throw IllegalArgumentException("This function can only be called with a positive test result")
        }
        return IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            testResult = testResult
        )
    }

    private fun isolationSelfAssessmentAndTest(
        selfAssessmentDate: LocalDate = indexCaseStartDate,
        onsetDate: LocalDate? = null,
        testResult: AcknowledgedTestResult
    ): IsolationState =
        IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            selfAssessment = selfAssessment(selfAssessmentDate, onsetDate),
            testResult = testResult
        )

    private fun isolationContactCase(
        encounterDate: LocalDate = this.encounterDate
    ): IsolationState =
        IsolationState(
            isolationConfiguration = IsolationConfiguration(),
            contact = contactCase(encounterDate)
        )

    private fun contactCase(
        encounterDate: LocalDate = this.encounterDate
    ): Contact =
        Contact(
            exposureDate = encounterDate,
            notificationDate = encounterDate
        )

    private fun ReceivedTestResult.toAcknowledgedTestResult(
        confirmedDate: LocalDate? = null
    ): AcknowledgedTestResult {
        val result = testResult.toRelevantVirologyTestResult()
        if (result == null) {
            throw IllegalArgumentException("This function cannot be called with a $result test result")
        }
        return AcknowledgedTestResult(
            testEndDate = testEndDate(fixedClock),
            testResult = result,
            testKitType = testKitType,
            acknowledgedDate = LocalDate.now(fixedClock),
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            shouldOfferFollowUpTest = shouldOfferFollowUpTest,
            confirmedDate = confirmedDate,
            confirmatoryDayLimit = confirmatoryDayLimit,
            confirmatoryTestCompletionStatus = confirmedDate?.let { COMPLETED_AND_CONFIRMED }
        )
    }

    private fun LocalDate.toInstant(): Instant =
        atStartOfDay(fixedClock.zone).toInstant()
}

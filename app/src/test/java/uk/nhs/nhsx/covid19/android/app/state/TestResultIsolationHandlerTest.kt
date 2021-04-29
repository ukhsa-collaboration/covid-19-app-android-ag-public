package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SymptomsOnsetDateCalculator
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransitionButStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Ignore
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.TransitionAndStoreTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SymptomsDate
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Confirm
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.OverwriteAndConfirm
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals

class TestResultIsolationHandlerTest {

    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxUnitFun = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxUnitFun = true)
    private val symptomsOnsetDateCalculator = mockk<SymptomsOnsetDateCalculator>()
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)

    private val testSubject = TestResultIsolationHandler(
        relevantTestResultProvider,
        isolationConfigurationProvider,
        symptomsOnsetDateCalculator,
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
        every { isolationConfigurationProvider.durationDays } returns isolationConfiguration
        setRelevantTestResult(result = null, isConfirmed = false)
    }

    // --- Positive, arriving in order

    @Test
    fun `when in isolation as index case with self-assessment, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = true),
            positiveTestResultIndicative
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index case without self-assessment, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = false),
            positiveTestResultIndicative
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index and contact case, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCase(selfAssessment = true),
            positiveTestResultIndicative
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index case, with previous positive confirmed test result from current isolation, positive confirmed test result is ignored`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = false),
            positiveTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index case with self-assessment, positive confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = true),
            positiveTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(Overwrite), result)
    }

    @Test
    fun `when in isolation as index and contact case, positive confirmed test result removes contact case`() {
        val state = isolationStateContactAndIndexCase(selfAssessment = true)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            positiveTestResultConfirmed
        )

        val expectedState = state.copy(contactCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as contact case, positive indicative test result adds index case to isolation`() {
        val positiveTestResult = positiveTestResultIndicative.copy(testEndDate = Instant.parse("2020-08-02T12:00:00Z"))
        setOnsetDate(positiveTestResult, LocalDate.parse("2020-07-30"))

        val state = isolationStateContactCaseOnly()
        val result = testSubject.computeTransitionWithTestResult(
            state, // start=2020-07-23, expiry=2020-08-03
            positiveTestResult
        )

        val expectedState = state.copy(
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-30"),
                expiryDate = LocalDate.parse("2020-08-13"), // limited to not exceed max duration
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as contact case, positive confirmed test result adds index case to isolation and removes contact case`() {
        setOnsetDate(positiveTestResultConfirmed, positiveTestResultConfirmed.testEndDate.minus(3, DAYS).toLocalDate(fixedClock.zone))

        val state = isolationStateContactCaseOnly()
        val result = testSubject.computeTransitionWithTestResult(
            state,
            positiveTestResultConfirmed
        )

        val expectedState = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = positiveTestResultConfirmed.testEndDate
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as index case, with relevant positive indicative, positive confirmed test result does not transition`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val state = isolationStateIndexCaseOnly(selfAssessment = false)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            positiveTestResultConfirmed
        )

        assertEquals(
            DoNotTransitionButStoreTestResult(Confirm(confirmedDate = positiveTestResultConfirmed.testEndDate)),
            result
        )
    }

    private fun setOnsetDate(testResult: ReceivedTestResult, date: LocalDate) {
        every { symptomsOnsetDateCalculator.symptomsOnsetDateFromTestResult(testResult) } returns date
    }

    @Test
    fun `when not in isolation, positive indicative test result triggers isolation`() {
        setOnsetDate(positiveTestResultIndicative, LocalDate.parse("2020-07-22"))

        val result = testSubject.computeTransitionWithTestResult(
            Default(),
            positiveTestResultIndicative
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous index case isolation, positive indicative test result triggers isolation`() {
        setOnsetDate(positiveTestResultIndicative, LocalDate.parse("2020-07-22"))

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateIndexCaseOnly(
                    selfAssessment = true,
                    indexCaseStartInstant = indexCaseStartDate.minus(13, DAYS)
                )
            ),
            positiveTestResultIndicative
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, without previous test result, positive indicative test result triggers isolation`() {
        setOnsetDate(positiveTestResultIndicative, LocalDate.parse("2020-07-22"))

        setRelevantTestResult(result = null, isConfirmed = false)

        val result = testSubject.computeTransitionWithTestResult(
            Default(),
            positiveTestResultIndicative
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with relevant negative, positive confirmed test result triggers isolation`() {
        setOnsetDate(positiveTestResultConfirmed, LocalDate.parse("2020-07-22"))

        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(),
            positiveTestResultConfirmed
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous index case, with relevant negative, positive confirmed test result triggers isolation`() {
        setOnsetDate(positiveTestResultConfirmed, LocalDate.parse("2020-07-22"))

        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateIndexCaseOnly(
                    selfAssessment = true,
                    indexCaseStartInstant = indexCaseStartDate.minus(13, DAYS)
                )
            ),
            positiveTestResultConfirmed
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, with relevant negative, positive confirmed test result triggers isolation`() {
        setOnsetDate(positiveTestResultConfirmed, LocalDate.parse("2020-07-22"))

        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            positiveTestResultConfirmed
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, with relevant negative, positive confirmed test result triggers isolation, test result with explicit onset date`() {
        val positiveTestResult = positiveTestResultConfirmed.copy(
            symptomsOnsetDate = SymptomsDate(
                explicitDate = LocalDate.parse("2020-08-01")
            )
        )
        setOnsetDate(positiveTestResult, LocalDate.parse("2020-08-01"))

        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            positiveTestResult
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-08-01"),
                expiryDate = LocalDate.parse("2020-08-12"),
                selfAssessment = true
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, with relevant negative, positive confirmed test result triggers isolation, test result with cannot remember onset date`() {
        val positiveTestResult = positiveTestResultConfirmed.copy(
            symptomsOnsetDate = SymptomsDate(
                explicitDate = null
            )
        )

        setOnsetDate(positiveTestResult, LocalDate.parse("2020-07-22"))

        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            positiveTestResult
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = true
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous index case, without relevant negative, positive confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateIndexCaseOnly(
                    selfAssessment = true,
                    indexCaseStartInstant = indexCaseStartDate.minus(13, DAYS)
                )
            ),
            positiveTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, without relevant negative, positive confirmed test result triggers isolation`() {
        setOnsetDate(positiveTestResultConfirmed, LocalDate.parse("2020-07-22"))

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            positiveTestResultConfirmed
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, without relevant negative, positive confirmed test result triggers isolation`() {
        setOnsetDate(positiveTestResultConfirmed, LocalDate.parse("2020-07-22"))

        val result = testSubject.computeTransitionWithTestResult(
            Default(),
            positiveTestResultConfirmed
        )

        val expectedState = Isolation(
            isolationStart = testEndDate,
            isolationConfiguration = isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    // --- Positive, arriving out of order
    // -- Tests that are "way too old", i.e., would expire before the start of an existing (active or expired) isolation

    @Test
    fun `when in isolation as a contact case, positive confirmed test result that is way too old does not transition`() {
        `when has contact case, positive test result that is way too old does not transition`(
            isolationActive = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as a contact case, positive indicative test result that is way too old does not transition`() {
        `when has contact case, positive test result that is way too old does not transition`(
            isolationActive = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with previous contact case, positive confirmed test result that is way too old does not transition`() {
        `when has contact case, positive test result that is way too old does not transition`(
            isolationActive = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with previous contact case, positive indicative test result that is way too old does not transition`() {
        `when has contact case, positive test result that is way too old does not transition`(
            isolationActive = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has contact case, positive test result that is way too old does not transition`(
        isolationActive: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val isolation = isolationStateContactCaseOnly(
            encounterDate =
                if (isolationActive) encounterDate
                else encounterDate.minus(13, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val receivedTestResult = positiveTestResult(receivedTestConfirmed)
            .copy(testEndDate = isolation.isolationStart.minus(11, DAYS))

        val result = testSubject.computeTransitionWithTestResult(
            state,
            receivedTestResult
        )

        assertEquals(Ignore(preventKeySubmission = true), result)
    }

    @Test
    fun `when in isolation as an index case with self-assessment, positive confirmed test result that is way too old does not transition`() {
        `when has index case with self-assessment, positive test result that is way too old does not transition`(
            isolationActive = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as an index case with self-assessment, positive indicative test result that is way too old does not transition`() {
        `when has index case with self-assessment, positive test result that is way too old does not transition`(
            isolationActive = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with previous index case with self-assessment, positive confirmed test result that is way too old does not transition`() {
        `when has index case with self-assessment, positive test result that is way too old does not transition`(
            isolationActive = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case with self-assessment, positive indicative test result that is way too old does not transition`() {
        `when has index case with self-assessment, positive test result that is way too old does not transition`(
            isolationActive = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has index case with self-assessment, positive test result that is way too old does not transition`(
        isolationActive: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = true,
            indexCaseStartInstant =
                if (isolationActive) indexCaseStartDate
                else indexCaseStartDate.minus(13, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val receivedTestResult = positiveTestResult(receivedTestConfirmed)
            .copy(testEndDate = isolation.symptomsOnsetInstant().minus(11, DAYS))

        val result = testSubject.computeTransitionWithTestResult(
            state,
            receivedTestResult
        )

        assertEquals(Ignore(preventKeySubmission = true), result)
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive confirmed, positive confirmed test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive indicative, positive confirmed test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive confirmed, positive indicative test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when in isolation as an index case without self-assessment, with relevant positive indicative, positive indicative test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with previous index case without self-assessment, with relevant positive confirmed, positive confirmed test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case without self-assessment, with relevant positive indicative, positive confirmed test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case without self-assessment, with relevant positive confirmed, positive indicative test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false
        )
    }

    @Test
    fun `when not in isolation, with previous index case without self-assessment, with relevant positive indicative, positive indicative test result that is way too old does not transition`() {
        `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has index case without self-assessment, with relevant positive, positive test result that is way too old does not transition`(
        isolationActive: Boolean,
        relevantTestConfirmed: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)
        setRelevantTestResult(
            RelevantVirologyTestResult.POSITIVE,
            relevantTestConfirmed,
            relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = relevantTestDate
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val receivedTestResult = positiveTestResult(receivedTestConfirmed)
            .copy(testEndDate = relevantTestDate.minus(11, DAYS))

        val result = testSubject.computeTransitionWithTestResult(
            state,
            receivedTestResult
        )

        assertEquals(Ignore(preventKeySubmission = true), result)
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
    fun `when not in isolation, with previous index case with self-assessment, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            receivedTestConfirmed = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case with self-assessment, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            receivedTestConfirmed = false
        )
    }

    private fun `when has index case with self-assessment, positive test result older than symptoms replaces index case`(
        isolationActive: Boolean,
        receivedTestConfirmed: Boolean
    ) {
        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = true,
            indexCaseStartInstant =
                if (isolationActive) indexCaseStartDate
                else indexCaseStartDate.minus(13, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = isolation.symptomsOnsetInstant().minus(1, DAYS)
        )

        val expectedOnsetDate = testResult.testEndDate.minus(3, DAYS).toLocalDate(fixedClock.zone)

        setOnsetDate(testResult, expectedOnsetDate)

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        val expectedIsolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = testResult.testEndDate
        )
        val expectedState =
            if (isolationActive) expectedIsolation
            else Default(previousIsolation = expectedIsolation)

        val expectedTransition = TransitionAndStoreTestResult(
            newState = expectedState,
            testResultStorageOperation = Overwrite
        )

        assertEquals(expectedTransition, result)
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
    fun `not in isolation, with previous index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with previous index case with self-assessment, with relevant positive confirmed, positive indicative test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `not in isolation, with previous index case with self-assessment, with relevant positive indicative, positive confirmed test result older than symptoms replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than symptoms replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with previous index case with self-assessment, with relevant positive indicative, positive indicative test result older than symptoms replaces index case`() {
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
        setRelevantTestResult(
            RelevantVirologyTestResult.POSITIVE,
            isConfirmed = relevantTestConfirmed,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = true,
            indexCaseStartInstant = relevantTestDate.minus(4, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = isolation.symptomsOnsetInstant().minus(1, DAYS)
        )

        val expectedOnsetDate = testResult.testEndDate.minus(3, DAYS).toLocalDate(fixedClock.zone)

        setOnsetDate(testResult, expectedOnsetDate)

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        val expectedIsolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = testResult.testEndDate
        )
        val expectedState =
            if (isolationActive) expectedIsolation
            else Default(previousIsolation = expectedIsolation)
        val expectedTestResultStorageOperation =
            if (shouldConfirm) OverwriteAndConfirm(confirmedDate = relevantTestDate)
            else Overwrite
        val expectedTransition = TransitionAndStoreTestResult(
            expectedState,
            expectedTestResultStorageOperation
        )

        assertEquals(expectedTransition, result)
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
    fun `not in isolation, with previous index case without self-assessment, with relevant positive confirmed, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with previous index case without self-assessment, with relevant positive confirmed, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `not in isolation, with previous index case without self-assessment, with relevant positive indicative, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case without self-assessment, with relevant positive, positive test result older than relevant replaces index case`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with previous index case without self-assessment, with relevant positive indicative, positive indicative test result older than relevant test result replaces index case`() {
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
        setRelevantTestResult(
            RelevantVirologyTestResult.POSITIVE,
            isConfirmed = relevantTestConfirmed,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = relevantTestDate
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val expectedOnsetDate = testResult.testEndDate.minus(3, DAYS).toLocalDate(fixedClock.zone)

        setOnsetDate(testResult, expectedOnsetDate)

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        val expectedIsolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = testResult.testEndDate
        )
        val expectedState =
            if (isolationActive) expectedIsolation
            else Default(previousIsolation = expectedIsolation)
        val expectedTestResultStorageOperation =
            if (shouldConfirm) OverwriteAndConfirm(confirmedDate = relevantTestDate)
            else Overwrite
        val expectedTransition = TransitionAndStoreTestResult(
            expectedState,
            expectedTestResultStorageOperation
        )

        assertEquals(expectedTransition, result)
    }

    // - Index case only with self-assessment, with relevant positive newer than received, do not transition, possibly confirm

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive confirmed, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = true,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `when in isolation as index case with self-assessment, with relevant positive indicative, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = true,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with previous index case with self-assessment, with relevant positive confirmed, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with previous index case with self-assessment, with relevant positive confirmed, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = false,
            relevantTestConfirmed = true,
            receivedTestConfirmed = false,
            shouldConfirm = true
        )
    }

    @Test
    fun `not in isolation, with previous index case with self-assessment, with relevant positive indicative, positive confirmed test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = true,
            shouldConfirm = false
        )
    }

    @Test
    fun `not in isolation, with previous index case with self-assessment, with relevant positive indicative, positive indicative test result older than relevant test result replaces index case`() {
        `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
            isolationActive = false,
            relevantTestConfirmed = false,
            receivedTestConfirmed = false,
            shouldConfirm = false
        )
    }

    private fun `when has index case with self-assessment, with relevant positive, positive test result older than relevant does not transition`(
        isolationActive: Boolean,
        relevantTestConfirmed: Boolean,
        receivedTestConfirmed: Boolean,
        shouldConfirm: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)
        setRelevantTestResult(
            RelevantVirologyTestResult.POSITIVE,
            isConfirmed = relevantTestConfirmed,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = true,
            indexCaseStartInstant = relevantTestDate.minus(2, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResult(receivedTestConfirmed).copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        val expectedTestResultStorageOperation =
            if (shouldConfirm) OverwriteAndConfirm(confirmedDate = relevantTestDate)
            else Overwrite
        val expectedTransition = DoNotTransitionButStoreTestResult(expectedTestResultStorageOperation)

        assertEquals(expectedTransition, result)
    }

    // -- Positive confirmed tests that are older than a previous negative

    @Test
    fun `when in isolation as an has index case with self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation does not transition`() {
        `when has index case with self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation does not transition`(
            isolationActive = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case with self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation does not transition`() {
        `when has index case with self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation does not transition`(
            isolationActive = false
        )
    }

    private fun `when has index case with self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation does not transition`(
        isolationActive: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)
        setRelevantTestResult(
            RelevantVirologyTestResult.NEGATIVE,
            isConfirmed = true,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = true,
            indexCaseStartInstant = relevantTestDate.minus(2, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        assertEquals(DoNotTransitionButStoreTestResult(Overwrite), result)
    }

    @Test
    fun `when in isolation as an has index case without self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces index case but preserves isolation start`() {
        `when has index case without self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces index case but preserves isolation start`(
            isolationActive = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case without self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces index case but preserves isolation start`() {
        `when has index case without self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces index case but preserves isolation start`(
            isolationActive = false
        )
    }

    private fun `when has index case without self-assessment, with relevant negative, positive confirmed test result older than relevant test and newer than isolation replaces index case but preserves isolation start`(
        isolationActive: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)
        setRelevantTestResult(
            RelevantVirologyTestResult.NEGATIVE,
            isConfirmed = true,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = relevantTestDate.minus(2, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val expectedOnsetDate = testResult.testEndDate.minus(3, DAYS).toLocalDate(fixedClock.zone)

        setOnsetDate(testResult, expectedOnsetDate)

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        val expectedIsolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = testResult.testEndDate
        ).copy(
            isolationStart = isolation.isolationStart
        )
        val expectedState =
            if (isolationActive) expectedIsolation
            else Default(previousIsolation = expectedIsolation)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    // -- Positive indicative tests that are older than a previous negative

    @Test
    fun `when in isolation as an has index case without self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`() {
        `when has index case without self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
            isolationActive = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case without self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`() {
        `when has index case without self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
            isolationActive = false
        )
    }

    private fun `when has index case without self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
        isolationActive: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)
        setRelevantTestResult(
            RelevantVirologyTestResult.NEGATIVE,
            isConfirmed = true,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = relevantTestDate.minus(2, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        assertEquals(Ignore(preventKeySubmission = true), result)
    }

    @Test
    fun `when in isolation as an has index case with self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`() {
        `when has index case with self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
            isolationActive = true
        )
    }

    @Test
    fun `when not in isolation, with previous index case with self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`() {
        `when has index case with self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
            isolationActive = false
        )
    }

    private fun `when has index case with self-assessment, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
        isolationActive: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)
        setRelevantTestResult(
            RelevantVirologyTestResult.NEGATIVE,
            isConfirmed = true,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateIndexCaseOnly(
            selfAssessment = true,
            indexCaseStartInstant = relevantTestDate.minus(2, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        assertEquals(Ignore(preventKeySubmission = true), result)
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
        setRelevantTestResult(
            RelevantVirologyTestResult.NEGATIVE,
            isConfirmed = true,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateContactCaseOnly(
            encounterDate = relevantTestDate.minus(2, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResultConfirmed.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val expectedOnsetDate = testResult.testEndDate.minus(3, DAYS).toLocalDate(fixedClock.zone)

        setOnsetDate(testResult, expectedOnsetDate)

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        val expectedIsolation = isolationStateIndexCaseOnly(
            selfAssessment = false,
            indexCaseStartInstant = testResult.testEndDate
        )
        val expectedState =
            if (isolationActive) expectedIsolation
            else Default(previousIsolation = expectedIsolation)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`() {
        `when has contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
            isolationActive = true
        )
    }

    @Test
    fun `when not in isolation, with contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`() {
        `when has contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
            isolationActive = false
        )
    }

    private fun `when has contact case, with relevant negative, positive indicative test result older than relevant test and newer than isolation does not transition`(
        isolationActive: Boolean
    ) {
        val relevantTestDate =
            if (isolationActive) testEndDate
            else testEndDate.minus(13, DAYS)
        setRelevantTestResult(
            RelevantVirologyTestResult.NEGATIVE,
            isConfirmed = true,
            testEndDate = relevantTestDate
        )

        val isolation = isolationStateContactCaseOnly(
            encounterDate = relevantTestDate.minus(2, DAYS)
        )
        val state =
            if (isolationActive) isolation
            else Default(previousIsolation = isolation)

        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        assertEquals(Ignore(preventKeySubmission = true), result)
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

    private fun `when not in isolation, expired positive test result stores expired index isolation`(
        receivedTestConfirmed: Boolean
    ) {
        val receivedTestResult = positiveTestResult(confirmed = receivedTestConfirmed)
            .copy(testEndDate = now.minus(20, DAYS))
        val state = Default()

        val expectedOnsetDate = receivedTestResult.testEndDate.minus(3, DAYS).toLocalDate(fixedClock.zone)

        setOnsetDate(receivedTestResult, expectedOnsetDate)

        val result = testSubject.computeTransitionWithTestResult(
            state,
            receivedTestResult
        )

        val expectedState = Default(
            previousIsolation = isolationStateIndexCaseOnly(
                selfAssessment = false,
                indexCaseStartInstant = receivedTestResult.testEndDate
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    // --- Negative, arriving in order

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, new negative confirmed test result removes index case`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val state = isolationStateContactAndIndexCase(selfAssessment = false)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed.copy(testEndDate = Instant.now(fixedClock).plus(1, DAYS))
        )

        val expectedState = state.copy(indexCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, new negative confirmed test result ends isolation`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val state = isolationStateIndexCaseOnly(selfAssessment = false)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed
        )

        val previousIsolationWithUpdatedExpiryDate = Isolation(
            isolationStart = state.isolationStart,
            isolationConfiguration = state.isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = state.indexCase!!.symptomsOnsetDate,
                expiryDate = LocalDateTime.ofInstant(negativeTestResultConfirmed.testEndDate, fixedClock.zone)
                    .toLocalDate(),
                selfAssessment = state.indexCase!!.selfAssessment
            )
        )

        val expectedState = Default(previousIsolation = previousIsolationWithUpdatedExpiryDate)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as contact case only, with relevant positive unconfirmed, new negative confirmed test result does not transition`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly(),
            negativeTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(Overwrite), result)
    }

    @Test
    fun `when in isolation, with relevant positive confirmed, new negative confirmed test result does not transition`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = false),
            negativeTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index case only, without relevant positive confirmed, new negative confirmed test result ends isolation`() {
        val state = isolationStateIndexCaseOnly(selfAssessment = true)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed
        )

        val previousIsolationWithUpdatedExpiryDate = Isolation(
            isolationStart = state.isolationStart,
            isolationConfiguration = state.isolationConfiguration,
            indexCase = IndexCase(
                symptomsOnsetDate = state.indexCase!!.symptomsOnsetDate,
                expiryDate = LocalDateTime.ofInstant(negativeTestResultConfirmed.testEndDate, fixedClock.zone)
                    .toLocalDate(),
                selfAssessment = state.indexCase!!.selfAssessment
            )
        )

        val expectedState = Default(previousIsolation = previousIsolationWithUpdatedExpiryDate)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as contact and index case, without relevant positive confirmed, new negative confirmed test result removes index case`() {
        val state = isolationStateContactAndIndexCase(selfAssessment = true)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed
        )

        val expectedState = state.copy(indexCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as contact case only, without relevant positive confirmed, new negative confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly(),
            negativeTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(Overwrite), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant negative, new negative confirmed test result does not transition`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(),
            negativeTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when not in isolation, with previous index case, new negative confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateIndexCaseOnly(
                    selfAssessment = true,
                    indexCaseStartInstant = indexCaseStartDate.minus(13, DAYS)
                )
            ),
            negativeTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous index case, with relevant negative, negative confirmed test result does not transition`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)
        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateIndexCaseOnly(
                    selfAssessment = true,
                    indexCaseStartInstant = indexCaseStartDate.minus(13, DAYS)
                )
            ),
            negativeTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    // --- Negative, arriving out of order
    // -- Negative older than relevant test

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, negative confirmed test result older than relevant test does not transition`() {
        val relevantTestDate = Instant.now(fixedClock).plus(2, DAYS)
        setRelevantTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = relevantTestDate
        )

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCase(selfAssessment = false),
            negativeTestResultConfirmed.copy(testEndDate = relevantTestDate.minus(1, DAYS))
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, negative confirmed test result older than relevant test does not transition`() {
        val relevantTestDate = Instant.now(fixedClock).plus(2, DAYS)
        setRelevantTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = relevantTestDate
        )

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = false),
            negativeTestResultConfirmed.copy(testEndDate = relevantTestDate.minus(1, DAYS))
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    // -- Negative older than symptoms

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, negative confirmed test result older than symptoms onset does not transition`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val state = isolationStateContactAndIndexCase(selfAssessment = false)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed.copy(
                testEndDate = state.symptomsOnsetInstant().minus(1, DAYS)
            )
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, negative confirmed test result older than symptoms onset date does not transition`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val state = isolationStateIndexCaseOnly(selfAssessment = false)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed.copy(
                testEndDate = state.symptomsOnsetInstant().minus(1, DAYS)
            )
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as index case only, without relevant positive confirmed, negative confirmed test result older than symptoms onset date does not transition`() {
        val state = isolationStateIndexCaseOnly(selfAssessment = true)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed.copy(
                testEndDate = state.symptomsOnsetInstant().minus(1, DAYS)
            )
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as contact and index case, without relevant positive confirmed, negative confirmed test result older than symptoms onset date does not transition`() {
        val state = isolationStateContactAndIndexCase(selfAssessment = true)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed.copy(
                testEndDate = state.symptomsOnsetInstant().minus(1, DAYS)
            )
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when not in isolation, with previous index case, negative confirmed test result older than symptoms onset date does not transition`() {
        val previousIsolation = isolationStateIndexCaseOnly(
            selfAssessment = true,
            indexCaseStartInstant = indexCaseStartDate.minus(13, DAYS)
        )
        val state = Default(previousIsolation = previousIsolation)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            negativeTestResultConfirmed.copy(
                testEndDate = previousIsolation.symptomsOnsetInstant().minus(1, DAYS)
            )
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    // --- Void

    @Test
    fun `when in isolation as index case only, void confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = true),
            voidTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as contact case only, void confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = true),
            voidTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when in isolation as contact and index case, void confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCase(selfAssessment = true),
            voidTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    @Test
    fun `when not in isolation, void confirmed test result does not transition`() {
        val state = Default()
        val result = testSubject.computeTransitionWithTestResult(
            state,
            voidTestResultConfirmed
        )

        assertEquals(Ignore(preventKeySubmission = false), result)
    }

    private fun setRelevantTestResult(
        result: RelevantVirologyTestResult?,
        isConfirmed: Boolean,
        testEndDate: Instant = Companion.testEndDate
    ) {
        val isPositive = result == RelevantVirologyTestResult.POSITIVE

        mockkStatic("uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachineKt")
        every { any<Isolation>().hasConfirmedPositiveTestResult(any()) } returns (isPositive && isConfirmed)
        every { any<Isolation>().hasUnconfirmedPositiveTestResult(any()) } returns (isPositive && !isConfirmed)
        every { any<Isolation>().hasPositiveTestResult(any()) } returns isPositive

        every { relevantTestResultProvider.isTestResultPositive() } returns isPositive
        every { relevantTestResultProvider.isTestResultNegative() } returns (result == RelevantVirologyTestResult.NEGATIVE)

        val relevantTestResult =
            if (result == null) null
            else AcknowledgedTestResult(
                diagnosisKeySubmissionToken = "token",
                testEndDate = testEndDate,
                testResult = result,
                acknowledgedDate = testEndDate,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = !isConfirmed,
                confirmedDate = null
            )
        every { relevantTestResultProvider.testResult } returns relevantTestResult
        every { relevantTestResultProvider.getTestResultIfPositive() } returns
            if (relevantTestResult?.isPositive() == true) relevantTestResult
            else null
    }

    private fun Isolation.symptomsOnsetInstant(): Instant =
        indexCase!!.symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC)

    private fun isolationStateIndexCaseOnly(
        selfAssessment: Boolean = true,
        indexCaseStartInstant: Instant = indexCaseStartDate,
    ): Isolation {
        val indexCaseStartDate = LocalDateTime.ofInstant(indexCaseStartInstant, fixedClock.zone).toLocalDate()
        return Isolation(
            isolationStart = indexCaseStartInstant,
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = indexCaseStartDate.minus(3, DAYS),
                expiryDate = indexCaseStartDate.plus(11, DAYS),
                selfAssessment = selfAssessment
            )
        )
    }

    private fun isolationStateContactAndIndexCase(
        selfAssessment: Boolean = true,
        encounterDate: Instant = Companion.encounterDate,
        indexCaseStartInstant: Instant = indexCaseStartDate
    ): Isolation {
        val indexCaseStartDate = LocalDateTime.ofInstant(indexCaseStartInstant, fixedClock.zone).toLocalDate()
        return Isolation(
            isolationStart = selectEarliest(
                indexCaseStartInstant,
                encounterDate
            ),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = encounterDate,
                notificationDate = encounterDate.minus(1, DAYS),
                expiryDate = LocalDateTime.ofInstant(encounterDate, fixedClock.zone)
                    .toLocalDate()
                    .plus(11, DAYS)
            ),
            indexCase = IndexCase(
                symptomsOnsetDate = indexCaseStartDate.minus(3, DAYS),
                expiryDate = indexCaseStartDate.plus(11, DAYS),
                selfAssessment = selfAssessment
            )
        )
    }

    private fun isolationStateContactCaseOnly(
        encounterDate: Instant = Companion.encounterDate
    ): Isolation =
        Isolation(
            isolationStart = encounterDate,
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = encounterDate,
                notificationDate = encounterDate.minus(1, DAYS),
                expiryDate = LocalDateTime.ofInstant(encounterDate, fixedClock.zone)
                    .toLocalDate()
                    .plus(11, DAYS)
            )
        )

    private fun positiveTestResult(confirmed: Boolean): ReceivedTestResult =
        if (confirmed) positiveTestResultConfirmed
        else positiveTestResultIndicative

    companion object {
        val now: Instant = Instant.parse("2020-07-26T12:00:00Z")!!
        val testEndDate: Instant = Instant.parse("2020-07-25T12:00:00Z")!!
        val indexCaseStartDate: Instant = Instant.parse("2020-07-20T10:00:00Z")!!
        val encounterDate: Instant = now.minus(3, DAYS)!!
    }
}

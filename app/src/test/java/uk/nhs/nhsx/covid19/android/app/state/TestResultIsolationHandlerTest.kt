package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals

class TestResultIsolationHandlerTest {

    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxed = true)
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)

    private val testSubject = TestResultIsolationHandler(
        relevantTestResultProvider,
        isolationConfigurationProvider,
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

    private val expiredPositiveTestResultIndicative = positiveTestResultIndicative.copy(
        testEndDate = now.minus(20, DAYS)
    )

    private val positiveTestResultConfirmed = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    private val expiredPositiveTestResultConfirmed = positiveTestResultConfirmed.copy(
        testEndDate = now.minus(20, DAYS)
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
    fun `when in isolation as index and contact case, expired positive confirmed test result removes contact case`() {
        val state = isolationStateContactAndIndexCase(selfAssessment = true)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            expiredPositiveTestResultConfirmed
        )

        val expectedState = state.copy(contactCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when in isolation as contact case, positive indicative test result adds index case to isolation`() {
        val state = isolationStateContactCaseOnly()
        val result = testSubject.computeTransitionWithTestResult(
            state, // start=2020-07-23, expiry=2020-08-03
            positiveTestResultIndicative.copy(testEndDate = Instant.parse("2020-08-02T12:00:00Z"))
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
    fun `when in isolation as contact case, expired positive confirmed test result removes contact case and stores expired index isolation`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly(),
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default(
            previousIsolation = isolationStateIndexCaseOnly(
                selfAssessment = false,
                indexCaseStartInstant = expiredPositiveTestResultConfirmed.testEndDate
            )
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

        assertEquals(DoNotTransitionButStoreTestResult(Confirm(confirmedDate = positiveTestResultConfirmed.testEndDate)), result)
    }

    @Test
    fun `when in isolation as index case, with relevant positive confirmed, positive indicative test result older than symptoms replaces index case`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)

        val state = isolationStateIndexCaseOnly(selfAssessment = true)
        val testResult = positiveTestResultIndicative.copy(
            testEndDate = state.symptomsOnsetInstant().minus(1, DAYS)
        )
        val result = testSubject.computeTransitionWithTestResult(
            state,
            testResult
        )

        val expectedTransition = TransitionAndStoreTestResult(
            newState = isolationStateIndexCaseOnly(
                selfAssessment = false,
                indexCaseStartInstant = testResult.testEndDate
            ),
            testResultStorageOperation = OverwriteAndConfirm(confirmedDate = positiveTestResultConfirmed.testEndDate)
        )

        assertEquals(expectedTransition, result)
    }

    @Test
    fun `when in isolation as index case, with relevant positive confirmed, positive indicative test result older than relevant test does not transition`() {
        val relevantTestDate = testEndDate
        setRelevantTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = true,
            testEndDate = relevantTestDate
        )
        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = true),
            testResult
        )

        val expectedTransition = DoNotTransitionButStoreTestResult(OverwriteAndConfirm(confirmedDate = positiveTestResultConfirmed.testEndDate))

        assertEquals(expectedTransition, result)
    }

    @Test
    fun `when in isolation as index case, with relevant positive indicative, positive indicative test result older than relevant test does not transition`() {
        val relevantTestDate = testEndDate
        setRelevantTestResult(
            result = RelevantVirologyTestResult.POSITIVE,
            isConfirmed = false,
            testEndDate = relevantTestDate
        )
        val testResult = positiveTestResultIndicative.copy(
            testEndDate = relevantTestDate.minus(1, DAYS)
        )

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnly(selfAssessment = true),
            testResult
        )

        val expectedTransition = DoNotTransitionButStoreTestResult(Overwrite)

        assertEquals(expectedTransition, result)
    }

    @Test
    fun `when not in isolation, positive indicative test result triggers isolation`() {
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
    fun `when not in isolation, expired positive indicative test result stores expired index isolation`() {
        val state = Default()
        val result = testSubject.computeTransitionWithTestResult(
            state,
            expiredPositiveTestResultIndicative
        )

        val expectedState = Default(
            previousIsolation = isolationStateIndexCaseOnly(
                selfAssessment = false,
                indexCaseStartInstant = expiredPositiveTestResultConfirmed.testEndDate
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous index case isolation, positive indicative test result triggers isolation`() {
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
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            positiveTestResultConfirmed.copy(
                symptomsOnsetDate = SymptomsDate(
                    explicitDate = LocalDate.parse("2020-08-01")
                )
            )
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
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            positiveTestResultConfirmed.copy(
                symptomsOnsetDate = SymptomsDate(
                    explicitDate = null
                )
            )
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
    fun `when not in isolation, with previous contact case, with relevant negative, expired positive confirmed test result removes old contact case and stores expired index isolation`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default(
            previousIsolation = isolationStateIndexCaseOnly(
                selfAssessment = false,
                indexCaseStartInstant = expiredPositiveTestResultConfirmed.testEndDate
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, with previous index and contact case, with relevant negative, expired positive confirmed test result removes old contact case and stores expired index isolation`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactAndIndexCase(
                    selfAssessment = false,
                    encounterDate = encounterDate.minus(13, DAYS),
                    indexCaseStartInstant = indexCaseStartDate.minus(13, DAYS)
                )
            ),
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default(
            previousIsolation = isolationStateIndexCaseOnly(
                selfAssessment = false,
                indexCaseStartInstant = expiredPositiveTestResultConfirmed.testEndDate
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
    fun `when not in isolation, with previous contact case, without relevant negative, expired positive confirmed test result removes previous contact case and stores expired index isolation`() {
        val result = testSubject.computeTransitionWithTestResult(
            Default(
                previousIsolation = isolationStateContactCaseOnly(
                    encounterDate = encounterDate.minus(13, DAYS)
                )
            ),
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default(
            previousIsolation = isolationStateIndexCaseOnly(
                selfAssessment = false,
                indexCaseStartInstant = expiredPositiveTestResultConfirmed.testEndDate
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
    }

    @Test
    fun `when not in isolation, without relevant negative, positive confirmed test result triggers isolation`() {
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
                expiryDate = LocalDateTime.ofInstant(negativeTestResultConfirmed.testEndDate, fixedClock.zone).toLocalDate(),
                selfAssessment = state.indexCase!!.selfAssessment
            )
        )

        val expectedState = Default(previousIsolation = previousIsolationWithUpdatedExpiryDate)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
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
                expiryDate = LocalDateTime.ofInstant(negativeTestResultConfirmed.testEndDate, fixedClock.zone).toLocalDate(),
                selfAssessment = state.indexCase!!.selfAssessment
            )
        )

        val expectedState = Default(previousIsolation = previousIsolationWithUpdatedExpiryDate)

        assertEquals(TransitionAndStoreTestResult(expectedState, Overwrite), result)
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
        every { relevantTestResultProvider.testResult } returns
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

    companion object {
        val now: Instant = Instant.parse("2020-07-26T12:00:00Z")!!
        val testEndDate: Instant = Instant.parse("2020-07-25T12:00:00Z")!!
        val indexCaseStartDate: Instant = Instant.parse("2020-07-20T10:00:00Z")!!
        val encounterDate: Instant = now.minus(3, DAYS)!!
    }
}

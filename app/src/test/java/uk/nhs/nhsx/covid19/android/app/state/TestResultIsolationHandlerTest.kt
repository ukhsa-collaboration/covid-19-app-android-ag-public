package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import okhttp3.internal.UTC
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
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.IGNORE
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.OVERWRITE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals

class TestResultIsolationHandlerTest {

    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxed = true)
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)

    private val testSubject = spyk(
        TestResultIsolationHandler(
            relevantTestResultProvider,
            isolationConfigurationProvider,
            fixedClock
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

    private val isolationStateContactAndIndexCaseWithoutSelfAssessment = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = Instant.now(fixedClock),
            notificationDate = Instant.now(fixedClock),
            expiryDate = LocalDate.now(fixedClock).plus(11, DAYS)
        ),
        indexCase = IndexCase(
            symptomsOnsetDate = LocalDate.now(fixedClock),
            expiryDate = symptomsOnsetDate.plus(7, DAYS),
            selfAssessment = false
        )
    )

    private val isolationStateContactCaseOnly = Isolation(
        isolationStart = Instant.now(fixedClock),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = Instant.now(fixedClock),
            notificationDate = Instant.now(fixedClock),
            expiryDate = LocalDate.now(fixedClock).plus(11, DAYS)
        )
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
            isolationStateIndexCaseOnlyWithSelfAssessment,
            positiveTestResultIndicative
        )

        assertEquals(Ignore, result)
    }

    @Test
    fun `when in isolation as index case without self-assessment, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            positiveTestResultIndicative
        )

        assertEquals(Ignore, result)
    }

    @Test
    fun `when in isolation as index and contact case, positive indicative test result is ignored`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCaseWithoutSelfAssessment,
            positiveTestResultIndicative
        )

        assertEquals(Ignore, result)
    }

    @Test
    fun `when in isolation as index case, with previous positive confirmed test result from current isolation, positive confirmed test result is ignored`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            positiveTestResultConfirmed
        )

        assertEquals(Ignore, result)
    }

    @Test
    fun `when in isolation as index case with self-assessment, positive confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithSelfAssessment,
            positiveTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(OVERWRITE), result)
    }

    @Test
    fun `when in isolation as index case without self-assessment, positive confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            positiveTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(OVERWRITE), result)
    }

    @Test
    fun `when in isolation as index and contact case, positive confirmed test result removes contact case`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCaseWithoutSelfAssessment,
            positiveTestResultConfirmed
        )

        val expectedState = isolationStateContactAndIndexCaseWithoutSelfAssessment.copy(contactCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as index and contact case, expired positive confirmed test result removes contact case`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCaseWithoutSelfAssessment,
            expiredPositiveTestResultConfirmed
        )

        val expectedState = isolationStateContactAndIndexCaseWithoutSelfAssessment.copy(contactCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as contact case, positive indicative test result adds index case to isolation`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly,
            positiveTestResultIndicative
        )

        val expectedState = isolationStateContactCaseOnly.copy(
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = LocalDate.parse("2020-08-05"),
                selfAssessment = false
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as contact case, positive confirmed test result adds index case to isolation and removes contact case`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly,
            positiveTestResultConfirmed
        )

        val indexCaseExpiryDate = LocalDate.parse("2020-08-05")
        val expectedState = isolationStateContactCaseOnly.copy(
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-22"),
                expiryDate = indexCaseExpiryDate,
                selfAssessment = false
            ),
            contactCase = null
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
        assertEquals(indexCaseExpiryDate, ((result as TransitionAndStoreTestResult).newState as Isolation).expiryDate)
    }

    @Test
    fun `when in isolation as contact case, expired positive confirmed test result removes contact case`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly,
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default()

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, expired positive indicative test result does not transition`() {
        val state = Default()
        val result = testSubject.computeTransitionWithTestResult(
            state,
            expiredPositiveTestResultIndicative
        )

        assertEquals(DoNotTransitionButStoreTestResult(OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous index case isolation, positive indicative test result triggers isolation`() {
        val result = testSubject.computeTransitionWithTestResult(
            Default(previousIsolation = isolationStateIndexCaseOnlyWithSelfAssessment),
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous index case, with relevant negative, positive confirmed test result triggers isolation`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(previousIsolation = isolationStateIndexCaseOnlyWithSelfAssessment),
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, with relevant negative, positive confirmed test result triggers isolation`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(previousIsolation = isolationStateContactCaseOnly),
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, with relevant negative, expired positive confirmed test result removes old contact case`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(previousIsolation = isolationStateContactCaseOnly),
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default()

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous index and contact case, with relevant negative, expired positive confirmed test result removes old contact case`() {
        setRelevantTestResult(RelevantVirologyTestResult.NEGATIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            Default(previousIsolation = isolationStateContactAndIndexCaseWithoutSelfAssessment),
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default(
            previousIsolation = isolationStateContactAndIndexCaseWithoutSelfAssessment.copy(
                contactCase = null
            )
        )

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous index case, without relevant negative, positive confirmed test result does not transition`() {
        val state = Default(previousIsolation = isolationStateIndexCaseOnlyWithSelfAssessment)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            positiveTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, without relevant negative, positive confirmed test result triggers isolation`() {
        val state = Default(previousIsolation = isolationStateContactCaseOnly)
        val result = testSubject.computeTransitionWithTestResult(
            state,
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when not in isolation, with previous contact case, without relevant negative, expired positive confirmed test result removes previous contact case`() {
        val state = Default(previousIsolation = isolationStateContactCaseOnly)
        val result = testSubject.computeTransitionWithTestResult(
            state,
            expiredPositiveTestResultConfirmed
        )

        val expectedState = Default()

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
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

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as index and contact case, with relevant positive unconfirmed, negative confirmed test result removes index case`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCaseWithoutSelfAssessment,
            negativeTestResultConfirmed
        )

        val expectedState = isolationStateContactAndIndexCaseWithoutSelfAssessment.copy(indexCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as index case only, with relevant positive unconfirmed, negative confirmed test result ends isolation`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            negativeTestResultConfirmed
        )

        val previousIsolationWithUpdatedExpiryDate = Isolation(
            isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock),
                selfAssessment = false
            )
        )

        val expectedState = Default(previousIsolation = previousIsolationWithUpdatedExpiryDate)

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as contact case only, with relevant positive unconfirmed, negative confirmed test result does not transition`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = false)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly,
            negativeTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(OVERWRITE), result)
    }

    @Test
    fun `when in isolation, with relevant positive confirmed, negative confirmed test result does not transition`() {
        setRelevantTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)

        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            negativeTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(IGNORE), result)
    }

    @Test
    fun `when in isolation as index case only, without relevant positive confirmed, negative confirmed test result ends isolation`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            negativeTestResultConfirmed
        )

        val previousIsolationWithUpdatedExpiryDate = Isolation(
            isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock),
                selfAssessment = false
            )
        )

        val expectedState = Default(previousIsolation = previousIsolationWithUpdatedExpiryDate)

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as contact and index case, without relevant positive confirmed, negative confirmed test result removes index case`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCaseWithoutSelfAssessment,
            negativeTestResultConfirmed
        )

        val expectedState = isolationStateContactAndIndexCaseWithoutSelfAssessment.copy(indexCase = null)

        assertEquals(TransitionAndStoreTestResult(expectedState, OVERWRITE), result)
    }

    @Test
    fun `when in isolation as contact case only, without relevant positive confirmed, negative confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactCaseOnly,
            negativeTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(OVERWRITE), result)
    }

    @Test
    fun `when in isolation as index case only, void confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            voidTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(IGNORE), result)
    }

    @Test
    fun `when in isolation as contact case only, void confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateIndexCaseOnlyWithoutSelfAssessment,
            voidTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(IGNORE), result)
    }

    @Test
    fun `when in isolation as contact and index case, void confirmed test result does not transition`() {
        val result = testSubject.computeTransitionWithTestResult(
            isolationStateContactAndIndexCaseWithoutSelfAssessment,
            voidTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(IGNORE), result)
    }

    @Test
    fun `when not in isolation, void confirmed test result does not transition`() {
        val state = Default()
        val result = testSubject.computeTransitionWithTestResult(
            state,
            voidTestResultConfirmed
        )

        assertEquals(DoNotTransitionButStoreTestResult(IGNORE), result)
    }

    @Test
    fun `when test triggers transition return new state`() {
        val state = Default()
        val testResult = positiveTestResultConfirmed
        val newState = isolationStateIndexCaseOnlyWithSelfAssessment
        every { testSubject.computeTransitionWithTestResult(state, testResult) } returns TransitionAndStoreTestResult(newState, OVERWRITE)

        val result = testSubject.computeNextStateWithTestResult(state, testResult)

        assertEquals(newState, result)
    }

    @Test
    fun `when test does not trigger transition return current state`() {
        val state = Default()
        val testResult = positiveTestResultConfirmed
        every { testSubject.computeTransitionWithTestResult(state, testResult) } returns DoNotTransitionButStoreTestResult(OVERWRITE)

        val result = testSubject.computeNextStateWithTestResult(state, testResult)

        assertEquals(state, result)
    }

    @Test
    fun `when test ignored return current state`() {
        val state = Default()
        val testResult = positiveTestResultConfirmed
        every { testSubject.computeTransitionWithTestResult(state, testResult) } returns Ignore

        val result = testSubject.computeNextStateWithTestResult(state, testResult)

        assertEquals(state, result)
    }

    private fun setRelevantTestResult(
        result: RelevantVirologyTestResult?,
        isConfirmed: Boolean
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
                testEndDate = Instant.now(),
                testResult = result,
                acknowledgedDate = Instant.now(),
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = !isConfirmed,
                confirmedDate = null
            )
    }

    companion object {
        val now = Instant.parse("2020-07-26T12:00:00Z")!!
        val testEndDate = Instant.parse("2020-07-25T12:00:00Z")!!
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")!!
    }
}

@file:Suppress("DEPRECATION")

package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Default4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.ContactCase4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.IndexCase4_9
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult4_9
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS

class MigrateIsolationStateTest {

    private val stateStorage = mockk<StateStorage>(relaxUnitFun = true)
    private val stateStorage4_9 = mockk<StateStorage4_9>(relaxUnitFun = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxUnitFun = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxUnitFun = true)
    private val migrateTestResults = mockk<MigrateTestResults>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2021-01-15T10:00:00Z"), ZoneOffset.UTC)

    private val durationDays = DurationDays(
        // Just setting a non-default value so that we can distinguish this instance from the default
        contactCase = 10
    )

    private val migrateIsolationState = MigrateIsolationState(
        stateStorage,
        stateStorage4_9,
        relevantTestResultProvider,
        isolationConfigurationProvider,
        migrateTestResults,
        fixedClock
    )

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    //region No migration

    @Test
    fun `when already migrated do not migrate`() {
        every { stateStorage4_9.state } returns null

        migrateIsolationState()

        thenNoMigration()
    }

    //endregion

    //region Migrate valid data

    @Test
    fun `migrate default with no previous isolation and no test`() {
        every { stateStorage4_9.state } returns Default4_9()
        every { relevantTestResultProvider.testResult } returns null

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate default with no previous isolation and negative test`() {
        val oldTestResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns Default4_9()
        every { relevantTestResultProvider.testResult } returns oldTestResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = NegativeTest(
                testResult = AcknowledgedTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone),
                    testResult = oldTestResult.testResult,
                    testKitType = oldTestResult.testKitType,
                    acknowledgedDate = oldTestResult.acknowledgedDate.toLocalDate(fixedClock.zone),
                )
            )
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate default with self-assessment isolation and negative test`() {
        val oldIndexCase = IndexCase4_9(
            symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
            expiryDate = LocalDate.now(fixedClock).minusDays(1),
            selfAssessment = true
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            contactCase = null,
            indexCase = oldIndexCase
        )

        val oldTestResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns oldTestResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = oldIndexCase.symptomsOnsetDate.plusDays(2),
                    onsetDate = null
                ),
                testResult = AcknowledgedTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone),
                    testResult = oldTestResult.testResult,
                    testKitType = oldTestResult.testKitType,
                    acknowledgedDate = oldTestResult.acknowledgedDate.toLocalDate(fixedClock.zone),
                ),
                expiryDate = oldIndexCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate default with previous contact and self-assessment isolation, and positive test`() {
        val oldContactCase = ContactCase4_9(
            startDate = Instant.now(fixedClock).minus(5, DAYS),
            notificationDate = Instant.now(fixedClock).minus(4, DAYS),
            expiryDate = LocalDate.now(fixedClock).minusDays(1),
            dailyContactTestingOptInDate = LocalDate.now(fixedClock).minusDays(1)
        )
        val oldIndexCase = IndexCase4_9(
            symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
            expiryDate = LocalDate.now(fixedClock).plusDays(4),
            selfAssessment = true
        )
        val oldState = Default4_9(
            previousIsolation = Isolation4_9(
                isolationStart = Instant.now(fixedClock).minus(5, DAYS),
                isolationConfiguration = durationDays,
                contactCase = oldContactCase,
                indexCase = oldIndexCase
            )
        )

        val oldTestResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns oldTestResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = oldContactCase.startDate.toLocalDate(fixedClock.zone),
                notificationDate = oldContactCase.notificationDate!!.toLocalDate(fixedClock.zone),
                optOutOfContactIsolation = oldContactCase.dailyContactTestingOptInDate?.let { OptOutOfContactIsolation(it) },
                expiryDate = oldContactCase.expiryDate
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = oldIndexCase.symptomsOnsetDate.plusDays(2),
                    onsetDate = null
                ),
                testResult = AcknowledgedTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone),
                    testResult = oldTestResult.testResult,
                    testKitType = oldTestResult.testKitType,
                    acknowledgedDate = oldTestResult.acknowledgedDate.toLocalDate(fixedClock.zone),
                ),
                expiryDate = oldIndexCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = true
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate contact isolation (without notification date and DCT) and no test`() {
        val oldContactCase = ContactCase4_9(
            startDate = Instant.now(fixedClock).minus(5, DAYS),
            notificationDate = null,
            expiryDate = LocalDate.now(fixedClock).plusDays(3),
            dailyContactTestingOptInDate = null
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            indexCase = null,
            contactCase = oldContactCase
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns null

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = oldContactCase.startDate.toLocalDate(fixedClock.zone),
                // We fall back to exposure date when notification date is null
                notificationDate = oldContactCase.startDate.toLocalDate(fixedClock.zone),
                optOutOfContactIsolation = null,
                expiryDate = oldContactCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate contact isolation (with notification date, without DCT) and no test`() {
        val oldContactCase = ContactCase4_9(
            startDate = Instant.now(fixedClock).minus(5, DAYS),
            notificationDate = Instant.now(fixedClock).minus(4, DAYS),
            expiryDate = LocalDate.now(fixedClock).plusDays(3),
            dailyContactTestingOptInDate = null
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            indexCase = null,
            contactCase = oldContactCase
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns null

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = oldContactCase.startDate.toLocalDate(fixedClock.zone),
                notificationDate = oldContactCase.notificationDate!!.toLocalDate(fixedClock.zone),
                optOutOfContactIsolation = null,
                expiryDate = oldContactCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate contact isolation (with notification date, without DCT) and negative test`() {
        val oldContactCase = ContactCase4_9(
            startDate = Instant.now(fixedClock).minus(5, DAYS),
            notificationDate = Instant.now(fixedClock).minus(4, DAYS),
            expiryDate = LocalDate.now(fixedClock).plusDays(3),
            dailyContactTestingOptInDate = null
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            indexCase = null,
            contactCase = oldContactCase
        )

        val oldTestResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns oldTestResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = oldContactCase.startDate.toLocalDate(fixedClock.zone),
                notificationDate = oldContactCase.notificationDate!!.toLocalDate(fixedClock.zone),
                optOutOfContactIsolation = null,
                expiryDate = oldContactCase.expiryDate
            ),
            indexInfo = NegativeTest(
                testResult = AcknowledgedTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone),
                    testResult = oldTestResult.testResult,
                    testKitType = oldTestResult.testKitType,
                    acknowledgedDate = oldTestResult.acknowledgedDate.toLocalDate(fixedClock.zone),
                ),
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate self-assessment isolation and no test`() {
        val oldIndexCase = IndexCase4_9(
            symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
            expiryDate = LocalDate.now(fixedClock).plusDays(4),
            selfAssessment = true
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            indexCase = oldIndexCase
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns null

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = oldIndexCase.symptomsOnsetDate.plusDays(2),
                    onsetDate = null
                ),
                expiryDate = oldIndexCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate self-assessment isolation and positive test`() {
        val oldIndexCase = IndexCase4_9(
            symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
            expiryDate = LocalDate.now(fixedClock).plusDays(4),
            selfAssessment = true
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            indexCase = oldIndexCase
        )

        val oldTestResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns oldTestResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = oldIndexCase.symptomsOnsetDate.plusDays(2),
                    onsetDate = null
                ),
                testResult = AcknowledgedTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone),
                    testResult = oldTestResult.testResult,
                    testKitType = oldTestResult.testKitType,
                    acknowledgedDate = oldTestResult.acknowledgedDate.toLocalDate(fixedClock.zone),
                ),
                expiryDate = oldIndexCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate positive test isolation`() {
        val oldIndexCase = IndexCase4_9(
            symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
            expiryDate = LocalDate.now(fixedClock).plusDays(4),
            selfAssessment = false
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            indexCase = oldIndexCase
        )

        val oldTestResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns oldTestResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone)
                ),
                testResult = AcknowledgedTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone),
                    testResult = oldTestResult.testResult,
                    testKitType = oldTestResult.testKitType,
                    acknowledgedDate = oldTestResult.acknowledgedDate.toLocalDate(fixedClock.zone),
                ),
                expiryDate = oldIndexCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate contact and self-assessment isolation and negative test`() {
        val oldIndexCase = IndexCase4_9(
            symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
            expiryDate = LocalDate.now(fixedClock).minusDays(1),
            selfAssessment = true
        )
        val oldContactCase = ContactCase4_9(
            startDate = Instant.now(fixedClock).minus(5, DAYS),
            notificationDate = Instant.now(fixedClock).minus(4, DAYS),
            expiryDate = LocalDate.now(fixedClock).plusDays(1),
            dailyContactTestingOptInDate = null
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            contactCase = oldContactCase,
            indexCase = oldIndexCase
        )

        val oldTestResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns oldTestResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = oldContactCase.startDate.toLocalDate(fixedClock.zone),
                notificationDate = oldContactCase.notificationDate!!.toLocalDate(fixedClock.zone),
                optOutOfContactIsolation = null,
                expiryDate = oldContactCase.expiryDate
            ),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = oldIndexCase.symptomsOnsetDate.plusDays(2),
                    onsetDate = null
                ),
                testResult = AcknowledgedTestResult(
                    testEndDate = oldTestResult.testEndDate.toLocalDate(fixedClock.zone),
                    testResult = oldTestResult.testResult,
                    testKitType = oldTestResult.testKitType,
                    acknowledgedDate = oldTestResult.acknowledgedDate.toLocalDate(fixedClock.zone),
                ),
                expiryDate = oldIndexCase.expiryDate
            ),
            hasAcknowledgedEndOfIsolation = false
        )
        thenMigrateTo(expectedIsolationState)
    }

    //endregion

    //region Migrate invalid data (unsupported combinations of values)

    @Test
    fun `migrate default with no previous isolation and positive test, discard test`() {
        val testResult = AcknowledgedTestResult4_9(
            diagnosisKeySubmissionToken = "token",
            testEndDate = Instant.now(fixedClock).minus(2, DAYS),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = Instant.now(fixedClock).minus(1, DAYS)
        )

        every { stateStorage4_9.state } returns Default4_9()
        every { relevantTestResultProvider.testResult } returns testResult

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays
        )
        thenMigrateTo(expectedIsolationState)
    }

    @Test
    fun `migrate index isolation with neither self-assessment nor positive test, fall back to self-assessment`() {
        val oldIndexCase = IndexCase4_9(
            symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
            expiryDate = LocalDate.now(fixedClock).minusDays(1),
            selfAssessment = false
        )
        val oldState = Isolation4_9(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = durationDays,
            contactCase = null,
            indexCase = oldIndexCase
        )

        every { stateStorage4_9.state } returns oldState
        every { relevantTestResultProvider.testResult } returns null

        migrateIsolationState()

        val expectedIsolationState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = oldIndexCase.symptomsOnsetDate.plusDays(2),
                    onsetDate = null
                ),
                expiryDate = oldIndexCase.expiryDate
            )
        )
        thenMigrateTo(expectedIsolationState)
    }

    //endregion

    private fun thenNoMigration() {
        verify(exactly = 0) { migrateTestResults() }
        verify(exactly = 0) { stateStorage setProperty "state" value any<IsolationState>() }
        verify(exactly = 0) { relevantTestResultProvider.clear() }
        verify(exactly = 0) { stateStorage4_9.clear() }
    }

    private fun thenMigrateTo(expectedIsolationState: IsolationState) {
        verify { migrateTestResults() }
        verify { stateStorage setProperty "state" value expectedIsolationState }
        verify { relevantTestResultProvider.clear() }
        verify { stateStorage4_9.clear() }
    }
}

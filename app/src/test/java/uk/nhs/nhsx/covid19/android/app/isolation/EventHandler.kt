package uk.nhs.nhsx.covid19.android.app.isolation

import uk.nhs.nhsx.covid19.android.app.isolation.Event.contactIsolationEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.indexIsolationEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTestWithIsolationPeriodOlderThanAssumedIsolationStartDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDateButOlderThanAssumedSymptomOnsetDayIfAny
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNewerThanAssumedSymptomOnsetDateAndAssumedSymptomOnsetDateNewerThanPositiveTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithIsolationPeriodOlderThanAssumedIsolationStartDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedVoidTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.retentionPeriodEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.riskyContact
import uk.nhs.nhsx.covid19.android.app.isolation.Event.riskyContactWithExposureDayOlderThanEarlyIsolationTermination
import uk.nhs.nhsx.covid19.android.app.isolation.Event.selfDiagnosedSymptomatic
import uk.nhs.nhsx.covid19.android.app.isolation.Event.selfDiagnosedSymptomaticWithAssumedOnsetDateOlderThanPositiveTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.terminatedRiskyContactEarly
import uk.nhs.nhsx.covid19.android.app.isolation.StateStorage4_10Representation.Companion.DEFAULT_CONFIRMATORY_DAY_LIMIT
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.SECONDS
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventHandler(
    private val isolationTestContext: IsolationTestContext,
    val isolationConfiguration: DurationDays,
) {

    fun handleEvent(event: Event) {
        val isolationStateMachine = isolationTestContext.getIsolationStateMachine()
        val today = LocalDate.now(isolationTestContext.clock)

        when (event) {
            riskyContact -> {
                val exposureDate = today.minus(1, DAYS)
                sendExposureNotification(exposureDate)
            }

            riskyContactWithExposureDayOlderThanEarlyIsolationTermination -> {
                val isolationState = isolationTestContext.getCurrentLogicalState()
                assertTrue(isolationState is PossiblyIsolating)
                val contactCaseOptInDate = isolationState.contactCase?.optOutOfContactIsolation?.date
                assertNotNull(contactCaseOptInDate)

                val exposureDate = contactCaseOptInDate.minusDays(1)
                sendExposureNotification(exposureDate)
            }

            selfDiagnosedSymptomatic -> isolationStateMachine.processEvent(
                OnPositiveSelfAssessment(CannotRememberDate)
            )

            selfDiagnosedSymptomaticWithAssumedOnsetDateOlderThanPositiveTestEndDate -> {
                val onsetDate = getRememberedPositiveTestResult().testEndDate.minusDays(1)
                isolationStateMachine.processEvent(OnPositiveSelfAssessment(SelectedDate.ExplicitDate(onsetDate)))
            }

            terminatedRiskyContactEarly -> {
                val exposureDate = today.minus(1, DAYS)
                isolationStateMachine.optOutOfContactIsolation(exposureDate, reason = QUESTIONNAIRE)
            }

            receivedConfirmedPositiveTest -> {
                val endDate = today.minusDays(1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveConfirmedTestResult(endDate))
                )
            }

            receivedConfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate -> {
                val endDate = getRememberedNegativeTestResult().testEndDate.minusDays(1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveConfirmedTestResult(endDate))
                )
            }

            receivedConfirmedPositiveTestWithIsolationPeriodOlderThanAssumedIsolationStartDate -> {
                val endDate = getIsolationStartDate()
                    .minusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong() + 1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveConfirmedTestResult(endDate))
                )
            }

            receivedUnconfirmedPositiveTest -> {
                val endDate = today.minusDays(1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveUnconfirmedTestResult(endDate))
                )
            }

            receivedUnconfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate -> {
                val endDate = getRememberedNegativeTestResult().testEndDate.minusDays(1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveUnconfirmedTestResult(endDate))
                )
            }

            receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate -> {
                val endDate = getRememberedOnsetDate().minusDays(1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveUnconfirmedTestResult(endDate))
                )
            }

            receivedUnconfirmedPositiveTestWithIsolationPeriodOlderThanAssumedIsolationStartDate -> {
                val endDate = getIsolationStartDate()
                    .minusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong() + 1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveUnconfirmedTestResult(endDate))
                )
            }

            receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny -> {
                val testBasedEndDate =
                    getRememberedNegativeTestResult().testEndDate.minusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)
                val onsetDateBasedEndDate = getOnsetDateIfAny()?.minusDays(1)

                val endDate = selectEarliest(onsetDateBasedEndDate, testBasedEndDate)
                assertTrue(endDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $endDate)")
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveUnconfirmedTestResult(endDate))
                )
            }

            receivedNegativeTest -> {
                val testResult = isolationTestContext.getCurrentState().testResult
                val endDate = testResult?.testEndDate?.plusDays(1)
                    ?: today.minusDays(1)
                assertTrue(endDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $endDate)")
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(endDate))
                )
            }

            receivedNegativeTestWithEndDateOlderThanAssumedSymptomOnsetDate -> {
                val endDate = getRememberedOnsetDate().minusDays(1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(endDate))
                )
            }

            receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDate -> {
                val testResult = getRememberedTestResult()
                assertFalse(testResult.isConfirmed(), "Test result is unexpectedly confirmed")

                val endDate = testResult.testEndDate.plusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)
                assertTrue(endDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $endDate)")

                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(endDate))
                )
            }

            receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny -> {
                val testResult = getRememberedTestResult()
                assertFalse(testResult.isConfirmed(), "Test result is unexpectedly confirmed")

                val onsetDate = getOnsetDateIfAny()
                val testEndDate = testResult.testEndDate

                val endDate = selectEarliest(onsetDate, testEndDate).minus(1, DAYS)
                assertTrue(endDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $endDate)")

                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(endDate))
                )
            }

            receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDateButOlderThanAssumedSymptomOnsetDayIfAny -> {
                val testResult = getRememberedTestResult()
                assertFalse(testResult.isConfirmed(), "Test result is unexpectedly confirmed")

                val onsetDate = getOnsetDateIfAny()
                val testEndDate = testResult.testEndDate

                val receivedTestEndDate = testEndDate.plusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)

                if (onsetDate != null) {
                    assertTrue(
                        onsetDate.isAfter(testEndDate),
                        "Onset date is not newer than test end date (onsetDate: $onsetDate, testEndDate: $testEndDate)"
                    )
                    assertTrue(
                        receivedTestEndDate.isBefore(onsetDate),
                        "Received test end date is not before onset date (receivedTestEndDate: $receivedTestEndDate, onsetDate: $onsetDate)"
                    )
                }

                assertTrue(
                    receivedTestEndDate.isBeforeOrEqual(today),
                    "testEndDate is in the future (testEndDate: $receivedTestEndDate)"
                )

                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(receivedTestEndDate))
                )
            }

            receivedNegativeTestWithEndDateNewerThanAssumedSymptomOnsetDateAndAssumedSymptomOnsetDateNewerThanPositiveTestEndDate -> {
                val testEndDate = getRememberedPositiveTestResult().testEndDate
                val onsetDate = getRememberedOnsetDate()
                assertTrue(
                    onsetDate.isAfter(testEndDate),
                    "Onset date is not newer than test end date (onsetDate: $onsetDate, testEndDate: $testEndDate)"
                )

                val receivedTestEndDate = onsetDate.plusDays(1)
                assertTrue(
                    receivedTestEndDate.isBeforeOrEqual(today),
                    "testEndDate is in the future (testEndDate: $receivedTestEndDate)"
                )
                assertTrue(
                    receivedTestEndDate.isAfter(onsetDate),
                    "receivedTestEndDate is not after onsetDate (receivedTestEndDate: $receivedTestEndDate, onsetDate: $onsetDate)"
                )

                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(receivedTestEndDate))
                )
            }

            receivedVoidTest -> isolationStateMachine.processEvent(
                OnTestResultAcknowledge(createVoidTestResult())
            )

            contactIsolationEnded -> {
                val isolationState = isolationTestContext.getCurrentLogicalState()
                assertTrue(isolationState is PossiblyIsolating)
                val contactCase = isolationState.contactCase
                assertNotNull(contactCase)
                advanceClockPastDate(contactCase.expiryDate)
            }

            indexIsolationEnded -> {
                val isolationState = isolationTestContext.getCurrentLogicalState()
                assertTrue(isolationState is PossiblyIsolating)
                val indexInfo = isolationState.indexInfo
                assertTrue(indexInfo is IndexCase)
                advanceClockPastDate(indexInfo.expiryDate)
            }

            retentionPeriodEnded -> {
                val isolationState = isolationTestContext.getCurrentLogicalState()
                assertFalse(isolationState.isActiveIsolation(isolationTestContext.clock))
                advanceClockPastTime(
                    isolationTestContext.clock.instant()
                        .plus(isolationConfiguration.pendingTasksRetentionPeriod.toLong(), DAYS)
                )
            }
        }
    }

    private fun getIsolationStartDate(): LocalDate {
        val isolationState = isolationTestContext.getCurrentLogicalState()
        assertTrue(isolationState is PossiblyIsolating)
        return isolationState.startDate
    }

    private fun getRememberedOnsetDate(): LocalDate {
        val isolationState = isolationTestContext.getCurrentLogicalState()
        assertTrue(isolationState is PossiblyIsolating)
        val indexInfo = isolationState.indexInfo
        assertTrue(indexInfo is IndexCase)
        val selfAssessment = indexInfo.selfAssessment
        assertNotNull(selfAssessment)
        return selfAssessment.assumedOnsetDate
    }

    private fun getOnsetDateIfAny(): LocalDate? {
        return isolationTestContext.getCurrentState().selfAssessment?.assumedOnsetDate
    }

    private fun getRememberedPositiveTestResult(): AcknowledgedTestResult {
        val rememberedTest = getRememberedTestResult()
        assertEquals(rememberedTest.testResult, POSITIVE, "Stored test result is not positive")
        return rememberedTest
    }

    private fun getRememberedNegativeTestResult(): AcknowledgedTestResult {
        val rememberedTest = getRememberedTestResult()
        assertEquals(rememberedTest.testResult, NEGATIVE, "Stored test result is not negative")
        return rememberedTest
    }

    private fun getRememberedTestResult(): AcknowledgedTestResult {
        val rememberedTest = isolationTestContext.getCurrentState().testResult
        assertNotNull(rememberedTest, "Did not find stored test result")
        return rememberedTest
    }

    private fun sendExposureNotification(exposureDate: LocalDate) {
        isolationTestContext.sendExposureNotification(exposureDate)
    }

    private fun advanceClockPastDate(date: LocalDate) {
        val instant = date.plusDays(1).atStartOfDay(isolationTestContext.clock.zone).toInstant()
        advanceClockPastTime(instant)
    }

    private fun advanceClockPastTime(instant: Instant) {
        val secondsUntilExpiration = SECONDS.between(isolationTestContext.clock.instant(), instant)
        isolationTestContext.advanceClock(secondsUntilExpiration + 1)
    }

    private fun createPositiveConfirmedTestResult(endDate: LocalDate): ReceivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "newToken",
        testEndDate = endDate.atStartOfDay(isolationTestContext.clock.zone).toInstant(),
        testResult = VirologyTestResult.POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    private fun createPositiveUnconfirmedTestResult(endDate: LocalDate): ReceivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "newToken",
        testEndDate = endDate.atStartOfDay(isolationTestContext.clock.zone).toInstant(),
        testResult = VirologyTestResult.POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = true,
        confirmatoryDayLimit = DEFAULT_CONFIRMATORY_DAY_LIMIT.toInt()
    )

    private fun createNegativeTestResult(endDate: LocalDate): ReceivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "newToken",
        testEndDate = endDate.atStartOfDay().toInstant(ZoneOffset.UTC),
        testResult = VirologyTestResult.NEGATIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    private fun createVoidTestResult(): ReceivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "newToken",
        testEndDate = Instant.now(isolationTestContext.clock).minus(1, DAYS),
        testResult = VOID,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )
}

package uk.nhs.nhsx.covid19.android.app.isolation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance.Builder
import uk.nhs.nhsx.covid19.android.app.isolation.Event.contactIsolationEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.indexIsolationEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedConfirmedPositiveTestWithIsolationPeriodOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNDaysNewerThanRememberedUnconfirmedTestEndDateButOlderThanAssumedSymptomOnsetDayIfAny
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateNewerThanAssumedSymptomOnsetDateAndAssumedSymptomOnsetDateNewerThanPositiveTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithEndDateOlderThanRememberedNegativeTestEndDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedUnconfirmedPositiveTestWithIsolationPeriodOlderThanAssumedSymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.isolation.Event.receivedVoidTest
import uk.nhs.nhsx.covid19.android.app.isolation.Event.retentionPeriodEnded
import uk.nhs.nhsx.covid19.android.app.isolation.Event.riskyContact
import uk.nhs.nhsx.covid19.android.app.isolation.Event.riskyContactWithExposureDayOlderThanIsolationTerminationDueToDCT
import uk.nhs.nhsx.covid19.android.app.isolation.Event.selfDiagnosedSymptomatic
import uk.nhs.nhsx.covid19.android.app.isolation.Event.terminateRiskyContactDueToDCT
import uk.nhs.nhsx.covid19.android.app.isolation.StateStorage4_10Representation.Companion.DEFAULT_CONFIRMATORY_DAY_LIMIT
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
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
    val testAppContext: TestApplicationContext,
    val isolationConfiguration: DurationDays,
) {

    fun handleEvent(event: Event) {
        val isolationStateMachine = testAppContext.getIsolationStateMachine()
        val today = LocalDate.now(testAppContext.clock)

        when (event) {
            riskyContact -> {
                val exposureDate = today.minus(1, DAYS)
                sendExposureNotification(exposureDate)
            }

            riskyContactWithExposureDayOlderThanIsolationTerminationDueToDCT -> {
                val isolationState = testAppContext.getCurrentLogicalState()
                assertTrue(isolationState is PossiblyIsolating)
                val contactCaseOptInDate = isolationState.contactCase?.dailyContactTestingOptInDate
                assertNotNull(contactCaseOptInDate)

                val exposureDate = contactCaseOptInDate.minusDays(1)
                sendExposureNotification(exposureDate)
            }

            selfDiagnosedSymptomatic -> isolationStateMachine.processEvent(
                OnPositiveSelfAssessment(CannotRememberDate)
            )

            terminateRiskyContactDueToDCT -> {
                isolationStateMachine.optInToDailyContactTesting()
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

            receivedConfirmedPositiveTestWithEndDateOlderThanAssumedSymptomOnsetDate -> {
                val endDate = getRememberedOnsetDate().minusDays(1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveConfirmedTestResult(endDate))
                )
            }

            receivedConfirmedPositiveTestWithIsolationPeriodOlderThanAssumedSymptomOnsetDate -> {
                val endDate = getRememberedOnsetDate()
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

            receivedUnconfirmedPositiveTestWithIsolationPeriodOlderThanAssumedSymptomOnsetDate -> {
                val endDate = getRememberedOnsetDate()
                    .minusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong() + 1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveUnconfirmedTestResult(endDate))
                )
            }

            receivedUnconfirmedPositiveTestWithEndDateNDaysOlderThanRememberedNegativeTestEndDate -> {
                val endDate = getRememberedNegativeTestResult().testEndDate.minusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createPositiveUnconfirmedTestResult(endDate))
                )
            }

            receivedNegativeTest -> {
                val testResult = testAppContext.getCurrentState().indexInfo?.testResult
                val endDate = testResult?.testEndDate?.plusDays(1)
                    ?: today.minusDays(1)
                assertTrue(endDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $endDate)")
                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(endDate))
                )
            }

            receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDate -> {
                val testResult = testAppContext.getCurrentState().indexInfo?.testResult
                assertNotNull(testResult, "Did not find required test result")
                assertFalse(testResult.isConfirmed(), "Test result is unexpectedly confirmed")

                val endDate = testResult.testEndDate.minus(1, DAYS)
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
                val testResult = testAppContext.getCurrentState().indexInfo?.testResult
                assertNotNull(testResult, "Did not find required test result")
                assertFalse(testResult.isConfirmed(), "Test result is unexpectedly confirmed")

                val endDate = testResult.testEndDate.plusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)
                assertTrue(endDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $endDate)")

                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(endDate))
                )
            }

            receivedNegativeTestWithEndDateOlderThanRememberedUnconfirmedTestEndDateAndOlderThanAssumedSymptomOnsetDayIfAny -> {
                val testResult = testAppContext.getCurrentState().indexInfo?.testResult
                assertNotNull(testResult, "Did not find required test result")
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
                val testResult = testAppContext.getCurrentState().indexInfo?.testResult
                assertNotNull(testResult, "Did not find required test result")
                assertFalse(testResult.isConfirmed(), "Test result is unexpectedly confirmed")

                val onsetDate = getOnsetDateIfAny()
                val testEndDate = testResult.testEndDate

                val receivedTestEndDate = testEndDate.plusDays(DEFAULT_CONFIRMATORY_DAY_LIMIT + 1)

                if (onsetDate != null) {
                    assertTrue(onsetDate.isAfter(testEndDate), "Onset date is not newer than test end date (onsetDate: $onsetDate, testEndDate: $testEndDate)")
                    assertTrue(receivedTestEndDate.isBefore(onsetDate), "Received test end date is not before onset date (receivedTestEndDate: $receivedTestEndDate, onsetDate: $onsetDate)")
                }

                assertTrue(receivedTestEndDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $receivedTestEndDate)")

                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(receivedTestEndDate))
                )
            }

            receivedNegativeTestWithEndDateNewerThanAssumedSymptomOnsetDateAndAssumedSymptomOnsetDateNewerThanPositiveTestEndDate -> {
                val testResult = testAppContext.getCurrentState().indexInfo?.testResult
                assertNotNull(testResult, "Did not find required test result")
                assertTrue(testResult.isPositive(), "Test result is not positive")

                val testEndDate = testResult.testEndDate
                val onsetDate = getRememberedOnsetDate()
                assertTrue(
                    onsetDate.isAfter(testEndDate),
                    "Onset date is not newer than test end date (onsetDate: $onsetDate, testEndDate: $testEndDate)"
                )

                val receivedTestEndDate = onsetDate.plusDays(1)
                assertTrue(receivedTestEndDate.isBeforeOrEqual(today), "testEndDate is in the future (testEndDate: $receivedTestEndDate)")
                assertTrue(receivedTestEndDate.isAfter(onsetDate), "receivedTestEndDate is not after onsetDate (receivedTestEndDate: $receivedTestEndDate, onsetDate: $onsetDate)")

                isolationStateMachine.processEvent(
                    OnTestResultAcknowledge(createNegativeTestResult(receivedTestEndDate))
                )
            }

            receivedVoidTest -> isolationStateMachine.processEvent(
                OnTestResultAcknowledge(createVoidTestResult())
            )

            contactIsolationEnded -> {
                val isolationState = testAppContext.getCurrentLogicalState()
                assertTrue(isolationState is PossiblyIsolating)
                val contactCase = isolationState.contactCase
                assertNotNull(contactCase)
                advanceClockPastDate(contactCase.expiryDate)
            }

            indexIsolationEnded -> {
                val isolationState = testAppContext.getCurrentLogicalState()
                assertTrue(isolationState is PossiblyIsolating)
                val indexInfo = isolationState.indexInfo
                assertTrue(indexInfo is IndexCase)
                advanceClockPastDate(indexInfo.expiryDate)
            }

            retentionPeriodEnded -> {
                val isolationState = testAppContext.getCurrentLogicalState()
                assertFalse(isolationState.isActiveIsolation(testAppContext.clock))
                advanceClockPastTime(
                    testAppContext.clock.instant()
                        .plus(isolationConfiguration.pendingTasksRetentionPeriod.toLong(), DAYS)
                )
            }
        }
    }

    private fun getRememberedOnsetDate(): LocalDate {
        val isolationState = testAppContext.getCurrentLogicalState()
        assertTrue(isolationState is PossiblyIsolating)
        val indexInfo = isolationState.indexInfo
        assertTrue(indexInfo is IndexCase)
        val isolationTrigger = indexInfo.isolationTrigger
        assertTrue(isolationTrigger is SelfAssessment)
        return isolationTrigger.assumedOnsetDate
    }

    private fun getOnsetDateIfAny(): LocalDate? {
        return ((testAppContext.getCurrentState().indexInfo as? IndexCase)?.isolationTrigger as? SelfAssessment)?.assumedOnsetDate
    }

    private fun getRememberedNegativeTestResult(): AcknowledgedTestResult {
        val rememberedTest = testAppContext.getCurrentState().indexInfo?.testResult
        assertNotNull(rememberedTest)
        assertEquals(rememberedTest.testResult, NEGATIVE)
        return rememberedTest
    }

    private fun sendExposureNotification(exposureNotification: LocalDate) {
        testAppContext.getExposureNotificationApi().mockExposureWindows =
            listOf(
                ExposureWindow.Builder()
                    .setDateMillisSinceEpoch(
                        exposureNotification.atStartOfDay(testAppContext.clock.zone).toInstant().toEpochMilli()
                    )
                    .setInfectiousness(Infectiousness.HIGH)
                    .setScanInstances(
                        listOf(
                            Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                            Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                            Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                            Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                            Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build(),
                            Builder().setMinAttenuationDb(160).setSecondsSinceLastScan(60).build()
                        )
                    )
                    .build()
            )

        testAppContext.sendExposureStateUpdatedBroadcast()
        testAppContext.runBackgroundTasks()
    }

    private fun advanceClockPastDate(date: LocalDate) {
        val instant = date.plusDays(1).atStartOfDay(testAppContext.clock.zone).toInstant()
        advanceClockPastTime(instant)
    }

    private fun advanceClockPastTime(instant: Instant) {
        val secondsUntilExpiration = SECONDS.between(testAppContext.clock.instant(), instant)
        testAppContext.advanceClock(secondsUntilExpiration + 1)
    }

    private fun createPositiveConfirmedTestResult(endDate: LocalDate): ReceivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "newToken",
        testEndDate = endDate.atStartOfDay(testAppContext.clock.zone).toInstant(),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    private fun createPositiveUnconfirmedTestResult(endDate: LocalDate): ReceivedTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "newToken",
        testEndDate = endDate.atStartOfDay(testAppContext.clock.zone).toInstant(),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = true
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
        testEndDate = Instant.now(testAppContext.clock).minus(1, DAYS),
        testResult = VOID,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )
}

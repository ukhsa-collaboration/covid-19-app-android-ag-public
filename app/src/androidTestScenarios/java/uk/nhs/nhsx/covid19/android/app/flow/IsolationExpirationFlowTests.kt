package uk.nhs.nhsx.covid19.android.app.flow

import android.app.PendingIntent
import android.content.Intent
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.analytics.AnalyticsTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.receiver.ExpirationCheckReceiver
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationExpirationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsolationExpirationFlowTests : AnalyticsTest() {

    private val selfDiagnosis = SelfDiagnosis(this)
    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)
    private val isolationChecker = IsolationChecker(testAppContext)
    private val isolationExpirationRobot = IsolationExpirationRobot()
    private val statusRobot = StatusRobot()

    @Before
    override fun setUp() {
        super.setUp()

        cancelAlarm(getIsolationExpirationAlarmPendingIntent())
    }

    @Ignore("Re-enable once the StatusActivity refactoring is merged. cancelAlarm will be replaced with a more appropriate tool")
    @Test
    fun selfDiagnosePositive_whenLastIsolationDayAt9pm_linkTestResult_shouldNotScheduleIsolationExpirationMessageAgain() {
        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 9th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        isolationChecker.assertActiveIndexNoContact()

        // Isolation expiration message alarm is scheduled
        assertNotNull(getIsolationExpirationAlarmPendingIntent())

        cancelAlarm(getIsolationExpirationAlarmPendingIntent())

        // Set date: 9th Jan at 9PM

        advanceClockAndRunBackgroundTasks(Duration.ofDays(8).plusHours(21).seconds)

        isolationChecker.assertActiveIndexNoContact()

        // IsolationExpiration activity is displayed (for this particular test above cancelAlarm disables this due to alarm triggering flakiness)

        // Link positive test result
        manualTestResultEntry.enterPositive(
            LAB_RESULT,
            expectedScreenState = PositiveContinueIsolation
        )

        isolationChecker.assertActiveIndexNoContact()

        // Isolation expiration message alarm is not scheduled
        assertNull(getIsolationExpirationAlarmPendingIntent())
    }

    @Test
    fun startIndexCase_dayBeforeIndexExpiresBefore9pm_doNotInform_after9m_inform_acknowledgeExpiration_indexExpires_notInIsolation() {
        // Day before expiry, at 8pm
        testAppContext.clock.currentInstant = Instant.parse("2020-01-01T20:00:00Z")

        val expiryDate = LocalDate.now(testAppContext.clock).plus(1, DAYS)
        setIsolationWithExpiryDate(expiryDate, isolationConfiguration)

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        // Day before expiry, at 9pm
        testAppContext.clock.currentInstant = Instant.parse("2020-01-01T21:00:00Z")

        startTestActivity<StatusActivity>()

        isolationChecker.assertActiveIndexNoContact()

        waitFor { isolationExpirationRobot.checkActivityIsDisplayed() }

        waitFor { isolationExpirationRobot.checkIsolationWillFinish(expiryDate) }

        isolationExpirationRobot.clickBackToHomeButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        // Day of expiry
        testAppContext.clock.currentInstant = Instant.parse("2020-01-02T00:00:00Z")

        waitFor { isolationChecker.assertExpiredIndexNoContact() }

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    @RetryFlakyTest
    fun startIndexCase_indexExpires_acknowledgeExpiration_notInIsolation() {
        val expiryDate = LocalDate.now(testAppContext.clock)
        setIsolationWithExpiryDate(expiryDate, isolationConfiguration)

        startTestActivity<StatusActivity>()

        waitFor { isolationChecker.assertExpiredIndexNoContact() }

        waitFor { isolationExpirationRobot.checkActivityIsDisplayed() }

        waitFor { isolationExpirationRobot.checkIsolationHasFinished(expiryDate) }

        isolationExpirationRobot.clickBackToHomeButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    private fun setIsolationWithExpiryDate(expiryDate: LocalDate, isolationConfiguration: IsolationConfiguration) {
        val selfAssessmentDate = LocalDate.now(testAppContext.clock)
            .plusDays(
                DAYS.between(LocalDate.now(testAppContext.clock), expiryDate) -
                        isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong()
            )

        testAppContext.setState(
            SelfAssessment(selfAssessmentDate).asIsolation(isolationConfiguration = isolationConfiguration)
        )

        val logicalState = testAppContext.getIsolationStateMachine().readLogicalState()
        assertTrue(logicalState is PossiblyIsolating)
        assertEquals(expiryDate, logicalState.expiryDate)
    }

    private fun cancelAlarm(intent: PendingIntent?) {
        if (intent != null) {
            testAppContext.getAlarmManager().cancel(intent)
            intent.cancel()
        }
    }

    private fun getIsolationExpirationAlarmPendingIntent(): PendingIntent? =
        PendingIntent.getBroadcast(
            testAppContext.app,
            IsolationExpirationAlarmController.EXPIRATION_ALARM_INTENT_ID,
            Intent(testAppContext.app, ExpirationCheckReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
        )
}

package uk.nhs.nhsx.covid19.android.app.flow

import android.app.PendingIntent
import android.content.Intent
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.analytics.AnalyticsTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.receiver.ExpirationCheckReceiver
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import java.time.Duration
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsolationExpirationFlowTests : AnalyticsTest() {

    private val selfDiagnosis = SelfDiagnosis(this)
    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)

    @Before
    override fun setUp() {
        super.setUp()

        cancelAlarm(getIsolationExpirationAlarmPendingIntent())
    }

    @Test
    fun selfDiagnosePositive_whenLastIsolationDayAt9pm_linkTestResult_shouldNotScheduleIsolationExpirationMessageAgain() {
        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 9th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        // Isolation expiration message alarm is scheduled
        assertNotNull(getIsolationExpirationAlarmPendingIntent())

        cancelAlarm(getIsolationExpirationAlarmPendingIntent())

        // Set date: 9th Jan at 9PM

        advanceClock(Duration.ofDays(8).plusHours(21).seconds)

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        // IsolationExpiration activity is displayed (for this particular test above cancelAlarm disables this due to alarm triggering flakiness)

        // Link positive test result
        manualTestResultEntry.enterPositive(
            LAB_RESULT,
            expectedScreenState = PositiveContinueIsolation
        )

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        // Isolation expiration message alarm is not scheduled
        assertNull(getIsolationExpirationAlarmPendingIntent())
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

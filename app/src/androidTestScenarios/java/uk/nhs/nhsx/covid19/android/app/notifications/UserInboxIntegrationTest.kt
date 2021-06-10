package uk.nhs.nhsx.covid19.android.app.notifications

import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowKeySharingReminder
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowUnknownTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserInboxIntegrationTest : EspressoTest() {

    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private lateinit var testSubject: UserInbox

    @Before
    fun setUp() {
        sharedPreferences.edit().clear()
        testSubject = testAppContext.getUserInbox()
    }

    @Test
    fun testOrderOfUserInboxItems() = notReported {
        val expiredSelfAssessment = isolationHelper.selfAssessment(expired = true)
        val expiredIsolation = expiredSelfAssessment.asIsolation()
        val expirationDate = expiredSelfAssessment.expiryDate
        val venueId = "venue-id"
        val testResult = ReceivedTestResult(
            "abc",
            testEndDate = Instant.now(testAppContext.clock),
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = false
        )
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = "abc",
            acknowledgedDate = Instant.now(testAppContext.clock).minus(25, ChronoUnit.HOURS),
            hasDeclinedSharingKeys = true
        )

        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert(venueId, INFORM)
        testAppContext.getUnacknowledgedTestResultsProvider().add(testResult)
        testAppContext.getReceivedUnknownTestResultProvider().value = true
        testAppContext.getKeySharingInfoProvider().keySharingInfo = keySharingInfo
        testAppContext.setState(expiredIsolation)
        testAppContext.getShouldShowEncounterDetectionActivityProvider().value = true

        val firstInboxItem = testSubject.fetchInbox()
        assertThat(firstInboxItem).isInstanceOf(ShowTestResult::class.java)
        testAppContext.getUnacknowledgedTestResultsProvider().remove(testResult)

        val secondInboxItem = testSubject.fetchInbox()
        assertThat(secondInboxItem).isInstanceOf(ShowUnknownTestResult::class.java)
        testAppContext.getReceivedUnknownTestResultProvider().value = false

        val thirdInboxItem = testSubject.fetchInbox()
        assertThat(thirdInboxItem).isInstanceOf(ShowIsolationExpiration::class.java)
        assertEquals(expirationDate, (thirdInboxItem as ShowIsolationExpiration).expirationDate)
        testAppContext.setState(expiredIsolation.copy(hasAcknowledgedEndOfIsolation = true))

        val fourthInboxItem = testSubject.fetchInbox()
        assertThat(fourthInboxItem).isInstanceOf(ShowEncounterDetection::class.java)
        testAppContext.getShouldShowEncounterDetectionActivityProvider().value = null

        val fifthInboxItem = testSubject.fetchInbox()
        assertThat(fifthInboxItem).isInstanceOf(ShowVenueAlert::class.java)
        assertEquals(venueId, (fifthInboxItem as ShowVenueAlert).venueId)
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = null

        val sixthInboxItem = testSubject.fetchInbox()
        assertThat(sixthInboxItem).isInstanceOf(ShowKeySharingReminder::class.java)
        testAppContext.getKeySharingInfoProvider().reset()

        assertNull(testSubject.fetchInbox())
    }
}

package uk.nhs.nhsx.covid19.android.app.notifications

import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.time.Month.AUGUST
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowKeySharingReminder
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import java.time.temporal.ChronoUnit

class UserInboxIntegrationTest : EspressoTest() {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var testSubject: UserInbox

    @Before
    fun setUp() {
        sharedPreferences.edit().clear()
        testSubject = testAppContext.getUserInbox()
    }

    @Test
    fun testPutAndDeleteItemsInUserInbox() = notReported {
        testSubject.addUserInboxItem(ShowEncounterDetection)
        assertThat(testSubject.fetchInbox()).isInstanceOf(ShowEncounterDetection::class.java)
        testSubject.clearItem(ShowEncounterDetection)
        assertNull(testSubject.fetchInbox())
    }

    @Test
    fun testFetchingItemsDoesNotDeleteThem() = notReported {
        testSubject.addUserInboxItem(ShowEncounterDetection)
        assertThat(testSubject.fetchInbox()).isInstanceOf(ShowEncounterDetection::class.java)
        assertNotNull(testSubject.fetchInbox())
    }

    @Test
    fun testPutAndDeleteItemsWithPropertyInUserInbox() = notReported {
        testSubject.addUserInboxItem(ShowVenueAlert("venue-id", INFORM))
        val inboxItem = testSubject.fetchInbox()
        assertThat(inboxItem).isInstanceOf(ShowVenueAlert::class.java)
        assertEquals("venue-id", (inboxItem as ShowVenueAlert).venueId)

        testSubject.clearItem(inboxItem)
        assertNull(testSubject.fetchInbox())
    }

    @Test
    fun testOrderOfUserInboxItems() = notReported {
        val expirationDate = LocalDate.of(2020, AUGUST, 6)
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
            hasDeclinedSharingKeys = true,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false
        )

        testSubject.addUserInboxItem(ShowVenueAlert(venueId, INFORM))
        testSubject.addUserInboxItem(ShowIsolationExpiration(expirationDate))
        testAppContext.getUnacknowledgedTestResultsProvider().add(testResult)
        testAppContext.getKeySharingInfoProvider().keySharingInfo = keySharingInfo
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now(testAppContext.clock),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now(testAppContext.clock).minusDays(2),
                    expiryDate = LocalDate.now(testAppContext.clock).plusDays(8),
                    selfAssessment = true
                )
            )
        )
        testSubject.addUserInboxItem(ShowEncounterDetection)

        val firstInboxItem = testSubject.fetchInbox()
        assertThat(firstInboxItem).isInstanceOf(ShowIsolationExpiration::class.java)
        assertEquals(expirationDate, (firstInboxItem as ShowIsolationExpiration).expirationDate)
        testSubject.clearItem(firstInboxItem)

        val secondInboxItem = testSubject.fetchInbox()
        assertThat(secondInboxItem).isInstanceOf(ShowTestResult::class.java)
        testAppContext.getTestResultHandler().acknowledge(testResult, LocalDate.now(testAppContext.clock), Overwrite)

        val thirdInboxItem = testSubject.fetchInbox()
        assertThat(thirdInboxItem).isInstanceOf(ShowEncounterDetection::class.java)
        testSubject.clearItem(thirdInboxItem as AddableUserInboxItem)

        val fourthInboxItem = testSubject.fetchInbox()
        assertThat(fourthInboxItem).isInstanceOf(ShowVenueAlert::class.java)
        assertEquals(venueId, (fourthInboxItem as ShowVenueAlert).venueId)
        testSubject.clearItem(fourthInboxItem)

        val fifthInboxItem = testSubject.fetchInbox()
        assertThat(fifthInboxItem).isInstanceOf(ShowKeySharingReminder::class.java)
        testAppContext.getKeySharingInfoProvider().reset()

        assertNull(testSubject.fetchInbox())
    }
}

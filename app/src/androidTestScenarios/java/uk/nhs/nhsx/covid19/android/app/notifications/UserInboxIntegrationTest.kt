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
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite

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
        testSubject.addUserInboxItem(ShowVenueAlert("venue-id"))
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
            Instant.now(),
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true
        )

        testSubject.addUserInboxItem(ShowVenueAlert(venueId))
        testSubject.addUserInboxItem(ShowIsolationExpiration(expirationDate))
        testAppContext.getUnacknowledgedTestResultsProvider().add(testResult)
        testSubject.addUserInboxItem(ShowEncounterDetection)

        val firstInboxItem = testSubject.fetchInbox()
        assertThat(firstInboxItem).isInstanceOf(ShowIsolationExpiration::class.java)
        assertEquals(expirationDate, (firstInboxItem as ShowIsolationExpiration).expirationDate)
        testSubject.clearItem(firstInboxItem)

        val secondInboxItem = testSubject.fetchInbox()
        assertThat(secondInboxItem).isInstanceOf(ShowTestResult::class.java)
        testAppContext.getTestResultHandler().acknowledge(testResult, Overwrite)

        val thirdInboxItem = testSubject.fetchInbox()
        assertThat(thirdInboxItem).isInstanceOf(ShowEncounterDetection::class.java)
        testSubject.clearItem(thirdInboxItem as AddableUserInboxItem)

        val fourthInboxItem = testSubject.fetchInbox()
        assertThat(fourthInboxItem).isInstanceOf(ShowVenueAlert::class.java)
        assertEquals(venueId, (fourthInboxItem as ShowVenueAlert).venueId)
        testSubject.clearItem(fourthInboxItem)

        assertNull(testSubject.fetchInbox())
    }
}

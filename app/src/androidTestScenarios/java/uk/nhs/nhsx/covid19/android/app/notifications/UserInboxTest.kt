package uk.nhs.nhsx.covid19.android.app.notifications

import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import java.time.LocalDate
import java.time.Month.AUGUST
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserInboxTest : EspressoTest() {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
    private val testSubject = AndroidUserInbox(sharedPreferences)

    @Before
    fun setUp() {
        sharedPreferences.edit().clear()
    }

    @Test
    fun testPutAndDeleteItemsInUserInbox() = notReported {
        testSubject.addUserInboxItem(ShowTestResult)
        assertThat(testSubject.fetchInbox()).isInstanceOf(ShowTestResult::class.java)
        testSubject.clearItem(ShowTestResult)
        assertNull(testSubject.fetchInbox())
    }

    @Test
    fun testFetchingItemsDoesNotDeleteThem() = notReported {
        testSubject.addUserInboxItem(ShowTestResult)
        assertThat(testSubject.fetchInbox()).isInstanceOf(ShowTestResult::class.java)
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

        testSubject.addUserInboxItem(ShowVenueAlert(venueId))
        testSubject.addUserInboxItem(ShowIsolationExpiration(expirationDate))
        testSubject.addUserInboxItem(ShowTestResult)
        testSubject.addUserInboxItem(ShowEncounterDetection)

        val firstInboxItem = testSubject.fetchInbox()
        assertThat(firstInboxItem).isInstanceOf(ShowIsolationExpiration::class.java)
        assertEquals(expirationDate, (firstInboxItem as ShowIsolationExpiration).expirationDate)
        testSubject.clearItem(firstInboxItem)

        val secondInboxItem = testSubject.fetchInbox()
        assertThat(secondInboxItem).isInstanceOf(ShowTestResult::class.java)
        testSubject.clearItem(secondInboxItem!!)

        val thirdInboxItem = testSubject.fetchInbox()
        assertThat(thirdInboxItem).isInstanceOf(ShowEncounterDetection::class.java)
        testSubject.clearItem(thirdInboxItem!!)

        val fourthInboxItem = testSubject.fetchInbox()
        assertThat(fourthInboxItem).isInstanceOf(ShowVenueAlert::class.java)
        assertEquals(venueId, (fourthInboxItem as ShowVenueAlert).venueId)
        testSubject.clearItem(fourthInboxItem)

        assertNull(testSubject.fetchInbox())
    }
}

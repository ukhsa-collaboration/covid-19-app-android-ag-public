package uk.nhs.nhsx.covid19.android.app.notifications

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserInboxTest {
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)

    private val isolationExpirationDateProvider =
        mockk<IsolationExpirationDateProvider>(relaxed = true)
    private val riskyVenueIdProvider = mockk<RiskyVenueIdProvider>(relaxed = true)
    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>(relaxed = true)

    private val testSubject = UserInbox(
        isolationExpirationDateProvider,
        riskyVenueIdProvider,
        shouldShowEncounterDetectionActivityProvider,
        unacknowledgedTestResultsProvider
    )

    @Before
    fun setUp() {
        every { isolationExpirationDateProvider.value } returns null
        every { riskyVenueIdProvider.value } returns null
        every { shouldShowEncounterDetectionActivityProvider.value } returns null
    }

    @Test
    fun `return ShowTestResult if there is unacknowledged result`() {
        val receivedTestResult = ReceivedTestResult(
            "abc",
            Instant.now(),
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true
        )
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(receivedTestResult)

        val receivedItem = testSubject.fetchInbox()
        verify { unacknowledgedTestResultsProvider.testResults }

        assertTrue(receivedItem is ShowTestResult)
    }

    @Test
    fun `return ShowIsolationExpiration if there is isolationExpirationDate`() {
        val isolationExpirationDate = "2007-12-03"
        every { isolationExpirationDateProvider.value } returns isolationExpirationDate

        val receivedItem = testSubject.fetchInbox()

        assertEquals(
            receivedItem,
            ShowIsolationExpiration(LocalDate.parse(isolationExpirationDate))
        )
    }

    @Test
    fun `return ShowEncounterDetection if there is no isolationExpirationDate and there is no unacknowledged result`() {
        every { isolationExpirationDateProvider.value } returns null
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { shouldShowEncounterDetectionActivityProvider.value } returns true

        val receivedItem = testSubject.fetchInbox()

        assertEquals(receivedItem, ShowEncounterDetection)
    }

    @Test
    fun `return ShowVenueAlert if there is only venueId`() {
        val venueId = "ID1"

        every { isolationExpirationDateProvider.value } returns null
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { shouldShowEncounterDetectionActivityProvider.value } returns false
        every { riskyVenueIdProvider.value } returns venueId

        val receivedItem = testSubject.fetchInbox()

        assertEquals(receivedItem, ShowVenueAlert(venueId))
    }

    @Test
    fun `return nothing if there is only an acknowledged result`() {
        val acknowledgedTestResult = AcknowledgedTestResult(
            "abc",
            Instant.now(),
            RelevantVirologyTestResult.POSITIVE,
            LAB_RESULT,
            Instant.now()
        )
        every { unacknowledgedTestResultsProvider.testResults } returns emptyList()
        every { relevantTestResultProvider.testResult } returns acknowledgedTestResult

        val receivedItem = testSubject.fetchInbox()

        verify { unacknowledgedTestResultsProvider.testResults }

        assertNull(receivedItem)
    }

    @Test
    fun `add item to user inbox show isolation expiration`() {
        val expirationDate = LocalDate.now()

        testSubject.addUserInboxItem(ShowIsolationExpiration(expirationDate))

        verify { isolationExpirationDateProvider setProperty "value" value eq(expirationDate.toString()) }
    }

    @Test
    fun `add item to user inbox show venue alert`() {
        val venueId = "1"

        testSubject.addUserInboxItem(ShowVenueAlert(venueId))

        verify { riskyVenueIdProvider setProperty "value" value eq(venueId) }
    }

    @Test
    fun `add item to user inbox show encounter detection`() {
        testSubject.addUserInboxItem(ShowEncounterDetection)

        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value eq(true) }
    }

    @Test
    fun `remove item to user inbox show isolation expiration`() {
        val expirationDate = LocalDate.now()

        testSubject.clearItem(ShowIsolationExpiration(expirationDate))

        verify { isolationExpirationDateProvider setProperty "value" value null }
    }

    @Test
    fun `remove item to user inbox show venue alert`() {
        val venueId = "1"

        testSubject.clearItem(ShowVenueAlert(venueId))

        verify { riskyVenueIdProvider setProperty "value" value null }
    }

    @Test
    fun `remove ShowEncounterDetection from user inbox`() {
        testSubject.clearItem(ShowEncounterDetection)

        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value null }
    }

    @Test
    fun `registerListener adds listener to a list`() {
        val listener = { }
        testSubject.registerListener(listener)

        assertEquals(1, testSubject.listeners.size)
    }

    @Test
    fun `unregisterListener removes listener from a list`() {
        val listener = { }
        testSubject.registerListener(listener)

        assertEquals(1, testSubject.listeners.size)

        testSubject.unregisterListener(listener)

        assertEquals(0, testSubject.listeners.size)
    }
}

package uk.nhs.nhsx.covid19.android.app.notifications

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider

class UserInboxTest {
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)

    private val isolationExpirationDateProvider =
        mockk<IsolationExpirationDateProvider>(relaxed = true)
    private val riskyVenueIdProvider = mockk<RiskyVenueIdProvider>(relaxed = true)
    private val riskyVenueAlertProvider = mockk<RiskyVenueAlertProvider>(relaxed = true)
    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>(relaxed = true)

    @Before
    fun setUp() {
        every { isolationExpirationDateProvider.value } returns null
        every { riskyVenueIdProvider.value } returns null
        every { riskyVenueAlertProvider.riskyVenueAlert } returns null
        every { shouldShowEncounterDetectionActivityProvider.value } returns null
    }

    @Test
    fun `migration from RiskyVenueIdProvider`() {
        every { riskyVenueIdProvider.value } returns "12345"
        every { riskyVenueAlertProvider.riskyVenueAlert } returns null

        UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        verify { riskyVenueIdProvider.value = null }
        verify { riskyVenueAlertProvider.riskyVenueAlert = RiskyVenueAlert("12345", INFORM) }
    }

    @Test
    fun `no migration from RiskyVenueIdProvider`() {
        every { riskyVenueIdProvider.value } returns null

        UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        verify(exactly = 0) { riskyVenueAlertProvider.riskyVenueAlert = any() }
        verify(exactly = 0) { riskyVenueIdProvider.value = null }
    }

    @Test
    fun `return ShowTestResult if there is unacknowledged result`() {
        val receivedTestResult = ReceivedTestResult(
            "abc",
            Instant.now(),
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(receivedTestResult)

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        val receivedItem = testSubject.fetchInbox()
        verify { unacknowledgedTestResultsProvider.testResults }

        assertTrue(receivedItem is ShowTestResult)
    }

    @Test
    fun `return ShowIsolationExpiration if there is isolationExpirationDate`() {
        val isolationExpirationDate = "2007-12-03"
        every { isolationExpirationDateProvider.value } returns isolationExpirationDate

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

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

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        val receivedItem = testSubject.fetchInbox()

        assertEquals(receivedItem, ShowEncounterDetection)
    }

    @Test
    fun `return ShowVenueAlert if there is only venueId`() {
        val riskyVenueAlert = RiskyVenueAlert("ID1", INFORM)
        val showVenueAlert = ShowVenueAlert("ID1", INFORM)

        every { isolationExpirationDateProvider.value } returns null
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { shouldShowEncounterDetectionActivityProvider.value } returns false
        every { riskyVenueAlertProvider.riskyVenueAlert } returns riskyVenueAlert

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        val receivedItem = testSubject.fetchInbox()

        assertEquals(receivedItem, showVenueAlert)
    }

    @Test
    fun `return nothing if there is only an acknowledged result`() {
        val acknowledgedTestResult = AcknowledgedTestResult(
            "abc",
            Instant.now(),
            RelevantVirologyTestResult.POSITIVE,
            LAB_RESULT,
            acknowledgedDate = Instant.now(),
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )
        every { unacknowledgedTestResultsProvider.testResults } returns emptyList()
        every { relevantTestResultProvider.testResult } returns acknowledgedTestResult

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        val receivedItem = testSubject.fetchInbox()

        verify { unacknowledgedTestResultsProvider.testResults }

        assertNull(receivedItem)
    }

    @Test
    fun `add item to user inbox show isolation expiration`() {
        val expirationDate = LocalDate.now()

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.addUserInboxItem(ShowIsolationExpiration(expirationDate))

        verify { isolationExpirationDateProvider setProperty "value" value eq(expirationDate.toString()) }
    }

    @Test
    fun `add item to user inbox show venue alert`() {
        val showVenueAlert = ShowVenueAlert("ID1", INFORM)
        val riskyVenueAlert = RiskyVenueAlert("ID1", INFORM)

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.addUserInboxItem(showVenueAlert)

        verify { riskyVenueAlertProvider.riskyVenueAlert = riskyVenueAlert }
    }

    @Test
    fun `add item to user inbox show encounter detection`() {
        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.addUserInboxItem(ShowEncounterDetection)

        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value eq(true) }
    }

    @Test
    fun `remove item to user inbox show isolation expiration`() {
        val expirationDate = LocalDate.now()

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.clearItem(ShowIsolationExpiration(expirationDate))

        verify { isolationExpirationDateProvider setProperty "value" value null }
    }

    @Test
    fun `remove item to user inbox show venue alert`() {
        val venue = ShowVenueAlert("ID1", INFORM)

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.clearItem(venue)

        verify { riskyVenueAlertProvider.riskyVenueAlert = null }
    }

    @Test
    fun `remove ShowEncounterDetection from user inbox`() {
        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.clearItem(ShowEncounterDetection)

        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value null }
    }

    @Test
    fun `registerListener adds listener to a list`() {
        val listener = { }

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.registerListener(listener)

        assertEquals(1, testSubject.listeners.size)
    }

    @Test
    fun `unregisterListener removes listener from a list`() {
        val listener = { }

        val testSubject = UserInbox(
            isolationExpirationDateProvider,
            riskyVenueIdProvider,
            riskyVenueAlertProvider,
            shouldShowEncounterDetectionActivityProvider,
            unacknowledgedTestResultsProvider
        )

        testSubject.registerListener(listener)

        assertEquals(1, testSubject.listeners.size)

        testSubject.unregisterListener(listener)

        assertEquals(0, testSubject.listeners.size)
    }
}

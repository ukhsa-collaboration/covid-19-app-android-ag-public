package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlow
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.None
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ContinueInitialKeySharing
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowKeySharingReminder
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowUnknownTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.DoNotNotify
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.Notify
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FetchUserInboxItemTest {

    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxUnitFun = true)
    private val shouldNotifyStateExpiration = mockk<ShouldNotifyStateExpiration>()
    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>(relaxUnitFun = true)
    private val shouldEnterShareKeysFlow = mockk<ShouldEnterShareKeysFlow>()
    private val riskyVenueAlertProvider = mockk<RiskyVenueAlertProvider>(relaxUnitFun = true)
    private val receivedUnknownTestResultProvider = mockk<ReceivedUnknownTestResultProvider>(relaxUnitFun = true)

    private val fetchUserInboxItem = FetchUserInboxItem(
        unacknowledgedTestResultsProvider,
        shouldNotifyStateExpiration,
        shouldShowEncounterDetectionActivityProvider,
        shouldEnterShareKeysFlow,
        riskyVenueAlertProvider,
        receivedUnknownTestResultProvider
    )

    @Before
    fun setUp() {
        every { shouldNotifyStateExpiration() } returns DoNotNotify
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { riskyVenueAlertProvider.riskyVenueAlert } returns null
        every { shouldShowEncounterDetectionActivityProvider.value } returns null
        every { shouldEnterShareKeysFlow.invoke() } returns None
        every { receivedUnknownTestResultProvider.value } returns false
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

        val receivedItem = fetchUserInboxItem()
        verify { unacknowledgedTestResultsProvider.testResults }

        assertTrue(receivedItem is ShowTestResult)
    }

    @Test
    fun `return ShowUnknownTestResult if flag is set`() {
        every { receivedUnknownTestResultProvider.value } returns true

        val receivedItem = fetchUserInboxItem()

        assertTrue(receivedItem is ShowUnknownTestResult)
    }

    @Test
    fun `do not return ShowIsolationExpiration if shouldNotifyStateExpiration returns DoNotNotify`() {
        every { shouldNotifyStateExpiration() } returns DoNotNotify

        val receivedItem = fetchUserInboxItem()

        assertNull(receivedItem)
    }

    @Test
    fun `return ShowIsolationExpiration if shouldNotifyStateExpiration returns Notify`() {
        val expiryDate = LocalDate.now()
        every { shouldNotifyStateExpiration() } returns Notify(expiryDate)

        val receivedItem = fetchUserInboxItem()

        assertEquals(
            ShowIsolationExpiration(expiryDate),
            receivedItem
        )
    }

    @Test
    fun `return ShowEncounterDetection if there is no isolation expiration date and there is no unacknowledged result`() {
        every { shouldNotifyStateExpiration() } returns DoNotNotify
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { shouldShowEncounterDetectionActivityProvider.value } returns true

        val receivedItem = fetchUserInboxItem()

        assertEquals(ShowEncounterDetection, receivedItem)
    }

    @Test
    fun `return ContinueInitialKeySharing when ShouldEnterShareKeysFlow returns Initial`() {
        every { shouldNotifyStateExpiration() } returns DoNotNotify
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { shouldShowEncounterDetectionActivityProvider.value } returns false
        every { shouldEnterShareKeysFlow.invoke() } returns ShouldEnterShareKeysFlowResult.Initial

        val receivedItem = fetchUserInboxItem()

        assertEquals(ContinueInitialKeySharing, receivedItem)
    }

    @Test
    fun `return ShowKeySharingReminder when ShouldEnterShareKeysFlow returns Reminder`() {
        every { shouldNotifyStateExpiration() } returns DoNotNotify
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { shouldShowEncounterDetectionActivityProvider.value } returns false
        every { shouldEnterShareKeysFlow.invoke() } returns ShouldEnterShareKeysFlowResult.Reminder

        val receivedItem = fetchUserInboxItem()

        assertEquals(ShowKeySharingReminder, receivedItem)
    }

    @Test
    fun `return ShowVenueAlert if there is only venueId`() {
        val riskyVenueAlert = RiskyVenueAlert("ID1", INFORM)
        val showVenueAlert = ShowVenueAlert("ID1", INFORM)

        every { shouldNotifyStateExpiration() } returns DoNotNotify
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()
        every { shouldShowEncounterDetectionActivityProvider.value } returns false
        every { riskyVenueAlertProvider.riskyVenueAlert } returns riskyVenueAlert
        every { shouldEnterShareKeysFlow.invoke() } returns None

        val receivedItem = fetchUserInboxItem()

        assertEquals(showVenueAlert, receivedItem)
    }
}

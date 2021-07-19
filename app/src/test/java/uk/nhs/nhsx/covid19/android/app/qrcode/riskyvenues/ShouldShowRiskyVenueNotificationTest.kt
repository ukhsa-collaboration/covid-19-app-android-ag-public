package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM

class ShouldShowRiskyVenueNotificationTest {

    private val riskyVenueAlertProvider = mockk<RiskyVenueAlertProvider>()

    private val testSubject = ShouldShowRiskyVenueNotification(riskyVenueAlertProvider)

    @Test
    fun `when no unacknowledged risky venue visit alert stored and receive M1 risky venue visit then should show notification`() {
        every { riskyVenueAlertProvider.riskyVenueAlert } returns null

        assertTrue(testSubject.invoke(messageTypeOfNewRiskyVenueVisit = INFORM))
    }

    @Test
    fun `when no unacknowledged risky venue visit alert stored and receive M2 risky venue visit then should show notification`() {
        every { riskyVenueAlertProvider.riskyVenueAlert } returns null

        assertTrue(testSubject.invoke(messageTypeOfNewRiskyVenueVisit = BOOK_TEST))
    }

    @Test
    fun `when unacknowledged M1 risky venue visit alert stored and receive M2 risky venue visit then should show notification`() {
        every { riskyVenueAlertProvider.riskyVenueAlert } returns riskyVenueVisitM1

        assertTrue(testSubject.invoke(messageTypeOfNewRiskyVenueVisit = BOOK_TEST))
    }

    @Test
    fun `when unacknowledged M1 risky venue visit alert stored and receive M1 risky venue visit then should not show notification`() {
        every { riskyVenueAlertProvider.riskyVenueAlert } returns riskyVenueVisitM1

        assertFalse(testSubject.invoke(messageTypeOfNewRiskyVenueVisit = INFORM))
    }

    @Test
    fun `when unacknowledged M2 risky venue visit alert stored and receive M1 risky venue visit then should not show notification`() {
        every { riskyVenueAlertProvider.riskyVenueAlert } returns riskyVenueVisitM2

        assertFalse(testSubject.invoke(messageTypeOfNewRiskyVenueVisit = INFORM))
    }

    @Test
    fun `when unacknowledged M2 risky venue visit alert stored and receive M2 risky venue visit then should not show notification`() {
        every { riskyVenueAlertProvider.riskyVenueAlert } returns riskyVenueVisitM2

        assertFalse(testSubject.invoke(messageTypeOfNewRiskyVenueVisit = BOOK_TEST))
    }

    private val riskyVenueVisitM1 = RiskyVenueAlert(id = "venue1", messageType = INFORM)
    private val riskyVenueVisitM2 = RiskyVenueAlert(id = "venue1", messageType = BOOK_TEST)
}

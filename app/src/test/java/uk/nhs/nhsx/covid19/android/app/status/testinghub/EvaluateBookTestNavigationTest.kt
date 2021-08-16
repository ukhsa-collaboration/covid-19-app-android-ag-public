package uk.nhs.nhsx.covid19.android.app.status.testinghub

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.status.testinghub.EvaluateBookTestNavigation.NavigationTarget.BookPcrTest
import kotlin.test.assertEquals

class EvaluateBookTestNavigationTest {

    private val lastVisitedBookTestTypeVenueDateProvider = mockk<LastVisitedBookTestTypeVenueDateProvider>()
    private val evaluateVenueAlertNavigation = mockk<EvaluateVenueAlertNavigation>()

    private val evaluateBookTestNavigation = EvaluateBookTestNavigation(
        lastVisitedBookTestTypeVenueDateProvider,
        evaluateVenueAlertNavigation
    )

    @Test
    fun `when user visited risky venue and was asked to book a test due to it recently then return result of EvaluateVenueAlertNavigation`() {
        val expectedNavigationTarget = EvaluateVenueAlertNavigation.NavigationTarget.SymptomsAfterRiskyVenue
        every { evaluateVenueAlertNavigation() } returns expectedNavigationTarget
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true

        assertEquals(EvaluateBookTestNavigation.NavigationTarget.SymptomsAfterRiskyVenue, evaluateBookTestNavigation())
    }

    @Test
    fun `when user did not receive a risky venue alert that asked them to book a test then return BookPcrTest`() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false

        assertEquals(BookPcrTest, evaluateBookTestNavigation())
    }
}

package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.BookATest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.SymptomsAfterRiskyVenue
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class EvaluateVenueAlertNavigationTest {

    private val fixedClock: Clock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)
    private val isolationStateMachine: IsolationStateMachine = mockk()

    private val isolationHelper = IsolationHelper(fixedClock)

    @Test
    fun `when in active index case isolation returns BookATest`() {
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.selfAssessment().asIsolation()
            .asLogical()

        assertEquals(BookATest, EvaluateVenueAlertNavigation(isolationStateMachine, fixedClock).invoke())
    }

    @Test
    fun `when not in active index case returns SymptomsAfterRiskyVenue`() {
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.contactCase().asIsolation()
            .asLogical()

        assertEquals(SymptomsAfterRiskyVenue, EvaluateVenueAlertNavigation(isolationStateMachine, fixedClock).invoke())
    }

    @Test
    fun `when expired index case returns SymptomsAfterRiskyVenue`() {
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.selfAssessment(expired = true)
            .asIsolation().asLogical()

        assertEquals(SymptomsAfterRiskyVenue, EvaluateVenueAlertNavigation(isolationStateMachine, fixedClock).invoke())
    }
}

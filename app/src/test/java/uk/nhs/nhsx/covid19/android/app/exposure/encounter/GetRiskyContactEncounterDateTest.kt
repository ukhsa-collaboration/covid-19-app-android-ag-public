package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetRiskyContactEncounterDateTest {
    private val mockIsolationStateMachine = mockk<IsolationStateMachine>()

    private val getRiskyContactEncounterDate = GetRiskyContactEncounterDate(mockIsolationStateMachine)

    @Test
    fun `returns encounter date when isolation state has contact case`() {
        val encounterDate = LocalDate.of(2021, 2, 3)
        every { mockIsolationStateMachine.readState().contact?.exposureDate } returns encounterDate

        assertEquals(encounterDate, getRiskyContactEncounterDate())
    }

    @Test
    fun `throws illegal state exception when there is no contact case`() {
        every { mockIsolationStateMachine.readState().contact } returns null

        assertNull(getRiskyContactEncounterDate())
    }
}

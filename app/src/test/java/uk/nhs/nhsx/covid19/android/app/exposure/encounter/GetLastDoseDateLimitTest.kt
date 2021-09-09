package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetLastDoseDateLimitTest {
    private val exposureDate = LocalDate.of(2021, 7, 20)
    private val getRiskyContactEncounterDate: GetRiskyContactEncounterDate = mockk()

    private val getLastDoseDateLimit = GetLastDoseDateLimit(getRiskyContactEncounterDate)

    @Test
    fun `returns date 14 whole days (15 days) back from date of encounter`() {
        val expected = LocalDate.of(2021, 7, 5)
        every { getRiskyContactEncounterDate() } returns exposureDate

        assertEquals(expected, getLastDoseDateLimit())
    }

    @Test
    fun `throws exception if there is no contact case encounter date`() {
        every { getRiskyContactEncounterDate() } returns null

        assertNull(getLastDoseDateLimit())
    }
}

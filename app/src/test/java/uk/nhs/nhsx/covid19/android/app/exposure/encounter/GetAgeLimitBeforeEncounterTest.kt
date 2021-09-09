package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetAgeLimitBeforeEncounterTest {
    private val exposureDate = LocalDate.of(2021, 8, 30)
    private val getRiskyContactEncounterDate: GetRiskyContactEncounterDate = mockk()
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()

    private val getAgeLimitBeforeEncounter =
        GetAgeLimitBeforeEncounter(getRiskyContactEncounterDate, localAuthorityPostCodeProvider)

    @Test
    fun `given an encounter date with English LA, returns a date 183 days before`() = runBlocking {
        val expected = LocalDate.of(2021, 2, 28)
        every { getRiskyContactEncounterDate() } returns exposureDate
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns PostCodeDistrict.ENGLAND

        assertEquals(expected, getAgeLimitBeforeEncounter())
    }

    @Test
    fun `given an encounter date with Welsh LA, returns the encounter date`() = runBlocking {
        val expected = exposureDate
        every { getRiskyContactEncounterDate() } returns exposureDate
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns PostCodeDistrict.WALES

        assertEquals(expected, getAgeLimitBeforeEncounter())
    }

    @Test
    fun `throws exception if can not get encounter date`() = runBlocking {
        every { getRiskyContactEncounterDate() } returns null

        assertNull(getAgeLimitBeforeEncounter())
    }
}

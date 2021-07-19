package uk.nhs.nhsx.covid19.android.app.notifications

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.util.adapters.RiskyVenueMessageTypeAdapter
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RiskyVenueAlertProviderTest {

    private val riskyVenueAlertStorage = mockk<RiskyVenueAlertStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(RiskyVenueMessageTypeAdapter()).build()

    @Test
    fun `add venue alert`() {
        every { riskyVenueAlertStorage.value } returns ""

        val testSubject = RiskyVenueAlertProvider(riskyVenueAlertStorage, moshi)
        testSubject.riskyVenueAlert = riskyVenueAlert

        verify { riskyVenueAlertStorage.value = riskyVenueAlertJson }
    }

    @Test
    fun `clear storage`() {
        every { riskyVenueAlertStorage.value } returns riskyVenueAlertJson

        val testSubject = RiskyVenueAlertProvider(riskyVenueAlertStorage, moshi)
        testSubject.riskyVenueAlert = null

        verify { riskyVenueAlertStorage.value = null }
    }

    @Test
    fun `read empty storage`() {
        every { riskyVenueAlertStorage.value } returns null

        val testSubject = RiskyVenueAlertProvider(riskyVenueAlertStorage, moshi)
        val expected = testSubject.riskyVenueAlert

        assertNull(expected)
    }

    @Test
    fun `read venue alert from storage`() {
        every { riskyVenueAlertStorage.value } returns riskyVenueAlertJson

        val testSubject = RiskyVenueAlertProvider(riskyVenueAlertStorage, moshi)
        val expected = testSubject.riskyVenueAlert

        assertEquals(riskyVenueAlert, expected)
    }

    @Test
    fun `read corrupt storage`() {
        every { riskyVenueAlertStorage.value } returns "yuefguwefguewiooewiew"

        val testSubject = RiskyVenueAlertProvider(riskyVenueAlertStorage, moshi)
        val expected = testSubject.riskyVenueAlert

        assertNull(expected)
    }

    companion object {
        val riskyVenueAlert = RiskyVenueAlert(
            id = "12345",
            messageType = INFORM
        )

        val riskyVenueAlertJson =
            """
            {"id":"12345","messageType":"M1"}
            """.trimIndent()
    }
}

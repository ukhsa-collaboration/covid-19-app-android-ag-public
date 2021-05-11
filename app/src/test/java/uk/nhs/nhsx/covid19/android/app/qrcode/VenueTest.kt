package uk.nhs.nhsx.covid19.android.app.qrcode

import org.junit.Assert.assertEquals
import org.junit.Test

class VenueTest {
    @Test
    fun `can format postcode with inward code length 2`() {
        val venue = Venue("A", "Venue1", postCode = "E17AA")
        val formattedPostCode = venue.formattedPostCode

        assertEquals(formattedPostCode, "E1 7AA")
    }

    @Test
    fun `can format postcode with inward length code 3`() {
        val venue = Venue("A", "Venue1", postCode = "EC16AA")
        val formattedPostCode = venue.formattedPostCode

        assertEquals(formattedPostCode, "EC1 6AA")
    }

    @Test
    fun `does not format postcode if length less than 5`() {
        val venue = Venue("A", "Venue1", postCode = "EC2")
        val formattedPostCode = venue.formattedPostCode

        assertEquals(formattedPostCode, "EC2")
    }

    @Test
    fun `does not format postcode if already formatted`() {
        val venue = Venue("A", "Venue1", postCode = "E4 6TJ")
        val formattedPostCode = venue.formattedPostCode

        assertEquals(formattedPostCode, "E4 6TJ")
    }
}

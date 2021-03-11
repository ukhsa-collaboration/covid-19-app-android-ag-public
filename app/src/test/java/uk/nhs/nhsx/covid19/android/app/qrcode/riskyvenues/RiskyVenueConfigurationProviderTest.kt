package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays

class RiskyVenueConfigurationProviderTest {

    private val moshi = Moshi.Builder().build()

    private val riskyVenueConfigurationStorage = mockk<RiskyVenueConfigurationStorage>(relaxed = true)

    private val testSubject = RiskyVenueConfigurationProvider(
        riskyVenueConfigurationStorage,
        moshi
    )

    @Test
    fun `verify serialization`() {
        every { riskyVenueConfigurationStorage.value } returns durationDaysJson

        val parsedDurationDays = testSubject.durationDays

        kotlin.test.assertEquals(durationDays, parsedDurationDays)
    }

    @Test
    fun `verify deserialization`() {

        testSubject.durationDays = durationDays

        verify { riskyVenueConfigurationStorage.value = durationDaysJson }
    }

    @Test
    fun `on exception will return default values`() {
        every { riskyVenueConfigurationStorage.value } returns """wrong_format"""

        val parsedDurationDays = testSubject.durationDays

        kotlin.test.assertEquals(RiskyVenueConfigurationDurationDays(), parsedDurationDays)
    }

    private val durationDays = RiskyVenueConfigurationDurationDays(
        optionToBookATest = 5
    )

    private val durationDaysJson =
        """{"optionToBookATest":5}"""
}

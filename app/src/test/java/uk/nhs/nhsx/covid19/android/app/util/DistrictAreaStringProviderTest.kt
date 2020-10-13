package uk.nhs.nhsx.covid19.android.app.util

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostalDistrictProvider
import kotlin.test.assertEquals

class DistrictAreaStringProviderTest {

    private val postalDistrictProvider = mockk<PostalDistrictProvider>()

    private val testSubject = DistrictAreaStringProvider(postalDistrictProvider)

    @Test
    fun `provide for Wales returns modified resource id`() {
        every { postalDistrictProvider.toPostalDistrict() } returns PostCodeDistrict.WALES

        val actual = testSubject.provide(R.string.privacy_notice)

        val expected = R.string.url_privacy_notice_wls

        assertEquals(expected, actual)
    }

    @Test
    fun `provide not for Wales returns original resource id`() {
        every { postalDistrictProvider.toPostalDistrict() } returns PostCodeDistrict.ENGLAND

        val actual = testSubject.provide(R.string.privacy_notice)

        val expected = R.string.privacy_notice

        assertEquals(expected, actual)
    }

    @Test
    fun `provide for Wales when no key in map returns original resource id`() {
        every { postalDistrictProvider.toPostalDistrict() } returns PostCodeDistrict.WALES

        val actual = testSubject.provide(R.string.tablet_information_url)

        val expected = R.string.tablet_information_url

        assertEquals(expected, actual)
    }

    @Test
    fun `when provided postal district is null returns original resource id`() {
        every { postalDistrictProvider.toPostalDistrict() } returns null

        val actual = testSubject.provide(R.string.privacy_notice)

        val expected = R.string.privacy_notice

        assertEquals(expected, actual)
    }
}

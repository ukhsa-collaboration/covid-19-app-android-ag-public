package uk.nhs.nhsx.covid19.android.app.util

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import kotlin.test.assertEquals

class DistrictAreaStringProviderTest {

    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()

    private val testSubject = DistrictAreaStringProvider(localAuthorityPostCodeProvider)

    @Test
    fun `provide for Wales returns modified resource id`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns PostCodeDistrict.WALES

        val actual = testSubject.provide(R.string.privacy_notice)

        val expected = R.string.url_privacy_notice_wls

        assertEquals(expected, actual)
    }

    @Test
    fun `provide not for Wales returns original resource id`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns PostCodeDistrict.ENGLAND

        val actual = testSubject.provide(R.string.privacy_notice)

        val expected = R.string.privacy_notice

        assertEquals(expected, actual)
    }

    @Test
    fun `provide for Wales when no key in map returns original resource id`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns PostCodeDistrict.WALES

        val actual = testSubject.provide(R.string.url_local_authority)

        val expected = R.string.url_local_authority

        assertEquals(expected, actual)
    }

    @Test
    fun `when provided postal district is null returns original resource id`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns null

        val actual = testSubject.provide(R.string.privacy_notice)

        val expected = R.string.privacy_notice

        assertEquals(expected, actual)
    }
}

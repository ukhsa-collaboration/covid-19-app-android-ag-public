package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.NORTHERN_IRELAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import kotlin.test.assertEquals

class PostalDistrictProviderTest {
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)

    private val testSubject = PostalDistrictProvider(sharedPreferences)

    @Test
    fun `toPostalDistrict returns England`() {
        every { sharedPreferences.all[VALUE_KEY] } returns ENGLAND.name

        val result = testSubject.toPostalDistrict()

        assertEquals(ENGLAND, result)
    }

    @Test
    fun `toPostalDistrict returns Wales`() {
        every { sharedPreferences.all[VALUE_KEY] } returns WALES.name

        val result = testSubject.toPostalDistrict()

        assertEquals(WALES, result)
    }

    @Test
    fun `toPostalDistrict returns Scotland`() {
        every { sharedPreferences.all[VALUE_KEY] } returns SCOTLAND.name

        val result = testSubject.toPostalDistrict()

        assertEquals(SCOTLAND, result)
    }

    @Test
    fun `toPostalDistrict returns Northern Ireland`() {
        every { sharedPreferences.all[VALUE_KEY] } returns NORTHERN_IRELAND.name

        val result = testSubject.toPostalDistrict()

        assertEquals(NORTHERN_IRELAND, result)
    }

    @Test
    fun `toPostalDistrict returns null`() {
        every { sharedPreferences.all[VALUE_KEY] } returns "test"

        val result = testSubject.toPostalDistrict()

        assertEquals(null, result)
    }

    @Test
    fun `postal district stored`() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor

        testSubject.storePostalDistrict(ENGLAND)

        verify { sharedPreferencesEditor.putString(VALUE_KEY, ENGLAND.name) }
    }

    @Test
    fun `postal district not stored if null`() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor

        testSubject.storePostalDistrict(null)

        verify(exactly = 0) { sharedPreferencesEditor.putString(VALUE_KEY, any()) }
    }

    companion object {
        private const val VALUE_KEY = "MAIN_POST_CODE_DISTRICT"
    }
}

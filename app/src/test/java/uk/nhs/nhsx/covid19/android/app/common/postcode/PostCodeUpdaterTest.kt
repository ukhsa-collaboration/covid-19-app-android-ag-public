package uk.nhs.nhsx.covid19.android.app.common.postcode

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.NORTHERN_IRELAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.INVALID_POST_DISTRICT
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.POST_DISTRICT_NOT_SUPPORTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.SUCCESS
import uk.nhs.nhsx.covid19.android.app.status.RiskyPostCodeIndicatorProvider
import kotlin.test.assertEquals

class PostCodeUpdaterTest {

    private val postCodeValidator = mockk<PostCodeValidator>(relaxed = true)
    private val postCodePrefs = mockk<PostCodeProvider>(relaxed = true)
    private val postCodeDistrictProvider = mockk<PostalDistrictProvider>(relaxed = true)
    private val riskyPostCodeIndicatorProvider = mockk<RiskyPostCodeIndicatorProvider>(relaxed = true)

    private val testSubject = PostCodeUpdater(
        postCodeValidator,
        postCodePrefs,
        postCodeDistrictProvider,
        riskyPostCodeIndicatorProvider
    )

    @Test
    fun `post code lower case valid`() = runBlocking {
        coEvery { postCodeValidator.validate(any()) } returns ENGLAND

        val postCodeLowerCase = "cm1"
        val postCodeUpdateState = testSubject.update(postCodeLowerCase)

        coVerify { postCodeValidator.validate(postCodeLowerCase.toUpperCase()) }
        verify { postCodePrefs setProperty "value" value eq(postCodeLowerCase.toUpperCase()) }
        verify { riskyPostCodeIndicatorProvider.clear() }
        assertEquals(postCodeUpdateState, SUCCESS)
    }

    @Test
    fun `post code upper case valid`() = runBlocking {
        coEvery { postCodeValidator.validate(any()) } returns ENGLAND

        val postCode = "CM1"
        val postCodeUpdateState = testSubject.update(postCode)

        coVerify { postCodeValidator.validate(postCode) }
        verify { postCodePrefs setProperty "value" value eq(postCode) }
        verify { riskyPostCodeIndicatorProvider.clear() }
        assertEquals(postCodeUpdateState, SUCCESS)
    }

    @Test
    fun `post code invalid`() = runBlocking {
        coEvery { postCodeValidator.validate(any()) } returns null

        val postCode = "CM1"
        val postCodeUpdateState = testSubject.update(postCode)

        coVerify { postCodeValidator.validate(postCode) }
        verify(exactly = 0) { postCodePrefs setProperty "value" value eq(postCode) }
        verify(exactly = 0) { riskyPostCodeIndicatorProvider.clear() }
        assertEquals(postCodeUpdateState, INVALID_POST_DISTRICT)
    }

    @Test
    fun `scotland post code not supported`() = runBlocking {
        coEvery { postCodeValidator.validate(any()) } returns SCOTLAND

        val postCode = "AB1"
        val postCodeUpdateState = testSubject.update(postCode)

        coVerify { postCodeValidator.validate(postCode) }
        assertEquals(postCodeUpdateState, POST_DISTRICT_NOT_SUPPORTED)
    }

    @Test
    fun `northern ireland post code not supported`() = runBlocking {
        coEvery { postCodeValidator.validate(any()) } returns NORTHERN_IRELAND

        val postCode = "BT1"
        val postCodeUpdateState = testSubject.update(postCode)

        coVerify { postCodeValidator.validate(postCode) }
        assertEquals(postCodeUpdateState, POST_DISTRICT_NOT_SUPPORTED)
    }
}

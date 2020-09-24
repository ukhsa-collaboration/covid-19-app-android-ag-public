/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.common.postcode

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.NORTHERN_IRELAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PostCodeValidatorTest {

    private val postCodeLoader = mockk<PostCodeLoader>(relaxed = true)

    private val validator =
        PostCodeValidator(
            postCodeLoader
        )

    @Test
    fun emptyPostCode() = runBlocking {
        coEvery { postCodeLoader.loadPostCodes() } returns mapOf()

        val postCodeDistrict = validator.validate("")

        assertEquals(null, postCodeDistrict)
    }

    @Test
    fun invalidPostCode() = runBlocking {
        coEvery { postCodeLoader.loadPostCodes() } returns provideMapOfDistrictsWithPostCodes()

        val postCodeDistrict = validator.validate("AAA")

        assertEquals(null, postCodeDistrict)
    }

    @Test
    fun validEnglishPostCodePrefix() = runBlocking {
        coEvery { postCodeLoader.loadPostCodes() } returns provideMapOfDistrictsWithPostCodes()

        val postCodeDistrict = validator.validate("ZE1")

        assertEquals(ENGLAND, postCodeDistrict)
    }

    @Test
    fun validWalesPostCodePrefix() = runBlocking {
        coEvery { postCodeLoader.loadPostCodes() } returns provideMapOfDistrictsWithPostCodes()

        val postCode = "WW1"
        val postCodeDistrict = validator.validate(postCode)

        assertEquals(WALES, postCodeDistrict)
    }

    @Test
    fun validScotlandPostCodePrefix() = runBlocking {
        coEvery { postCodeLoader.loadPostCodes() } returns provideMapOfDistrictsWithPostCodes()

        val postCode = "SC1"
        val postCodeDistrict = validator.validate(postCode)

        assertEquals(SCOTLAND, postCodeDistrict)
    }

    @Test
    fun validNorthIrelandPostCodePrefix() = runBlocking {
        coEvery { postCodeLoader.loadPostCodes() } returns provideMapOfDistrictsWithPostCodes()

        val postCode = "NI1"
        val postCodeDistrict = validator.validate(postCode)

        assertEquals(NORTHERN_IRELAND, postCodeDistrict)
    }

    @Test
    fun northIrelandPostCodePrefixNotValidAsEnglandPrefix() = runBlocking {
        coEvery { postCodeLoader.loadPostCodes() } returns provideMapOfDistrictsWithPostCodes()

        val postCode = "NI1"
        val postCodeDistrict = validator.validate(postCode)

        assertNotEquals(ENGLAND, postCodeDistrict)
    }

    private fun provideMapOfDistrictsWithPostCodes(): Map<String, List<String>> {
        return mapOf(
            ENGLAND.name to listOf("ZE1", "ZE2", "ZE3"),
            WALES.name to listOf("WW1", "WW2", "WW3"),
            SCOTLAND.name to listOf("SC1", "SC2", "SC3"),
            NORTHERN_IRELAND.name to listOf("NI1", "NI2")
        )
    }
}

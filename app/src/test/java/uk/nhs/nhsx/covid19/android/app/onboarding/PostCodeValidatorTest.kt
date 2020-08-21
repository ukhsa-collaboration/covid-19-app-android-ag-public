/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.onboarding

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeLoader
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeValidator
import kotlin.test.assertEquals

class PostCodeValidatorTest {

    private val postCodeLoader = mockk<PostCodeLoader>(relaxed = true)
    private val validator =
        PostCodeValidator(
            postCodeLoader
        )

    @Test
    fun emptyPostCode() = runBlocking {
        coEvery { postCodeLoader.readListFromJson() } returns listOf()

        val isValid = validator.validate("")

        assertEquals(false, isValid)
    }

    @Test
    fun invalidPostCode() = runBlocking {
        coEvery { postCodeLoader.readListFromJson() } returns provideListOfPostCodes()

        val isValid = validator.validate("AAA")

        assertEquals(false, isValid)
    }

    @Test
    fun validPostCodePrefix() = runBlocking {
        coEvery { postCodeLoader.readListFromJson() } returns provideListOfPostCodes()

        val isValid = validator.validate("ZE1")

        assertEquals(true, isValid)
    }

    private fun provideListOfPostCodes(): List<String> {
        return listOf("ZE1", "ZE2", "ZE3")
    }
}

/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.covid19.android.app.onboarding

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeValidator

class PostCodeValidatorTest {

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val validator =
        PostCodeValidator(
            postCodeProvider
        )

    @Test
    fun emptyPostCode() {
        val isValid = validator.validate("")

        assertThat(isValid).isFalse()

        verifyAll {
            postCodeProvider wasNot Called
        }
    }

    @Test
    fun invalidPostCode() {
        val isValid = validator.validate("A")

        assertThat(isValid).isFalse()

        verifyAll {
            postCodeProvider wasNot Called
        }
    }

    @Test
    fun validPostCodePrefix() {
        val isValid = validator.validate("SW15")

        assertThat(isValid).isTrue()

        verifyAll {
            postCodeProvider.value = ("SW15")
        }
    }
}

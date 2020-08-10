package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCodeValidator @Inject constructor(
    private val postCodeProvider: PostCodeProvider
) {
    private val postCodeRegex = Regex("^[A-Z]{1,2}[0-9R][0-9A-Z]?$")

    fun validate(postCode: String): Boolean {
        val isValid = postCodeRegex.matches(postCode)

        if (isValid) {
            postCodeProvider.value = postCode
        }

        return isValid
    }
}

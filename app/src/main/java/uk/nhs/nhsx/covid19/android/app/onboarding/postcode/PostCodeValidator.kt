package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCodeValidator @Inject constructor(
    private val postCodeLoader: PostCodeLoader
) {
    suspend fun validate(postCode: String): Boolean {
        return postCodeLoader.readListFromJson()?.any { it == postCode } ?: false
    }
}

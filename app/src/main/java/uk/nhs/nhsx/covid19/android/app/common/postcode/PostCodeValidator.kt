package uk.nhs.nhsx.covid19.android.app.common.postcode

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCodeValidator @Inject constructor(
    private val postCodeLoader: PostCodeLoader
) {
    suspend fun validate(postCode: String): PostCodeDistrict? {
        val postCodeDistrict =
            postCodeLoader.loadPostCodes().toList()
                .firstOrNull { postCode.trim() in it.second }?.first
        return PostCodeDistrict.fromString(postCodeDistrict)
    }
}

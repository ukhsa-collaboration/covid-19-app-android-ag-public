package uk.nhs.nhsx.covid19.android.app.common.postcode

import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAuthorityPostCodeProvider @Inject constructor(
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader
) {

    suspend fun getPostCodeDistrict(): PostCodeDistrict? {
        val localAuthorityId = localAuthorityProvider.value ?: return null

        val postCodeDistrictAsString = localAuthorityPostCodesLoader.load()?.localAuthorities?.get(localAuthorityId)?.country

        return PostCodeDistrict.fromString(postCodeDistrictAsString)
    }

    suspend fun requirePostCodeDistrict(): PostCodeDistrict {
        when (val postCodeDistrict = getPostCodeDistrict()) {
            ENGLAND, WALES -> return postCodeDistrict
            else -> throw IllegalStateException("The post code district is not England or Wales")
        }
    }

    suspend fun isWelshDistrict() = getPostCodeDistrict() == WALES
}

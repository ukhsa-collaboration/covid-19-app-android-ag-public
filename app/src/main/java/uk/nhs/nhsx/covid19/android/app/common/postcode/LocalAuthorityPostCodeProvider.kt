package uk.nhs.nhsx.covid19.android.app.common.postcode

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
}

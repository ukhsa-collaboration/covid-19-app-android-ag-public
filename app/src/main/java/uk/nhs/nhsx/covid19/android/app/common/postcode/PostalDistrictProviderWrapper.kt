package uk.nhs.nhsx.covid19.android.app.common.postcode

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import javax.inject.Inject

class PostalDistrictProviderWrapper @Inject constructor(
    private val postalDistrictProvider: PostalDistrictProvider,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) {

    suspend fun getPostCodeDistrict(): PostCodeDistrict? =
        if (RuntimeBehavior.isFeatureEnabled(FeatureFlag.LOCAL_AUTHORITY)) {
            localAuthorityPostCodeProvider.getPostCodeDistrict()
        } else {
            postalDistrictProvider.toPostalDistrict()
        }
}

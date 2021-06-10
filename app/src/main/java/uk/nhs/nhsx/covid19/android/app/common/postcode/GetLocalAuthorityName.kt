package uk.nhs.nhsx.covid19.android.app.common.postcode

import javax.inject.Inject

class GetLocalAuthorityName @Inject constructor(
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader
) {
    suspend operator fun invoke(): String? {
        val localAuthorityId = localAuthorityProvider.value ?: return null
        return localAuthorityPostCodesLoader.load()?.localAuthorities?.get(localAuthorityId)?.name
    }
}

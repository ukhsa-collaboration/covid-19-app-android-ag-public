package uk.nhs.nhsx.covid19.android.app.common.postcode

import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.ParseJsonError
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Unsupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAuthorityPostCodeValidator @Inject constructor(
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader
) {

    suspend fun validate(postCode: String): LocalAuthorityPostCodeValidationResult {
        val postCodeCleaned = postCode.toUpperCase(Locale.UK).trim()

        if (postCodeCleaned.isEmpty()) return Invalid

        val postDistrictAndLocalAuthorityMap = localAuthorityPostCodesLoader.load() ?: return ParseJsonError

        val localAuthorityIdsForPostDistrict =
            postDistrictAndLocalAuthorityMap.postcodes[postCodeCleaned] ?: return Invalid

        val localAuthorities: List<LocalAuthorityWithId> = localAuthorityIdsForPostDistrict
            .mapNotNull { localAuthorityId ->
                postDistrictAndLocalAuthorityMap.localAuthorities[localAuthorityId]?.let {
                    LocalAuthorityWithId(localAuthorityId, it)
                }
            }

        val postCodeSupported =
            localAuthorities.any { it.localAuthority.supported() }

        return if (postCodeSupported) Valid(postCodeCleaned, localAuthorities) else Unsupported
    }

    sealed class LocalAuthorityPostCodeValidationResult {
        data class Valid(val postCode: String, val localAuthorities: List<LocalAuthorityWithId>) : LocalAuthorityPostCodeValidationResult()
        object ParseJsonError : LocalAuthorityPostCodeValidationResult()
        object Invalid : LocalAuthorityPostCodeValidationResult()
        object Unsupported : LocalAuthorityPostCodeValidationResult()
    }
}

data class LocalAuthorityWithId(
    val id: String,
    val localAuthority: LocalAuthority
)

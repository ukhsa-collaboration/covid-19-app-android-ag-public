package uk.nhs.nhsx.covid19.android.app.common.postcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.NORTHERN_IRELAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.InvalidPostDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.PostDistrictNotSupported
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.Success
import uk.nhs.nhsx.covid19.android.app.status.RiskyPostCodeIndicatorProvider
import java.util.Locale
import javax.inject.Inject

class PostCodeUpdater @Inject constructor(
    private val postCodeValidator: PostCodeValidator,
    private val postCodeProvider: PostCodeProvider,
    private val postalDistrictProvider: PostalDistrictProvider,
    private val riskyPostCodeIndicatorProvider: RiskyPostCodeIndicatorProvider
) {
    suspend fun update(postCode: String): PostCodeUpdateState = withContext(Dispatchers.IO) {
        val postCodeUpperCased = postCode.toUpperCase(Locale.UK).trim()
        val postCodeDistrict = postCodeValidator.validate(postCodeUpperCased)

        postCodeDistrict?.let {
            if (postCodeDistrict == NORTHERN_IRELAND || postCodeDistrict == SCOTLAND) {
                return@withContext PostDistrictNotSupported
            }

            postCodeProvider.value = postCodeUpperCased
            postalDistrictProvider.storePostalDistrict(postCodeDistrict)
            riskyPostCodeIndicatorProvider.clear()

            return@withContext Success(postCodeUpperCased)
        }

        return@withContext InvalidPostDistrict
    }

    sealed class PostCodeUpdateState {
        data class Success(val postCode: String) : PostCodeUpdateState()
        object InvalidPostDistrict : PostCodeUpdateState()
        object PostDistrictNotSupported : PostCodeUpdateState()
    }
}

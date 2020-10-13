package uk.nhs.nhsx.covid19.android.app.common.postcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.NORTHERN_IRELAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.INVALID_POST_DISTRICT
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.POST_DISTRICT_NOT_SUPPORTED
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.SUCCESS
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
                return@withContext POST_DISTRICT_NOT_SUPPORTED
            }

            postCodeProvider.value = postCodeUpperCased
            postalDistrictProvider.storePostalDistrict(postCodeDistrict)
            riskyPostCodeIndicatorProvider.clear()

            return@withContext SUCCESS
        }

        return@withContext INVALID_POST_DISTRICT
    }

    enum class PostCodeUpdateState {
        SUCCESS, INVALID_POST_DISTRICT, POST_DISTRICT_NOT_SUPPORTED
    }
}

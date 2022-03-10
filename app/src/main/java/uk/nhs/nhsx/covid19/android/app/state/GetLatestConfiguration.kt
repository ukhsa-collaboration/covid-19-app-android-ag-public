package uk.nhs.nhsx.covid19.android.app.state

import kotlinx.coroutines.runBlocking
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import javax.inject.Inject

class GetLatestConfiguration @Inject constructor(
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider
) {
    operator fun invoke(): CountrySpecificConfiguration {
        val durationDays = isolationConfigurationProvider.durationDays
        return runBlocking {
            val countrySpecificConfiguration =
                when (localAuthorityPostCodeProvider.requirePostCodeDistrict()) {
                    ENGLAND -> durationDays.england
                    WALES -> durationDays.wales
                    else -> throw IllegalStateException("The post code district is not England or Wales")
                }
            countrySpecificConfiguration
        }
    }
}

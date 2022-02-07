package uk.nhs.nhsx.covid19.android.app.localstats

import uk.nhs.nhsx.covid19.android.app.common.postcode.GetLocalAuthorityName
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsResponse
import java.lang.IllegalStateException
import javax.inject.Inject

class LocalStatsMapper @Inject constructor(
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val getLocalAuthorityName: GetLocalAuthorityName
) {
    suspend fun map(localStatsResponse: LocalStatsResponse): LocalStats {
        val postCodeDistrict = localAuthorityPostCodeProvider.requirePostCodeDistrict()
        val countryRollingRateLastUpdate = getCountryRollingRateLastUpdate(postCodeDistrict, localStatsResponse)
        val getCountryRollingRate = getCountryRollingRate(postCodeDistrict, localStatsResponse)

        val localAuthority = localStatsResponse.localAuthorities[localAuthorityProvider.value]
            ?: throw IllegalStateException("Current local authority is not present")

        return LocalStats(
            postCodeDistrict = postCodeDistrict,
            localAuthorityName = getLocalAuthorityName() ?: "",
            lastFetch = localStatsResponse.lastFetch,
            countryNewCasesBySpecimenDateRollingRateLastUpdate = countryRollingRateLastUpdate,
            localAuthorityNewCasesBySpecimenDateRollingRateLastUpdate = localStatsResponse.metadata.localAuthorities.newCasesBySpecimenDateRollingRate.lastUpdate,
            localAuthorityNewCasesByPublishDateLastUpdate = localStatsResponse.metadata.localAuthorities.newCasesByPublishDate.lastUpdate,
            countryNewCasesBySpecimenDateRollingRate = getCountryRollingRate,
            localAuthorityData = localAuthority
        )
    }

    private fun getCountryRollingRate(
        postCodeDistrict: PostCodeDistrict,
        localStatsResponse: LocalStatsResponse
    ) = if (postCodeDistrict == ENGLAND) {
        localStatsResponse.england.newCasesBySpecimenDateRollingRate
    } else {
        localStatsResponse.wales.newCasesBySpecimenDateRollingRate
    }

    private fun getCountryRollingRateLastUpdate(
        postCodeDistrict: PostCodeDistrict,
        localStatsResponse: LocalStatsResponse
    ) = if (postCodeDistrict == ENGLAND) {
        localStatsResponse.metadata.england.newCasesBySpecimenDateRollingRate
    } else {
        localStatsResponse.metadata.wales.newCasesBySpecimenDateRollingRate
    }.lastUpdate
}

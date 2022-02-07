package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.CountryData
import uk.nhs.nhsx.covid19.android.app.remote.data.CountryMetadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Direction.UP
import uk.nhs.nhsx.covid19.android.app.remote.data.LastUpdateDate
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalAuthoritiesMetadata
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalAuthorityData
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsMetadata
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsResponse
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class MockLocalStatsApi @Inject constructor(val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader) :
    LocalStatsApi {

    override suspend fun fetchLocalStats() = MockApiModule.behaviour.invoke {
        val localAuthorities = localAuthorityPostCodesLoader.load()!!.localAuthorities.keys
        response.copy(localAuthorities = localAuthorities.associateWith { localAuthorityData })
    }

    companion object {
        private val lastFetch: Instant = Instant.parse("2021-11-24T16:02:31Z")
        private const val englandRollingRateLastUpdate = "2021-11-23"
        private const val walesRollingRateLastUpdate = "2021-11-22"
        private const val localAuthorityRollingRateLastUpdate = "2021-11-21"
        private const val localAuthorityNewCasesLastUpdate = "2021-11-20"
        private val englandNewCasesRollingRate = BigDecimal("234.22")
        private val walesNewCasesRollingRate = BigDecimal("221.11")
        private val localAuthorityData = LocalAuthorityData(
            newCasesByPublishDateRollingSum = 771,
            newCasesByPublishDateChange = 207,
            newCasesByPublishDateDirection = UP,
            newCasesByPublishDate = 105,
            newCasesByPublishDateChangePercentage = BigDecimal("36.7"),
            newCasesBySpecimenDateRollingRate = BigDecimal("289.5")
        )

        val response = LocalStatsResponse(
            lastFetch = lastFetch,
            metadata = LocalStatsMetadata(
                england = CountryMetadata(lastUpdate(englandRollingRateLastUpdate)),
                wales = CountryMetadata(lastUpdate(walesRollingRateLastUpdate)),
                localAuthorities = LocalAuthoritiesMetadata(
                    newCasesByPublishDate = lastUpdate(localAuthorityNewCasesLastUpdate),
                    newCasesBySpecimenDateRollingRate = lastUpdate(localAuthorityRollingRateLastUpdate),
                )
            ),
            england = CountryData(newCasesBySpecimenDateRollingRate = englandNewCasesRollingRate),
            wales = CountryData(newCasesBySpecimenDateRollingRate = walesNewCasesRollingRate),
            localAuthorities = mapOf()
        )

        private fun lastUpdate(date: String) = LastUpdateDate(localDate(date))
        private fun localDate(englandRollingRateLastUpdate: String) = LocalDate.parse(englandRollingRateLastUpdate)
    }
}

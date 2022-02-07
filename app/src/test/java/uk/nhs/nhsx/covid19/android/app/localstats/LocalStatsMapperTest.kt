package uk.nhs.nhsx.covid19.android.app.localstats

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.GetLocalAuthorityName
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.CountryData
import uk.nhs.nhsx.covid19.android.app.remote.data.CountryMetadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Direction.UP
import uk.nhs.nhsx.covid19.android.app.remote.data.LastUpdateDate
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalAuthoritiesMetadata
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalAuthorityData
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsMetadata
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsResponse
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.fail

class LocalStatsMapperTest {

    @Test
    fun `map local stats response to local stats object for England`() = runBlocking {
        val localAuthorityProvider = mockk<LocalAuthorityProvider>()
        every { localAuthorityProvider.value } returns "E06000037"

        val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND

        val getLocalAuthorityName = mockk<GetLocalAuthorityName>()
        coEvery { getLocalAuthorityName() } returns "West Berkshire"

        val testSubject =
            LocalStatsMapper(localAuthorityPostCodeProvider, localAuthorityProvider, getLocalAuthorityName)

        val actual = testSubject.map(localStatsResponse)

        val expected = LocalStats(
            postCodeDistrict = ENGLAND,
            localAuthorityName = "West Berkshire",
            lastFetch = lastFetch,
            countryNewCasesBySpecimenDateRollingRateLastUpdate = localDate(englandRollingRateLastUpdate),
            localAuthorityNewCasesBySpecimenDateRollingRateLastUpdate = localDate(localAuthorityRollingRateLastUpdate),
            localAuthorityNewCasesByPublishDateLastUpdate = localDate(localAuthorityNewCasesLastUpdate),
            countryNewCasesBySpecimenDateRollingRate = englandNewCasesRollingRate,
            localAuthorityDataForE06000037
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `map local stats response to local stats object for Wales`() = runBlocking {
        val localAuthorityProvider = mockk<LocalAuthorityProvider>()
        every { localAuthorityProvider.value } returns "E06000037"

        val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        val getLocalAuthorityName = mockk<GetLocalAuthorityName>()
        coEvery { getLocalAuthorityName() } returns "West Berkshire"

        val testSubject =
            LocalStatsMapper(localAuthorityPostCodeProvider, localAuthorityProvider, getLocalAuthorityName)

        val actual = testSubject.map(localStatsResponse)

        val expected = LocalStats(
            postCodeDistrict = WALES,
            localAuthorityName = "West Berkshire",
            lastFetch = lastFetch,
            countryNewCasesBySpecimenDateRollingRateLastUpdate = localDate(walesRollingRateLastUpdate),
            localAuthorityNewCasesBySpecimenDateRollingRateLastUpdate = localDate(localAuthorityRollingRateLastUpdate),
            localAuthorityNewCasesByPublishDateLastUpdate = localDate(localAuthorityNewCasesLastUpdate),
            countryNewCasesBySpecimenDateRollingRate = walesNewCasesRollingRate,
            localAuthorityDataForE06000037
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `map local stats response to local stats object for non-existent`() = runBlocking {
        val localAuthorityProvider = mockk<LocalAuthorityProvider>()
        every { localAuthorityProvider.value } returns "E07000099"

        val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        val getLocalAuthorityName = mockk<GetLocalAuthorityName>()
        coEvery { getLocalAuthorityName() } returns "West Berkshire"

        val testSubject =
            LocalStatsMapper(localAuthorityPostCodeProvider, localAuthorityProvider, getLocalAuthorityName)

        try {
            testSubject.map(localStatsResponse)
            fail("No IllegalStateException was thrown")
        } catch (exception: IllegalStateException) {
            assertEquals("Current local authority is not present", exception.message)
        } catch (throwable: Throwable) {
            fail("Wrong exception type. Expected IllegalStateException but got ${throwable.javaClass}")
        }
    }

    companion object {
        val lastFetch: Instant = Instant.parse("2021-11-24T00:00:00Z")
        const val englandRollingRateLastUpdate = "2021-11-23"
        const val walesRollingRateLastUpdate = "2021-11-21"
        const val localAuthorityRollingRateLastUpdate = "2021-11-21"
        const val localAuthorityNewCasesLastUpdate = "2021-11-20"
        val englandNewCasesRollingRate = BigDecimal("234.22")
        val walesNewCasesRollingRate = BigDecimal("221.11")
        val localAuthorityDataForE06000037 = LocalAuthorityData(
            newCasesByPublishDateRollingSum = -771,
            newCasesByPublishDateChange = 207,
            newCasesByPublishDateDirection = UP,
            newCasesByPublishDate = 105,
            newCasesByPublishDateChangePercentage = BigDecimal("36.7"),
            newCasesBySpecimenDateRollingRate = BigDecimal("289.5")
        )
        val localAuthorityDataForE07000099 = LocalAuthorityData(
            newCasesByPublishDateRollingSum = null,
            newCasesByPublishDateChange = null,
            newCasesByPublishDateDirection = null,
            newCasesByPublishDate = null,
            newCasesByPublishDateChangePercentage = null,
            newCasesBySpecimenDateRollingRate = null
        )
        val localStatsResponse = LocalStatsResponse(
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
            localAuthorities = mapOf(
                "E06000037" to localAuthorityDataForE06000037
            )
        )

        private fun lastUpdate(date: String) = LastUpdateDate(localDate(date))
        private fun localDate(englandRollingRateLastUpdate: String) = LocalDate.parse(englandRollingRateLastUpdate)
    }
}

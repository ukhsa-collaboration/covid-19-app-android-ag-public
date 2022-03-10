package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays

class GetLatestConfigurationTest {
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val durationDays = mockk<DurationDays>()
    private val englishConfiguration = mockk<CountrySpecificConfiguration>()
    private val welshConfiguration = mockk<CountrySpecificConfiguration>()

    private val testSubject = GetLatestConfiguration(localAuthorityPostCodeProvider, isolationConfigurationProvider)

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
        every { durationDays.wales } returns welshConfiguration
        every { durationDays.england } returns englishConfiguration
    }

    @Test
    fun `returns English configuration when in England`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND

        val result = testSubject()

        assertEquals(englishConfiguration, result)
    }

    @Test
    fun `returns Welsh configuration when in Wales`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        val result = testSubject()

        assertEquals(welshConfiguration, result)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws exception when country is not England or Wales`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns SCOTLAND

        testSubject()
    }
}

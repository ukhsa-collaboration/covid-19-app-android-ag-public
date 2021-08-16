package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider

class UpdateConfigurationsTest {

    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val isolationConfigurationApi = mockk<IsolationConfigurationApi>()
    private val riskyVenueConfigurationProvider = mockk<RiskyVenueConfigurationProvider>()
    private val riskyVenueConfigurationApi = mockk<RiskyVenueConfigurationApi>()

    private val updateConfigurations = UpdateConfigurations(
        isolationConfigurationProvider,
        isolationConfigurationApi,
        riskyVenueConfigurationProvider,
        riskyVenueConfigurationApi
    )

    private val expectedIsolationDurationDays = mockk<DurationDays>()
    private val isolationConfigurationResponse = IsolationConfigurationResponse(expectedIsolationDurationDays)
    private val expectedRiskyVenueDurationDays = mockk<RiskyVenueConfigurationDurationDays>()
    private val riskyVenueConfigurationResponse = RiskyVenueConfigurationResponse(expectedRiskyVenueDurationDays)

    @Before
    fun setUp() {
        coEvery { isolationConfigurationApi.getIsolationConfiguration() } returns isolationConfigurationResponse
        coEvery { riskyVenueConfigurationApi.getRiskyVenueConfiguration() } returns riskyVenueConfigurationResponse
    }

    @Test
    fun `when update configurations is called then store api response in providers`() = runBlocking {
        updateConfigurations()

        verify {
            isolationConfigurationProvider setProperty "durationDays" value eq(expectedIsolationDurationDays)
            riskyVenueConfigurationProvider setProperty "durationDays" value eq(expectedRiskyVenueDurationDays)
        }
    }

    @Test
    fun `when update configurations is called and fetching the isolation configuration api call fails then still update risky venue configuration`() =
        runBlocking {
            coEvery { isolationConfigurationApi.getIsolationConfiguration() } throws Exception()

            updateConfigurations()

            verify { riskyVenueConfigurationProvider setProperty "durationDays" value eq(expectedRiskyVenueDurationDays) }
            confirmVerified(isolationConfigurationProvider)
        }

    @Test
    fun `when update configurations is called and fetching the risky venue configuration api call fails then still update isolation configuration`() =
        runBlocking {
            coEvery { riskyVenueConfigurationApi.getRiskyVenueConfiguration() } throws Exception()

            updateConfigurations()

            verify { isolationConfigurationProvider setProperty "durationDays" value eq(expectedIsolationDurationDays) }
            confirmVerified(riskyVenueConfigurationProvider)
        }
}

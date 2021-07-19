package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM

class MigrateRiskyVenueIdProviderTest {

    private val riskyVenueIdProvider = mockk<RiskyVenueIdProvider>(relaxUnitFun = true)
    private val riskyVenueAlertProvider = mockk<RiskyVenueAlertProvider>(relaxUnitFun = true)

    private val migrateRiskyVenueIdProvider = MigrateRiskyVenueIdProvider(riskyVenueIdProvider, riskyVenueAlertProvider)

    @Test
    fun `perform migration if RiskyVenueIdProvider is not empty`() {
        every { riskyVenueIdProvider.value } returns "12345"

        migrateRiskyVenueIdProvider()

        verify { riskyVenueIdProvider setProperty "value" value null }
        verify { riskyVenueAlertProvider.riskyVenueAlert = RiskyVenueAlert("12345", INFORM) }
    }

    @Test
    fun `do not perform migration if RiskyVenueIdProvider is empty`() {
        every { riskyVenueIdProvider.value } returns null

        migrateRiskyVenueIdProvider()

        verify(exactly = 0) { riskyVenueIdProvider setProperty "value" value null }
        verify(exactly = 0) { riskyVenueAlertProvider.riskyVenueAlert = any() }
    }
}

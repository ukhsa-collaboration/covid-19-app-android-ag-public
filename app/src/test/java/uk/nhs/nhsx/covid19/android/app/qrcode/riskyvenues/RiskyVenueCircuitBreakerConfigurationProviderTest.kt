package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueCircuitBreakerConfigurationProvider.Companion.RISKY_VENUE_POLLING_CONFIGURATION_JSON_KEY
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RiskyVenueCircuitBreakerConfigurationProviderTest : ProviderTest<RiskyVenueCircuitBreakerConfigurationProvider, List<RiskyVenueCircuitBreakerConfiguration>>() {

    override val getTestSubject = ::RiskyVenueCircuitBreakerConfigurationProvider
    override val property = RiskyVenueCircuitBreakerConfigurationProvider::configs
    override val key = RISKY_VENUE_POLLING_CONFIGURATION_JSON_KEY
    override val defaultValue: List<RiskyVenueCircuitBreakerConfiguration> = emptyList()
    override val expectations: List<ProviderTestExpectation<List<RiskyVenueCircuitBreakerConfiguration>>> =
        listOf(
            ProviderTestExpectation(json = MULTIPLE_POLLING_CONFIGURATIONS_JSON, objectValue = multiplePollingConfigs)
        )

    @Test
    fun `test adding polling config`() {
        sharedPreferencesReturns(SINGLE_POLLING_CONFIGURATION_JSON)

        testSubject.add(pollingConfig)

        assertSharedPreferenceSetsValue(MULTIPLE_POLLING_CONFIGURATIONS_JSON)
    }

    @Test
    fun `test adding multiple polling configs`() {
        sharedPreferencesReturns("[]")

        testSubject.addAll(multiplePollingConfigs)

        assertSharedPreferenceSetsValue(MULTIPLE_POLLING_CONFIGURATIONS_JSON)
    }

    @Test
    fun `test removing polling config`() {
        sharedPreferencesReturns(MULTIPLE_POLLING_CONFIGURATIONS_JSON)

        testSubject.remove(pollingConfig)

        assertSharedPreferenceSetsValue(SINGLE_POLLING_CONFIGURATION_JSON)
    }

    companion object {
        private val fixedClock =
            Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

        private val pollingConfig = RiskyVenueCircuitBreakerConfiguration(
            startedAt = Instant.now(fixedClock),
            venueId = "venue2",
            approvalToken = "token2",
            messageType = BOOK_TEST
        )

        private val multiplePollingConfigs =
            listOf(
                RiskyVenueCircuitBreakerConfiguration(
                    startedAt = Instant.now(fixedClock),
                    venueId = "venue1",
                    approvalToken = "token1",
                    messageType = INFORM
                ),
                pollingConfig
            )

        private val SINGLE_POLLING_CONFIGURATION_JSON =
            """
            [{"startedAt":"2020-07-28T01:00:00Z","venueId":"venue1","approvalToken":"token1","isPolling":true,"messageType":"M1"}]
            """.trim()

        private val MULTIPLE_POLLING_CONFIGURATIONS_JSON =
            """
            [{"startedAt":"2020-07-28T01:00:00Z","venueId":"venue1","approvalToken":"token1","isPolling":true,"messageType":"M1"},{"startedAt":"2020-07-28T01:00:00Z","venueId":"venue2","approvalToken":"token2","isPolling":true,"messageType":"M2"}]
            """.trim()
    }
}

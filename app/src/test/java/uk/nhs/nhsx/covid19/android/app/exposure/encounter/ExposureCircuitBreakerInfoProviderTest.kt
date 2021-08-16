package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfoProvider.Companion.EXPOSURE_CIRCUIT_BREAKER_INFO_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ExposureCircuitBreakerInfoProviderTest : ProviderTest<ExposureCircuitBreakerInfoProvider, List<ExposureCircuitBreakerInfo>>() {

    override val getTestSubject = ::ExposureCircuitBreakerInfoProvider
    override val property = ExposureCircuitBreakerInfoProvider::info
    override val key = EXPOSURE_CIRCUIT_BREAKER_INFO_KEY
    override val defaultValue: List<ExposureCircuitBreakerInfo> = emptyList()
    override val expectations: List<ProviderTestExpectation<List<ExposureCircuitBreakerInfo>>> = listOf(
        ProviderTestExpectation(json = ITEM_WITHOUT_TOKEN, objectValue = listOf(itemWithoutToken), direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = MULTIPLE_ITEMS_JSON, objectValue = listOf(itemWithoutToken, itemWithToken), direction = JSON_TO_OBJECT)
    )

    @Test
    fun `add exposure circuit breaker info item`() {
        sharedPreferencesReturns(ITEM_WITHOUT_TOKEN)

        testSubject.add(itemWithToken)

        assertSharedPreferenceSetsValue(MULTIPLE_ITEMS_JSON)
    }

    @Test
    fun `remove exposure circuit breaker info item`() {
        sharedPreferencesReturns(MULTIPLE_ITEMS_JSON)

        testSubject.remove(itemWithoutToken)

        assertSharedPreferenceSetsValue(ITEM_WITH_TOKEN)
    }

    @Test
    fun `set approval token for polling`() {
        sharedPreferencesReturns(MULTIPLE_ITEMS_JSON)

        testSubject.setApprovalToken(itemWithoutToken, "new_token")

        assertSharedPreferenceSetsValue(MULTIPLE_ITEMS_WITH_TOKEN_JSON)
    }

    companion object {
        private val fixedClock = Clock.fixed(Instant.parse("2020-10-10T10:00:00Z"), ZoneOffset.UTC)
        private val startOfDayMillis =
            fixedClock.instant().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).toEpochMilli()

        private val itemWithoutToken = ExposureCircuitBreakerInfo(10.0, startOfDayMillis, 1, 2, fixedClock.millis())
        private val itemWithToken =
            ExposureCircuitBreakerInfo(13.0, startOfDayMillis, 1, 2, fixedClock.millis(), "token1")

        private val ITEM_WITHOUT_TOKEN =
            """
            [{"maximumRiskScore":10.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()}}]
            """.trim()

        private val ITEM_WITH_TOKEN =
            """
            [{"maximumRiskScore":13.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"token1"}]
            """.trim()

        private val MULTIPLE_ITEMS_JSON =
            """
            [{"maximumRiskScore":10.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()}},{"maximumRiskScore":13.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"token1"}]
            """.trim()

        private val MULTIPLE_ITEMS_WITH_TOKEN_JSON =
            """
            [{"maximumRiskScore":10.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"new_token"},{"maximumRiskScore":13.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"token1"}]
            """.trim()
    }
}

package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.remote.data.PostDistrictsResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyPostCodeDistributionResponse

class MockRiskyPostDistrictsApi : RiskyPostDistrictsApi {
    override suspend fun fetchRiskyPostDistricts(): PostDistrictsResponse {
        return PostDistrictsResponse(
            mapOf(
                "A1" to RiskLevel.HIGH,
                "CM1" to RiskLevel.HIGH,
                "A2" to RiskLevel.LOW,
                "CM2" to RiskLevel.MEDIUM,
                "AL1" to RiskLevel.LOW,
                "SE1" to RiskLevel.LOW
            )
        )
    }

    override suspend fun fetchRiskyPostCodeDistribution(): RiskyPostCodeDistributionResponse {
        return RiskyPostCodeDistributionResponse(
            postDistricts = mapOf(
                "A1" to "high",
                "CM1" to "high",
                "A2" to "green",
                "CM2" to "medium",
                "AL1" to "neutral",
                "AL2" to "green",
                "AL3" to "yellow",
                "AL4" to "amber",
                "AL5" to "red"
            ),
            riskLevels = mapOf(
                "neutral" to RiskIndicator(
                    colorScheme = NEUTRAL,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 1")),
                    heading = Translatable(mapOf("en" to "Heading low")),
                    content = Translatable(mapOf("en" to "Content low")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/"))
                ),
                "green" to RiskIndicator(
                    colorScheme = GREEN,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 1")),
                    heading = Translatable(mapOf("en" to "Heading low")),
                    content = Translatable(mapOf("en" to "Content low")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/"))
                ),
                "yellow" to RiskIndicator(
                    colorScheme = YELLOW,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 2")),
                    heading = Translatable(mapOf("en" to "Heading medium")),
                    content = Translatable(mapOf("en" to "Content medium")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/"))
                ),
                "amber" to RiskIndicator(
                    colorScheme = AMBER,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 2")),
                    heading = Translatable(mapOf("en" to "Heading medium")),
                    content = Translatable(mapOf("en" to "Content medium")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/"))
                ),
                "red" to RiskIndicator(
                    colorScheme = RED,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 3")),
                    heading = Translatable(mapOf("en" to "Heading high")),
                    content = Translatable(mapOf("en" to "Content high")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/"))
                )
            )
        )
    }
}

package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyPostCodeDistributionResponse

class MockRiskyPostDistrictsApi : RiskyPostDistrictsApi {
    override suspend fun fetchRiskyPostCodeDistribution(): RiskyPostCodeDistributionResponse {
        val meetingPolicy = Policy(
            policyIcon = PolicyIcon.MEETING_PEOPLE,
            policyHeading = Translatable(mapOf("en" to "Meeting people")),
            policyContent = Translatable(mapOf("en" to "Rule of six indoors and outdoors, in all settings."))
        )
        val travelPolicy = Policy(
            policyIcon = PolicyIcon.TRAVELLING,
            policyHeading = Translatable(mapOf("en" to "Travelling")),
            policyContent = Translatable(mapOf("en" to "You can continue to travel to venues that are open, for work or for education, but should reduce the number of journeys you make."))
        )

        val mediumPolicyData = PolicyData(
            heading = Translatable(mapOf("en" to "Coronavirus cases are high in your area")),
            content = Translatable(mapOf("en" to "Your area has some extra local restrictions in place.")),
            footer = Translatable(mapOf("en" to "Find out more about the measures that apply in your area to help reduce the spread of coronavirus.")),
            policies = listOf(meetingPolicy, travelPolicy),
            localAuthorityRiskTitle = Translatable(mapOf("en" to "[local authority] ([postcode]) is in local COVID alert level: medium"))
        )

        val lowPolicyData = PolicyData(
            heading = Translatable(mapOf("en" to "Your area has coronavirus cases")),
            content = Translatable(mapOf("en" to "Your area is in line with national restrictions.")),
            footer = Translatable(mapOf("en" to "Find out more about the measures that apply in your area to help reduce the spread of coronavirus.")),
            policies = listOf(meetingPolicy),
            localAuthorityRiskTitle = Translatable(mapOf("en" to "[local authority] ([postcode]) is in local COVID alert level: low"))
        )

        return RiskyPostCodeDistributionResponse(
            postDistricts = mapOf(
                "A1" to "red",
                "CM1" to "red",
                "A2" to "green",
                "CM2" to "yellow",
                "AL1" to "neutral",
                "AL2" to "green",
                "AL3" to "yellow",
                "AL4" to "amber",
                "AL5" to "red"
            ),
            localAuthorities = mapOf(
                "E07000240" to "neutral"
            ),
            riskLevels = mapOf(
                "neutral" to RiskIndicator(
                    colorScheme = NEUTRAL,
                    name = Translatable(mapOf("en" to "[postcode] is in local COVID alert level: low")),
                    heading = Translatable(mapOf("en" to "Heading low")),
                    content = Translatable(mapOf("en" to "Content low")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/")),
                    policyData = lowPolicyData
                ),
                "green" to RiskIndicator(
                    colorScheme = GREEN,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 1")),
                    heading = Translatable(mapOf("en" to "Heading low")),
                    content = Translatable(mapOf("en" to "Content low")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/")),
                    policyData = lowPolicyData
                ),
                "yellow" to RiskIndicator(
                    colorScheme = YELLOW,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 2")),
                    heading = Translatable(mapOf("en" to "Heading medium")),
                    content = Translatable(mapOf("en" to "Content medium")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/")),
                    policyData = mediumPolicyData
                ),
                "amber" to RiskIndicator(
                    colorScheme = AMBER,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 2")),
                    heading = Translatable(mapOf("en" to "Heading medium")),
                    content = Translatable(mapOf("en" to "Content medium")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/")),
                    policyData = mediumPolicyData
                ),
                "red" to RiskIndicator(
                    colorScheme = RED,
                    name = Translatable(mapOf("en" to "[postcode] is in Local Alert Level 3")),
                    heading = Translatable(mapOf("en" to "Heading high")),
                    content = Translatable(mapOf("en" to "Content high")),
                    linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                    linkUrl = Translatable(mapOf("en" to "https://a.b.c/")),
                    policyData = null
                )
            )
        )
    }
}

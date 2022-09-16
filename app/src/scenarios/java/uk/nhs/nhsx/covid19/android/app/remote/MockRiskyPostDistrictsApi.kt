package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.BLACK
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.MAROON
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlData
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlsWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyPostCodeDistributionResponse

class MockRiskyPostDistrictsApi : RiskyPostDistrictsApi {

    private val meetingPolicy = Policy(
        policyIcon = PolicyIcon.MEETING_PEOPLE,
        policyHeading = TranslatableString(mapOf("en" to "Meeting people")),
        policyContent = TranslatableString(mapOf("en" to "Rule of six indoors and outdoors, in all settings."))
    )
    private val travelPolicy = Policy(
        policyIcon = PolicyIcon.TRAVELLING,
        policyHeading = TranslatableString(mapOf("en" to "Travelling")),
        policyContent = TranslatableString(mapOf("en" to "You can continue to travel to venues that are open, for work or for education, but should reduce the number of journeys you make."))
    )
    private val socialDistancingPolicy = Policy(
        policyIcon = PolicyIcon.SOCIAL_DISTANCING,
        policyHeading = TranslatableString(mapOf("en" to "Social distancing")),
        policyContent = TranslatableString(mapOf("en" to "Please keep a safe distance of at least 2 meters people not living in your household."))
    )
    private val workPolicy = Policy(
        policyIcon = PolicyIcon.WORK,
        policyHeading = TranslatableString(mapOf("en" to "Work")),
        policyContent = TranslatableString(mapOf("en" to "If working from home is possible, it is advised to do so."))
    )

    private val mediumPolicyData = PolicyData(
        heading = TranslatableString(mapOf("en" to "Coronavirus cases are high in your area")),
        content = TranslatableString(mapOf("en" to "Your area has some extra local restrictions in place.")),
        footer = TranslatableString(mapOf("en" to "Find out more about the measures that apply in your area to help reduce the spread of coronavirus.")),
        policies = listOf(meetingPolicy, travelPolicy),
        localAuthorityRiskTitle = TranslatableString(mapOf("en" to "[local authority] ([postcode]) is in local COVID alert level: medium"))
    )

    private val lowPolicyData = PolicyData(
        heading = TranslatableString(mapOf("en" to "Your area has coronavirus cases")),
        content = TranslatableString(mapOf("en" to "Your area is in line with national restrictions.")),
        footer = TranslatableString(mapOf("en" to "Find out more about the measures that apply in your area to help reduce the spread of coronavirus.")),
        policies = listOf(meetingPolicy),
        localAuthorityRiskTitle = TranslatableString(mapOf("en" to "[local authority] ([postcode]) is in local COVID alert level: low"))
    )

    private val tierFourPolicyData = PolicyData(
        heading = TranslatableString(mapOf("en" to "Your area has coronavirus cases")),
        content = TranslatableString(mapOf("en" to "Your area is in line with national restrictions.")),
        footer = TranslatableString(mapOf("en" to "Find out more about the measures that apply in your area to help reduce the spread of coronavirus.")),
        policies = listOf(meetingPolicy, travelPolicy, socialDistancingPolicy),
        localAuthorityRiskTitle = TranslatableString(mapOf("en" to "[local authority] ([postcode]) is in local COVID alert level: Tier 4"))
    )

    private val tierFivePolicyData = PolicyData(
        heading = TranslatableString(mapOf("en" to "Your area has coronavirus cases")),
        content = TranslatableString(mapOf("en" to "Your area is in line with national restrictions.")),
        footer = TranslatableString(mapOf("en" to "Find out more about the measures that apply in your area to help reduce the spread of coronavirus.")),
        policies = listOf(meetingPolicy, travelPolicy, socialDistancingPolicy, workPolicy),
        localAuthorityRiskTitle = TranslatableString(mapOf("en" to "[local authority] ([postcode]) is in local COVID alert level: Tier 5"))
    )

    val externalUrls = ExternalUrlsWrapper(
        title = TranslatableString(mapOf("en" to "Keep your app updated:")),
        urls = listOf(
            ExternalUrlData(
                title = TranslatableString(mapOf("en" to "Check the App Store")),
                url = TranslatableString(mapOf(
                    "en" to "https://apps.apple.com/gb/app/nhs-covid-19/id1520427663"))
            ),
            ExternalUrlData(
                title = TranslatableString(mapOf("en" to "Check the Google Play Store")),
                url = TranslatableString(mapOf(
                    "en" to "https://play.google.com/store/apps/details?id=uk.nhs.covid19.production&hl=en_US&gl=UK"))
            ),
            ExternalUrlData(
                title = TranslatableString(mapOf("en" to "Check the app website")),
                url = TranslatableString(mapOf(
                    "en" to "https://www.gov.uk/government/collections/nhs-covid-19-app"))
            ),
        )
    )

    private val successResponse: RiskyPostCodeDistributionResponse = RiskyPostCodeDistributionResponse(
        postDistricts = mapOf(
            "A1" to "red",
            "CM1" to "red",
            "A2" to "green",
            "CM2" to "yellow",
            "AL1" to "neutral",
            "AL2" to "green",
            "AL3" to "yellow",
            "AL4" to "amber",
            "AL5" to "red",
            "SE1" to "maroon",
            "SE2" to "black",
        ),
        localAuthorities = mapOf(
            "E07000240" to "neutral",
            "E09000022" to "maroon",
            "E09000004" to "black",
        ),
        riskLevels = mapOf(
            "neutral" to RiskIndicator(
                colorScheme = NEUTRAL,
                colorSchemeV2 = NEUTRAL,
                name = TranslatableString(mapOf("en" to "[postcode] is in local COVID alert level: low")),
                heading = TranslatableString(mapOf("en" to "Heading low")),
                content = TranslatableString(mapOf("en" to "Content low")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = lowPolicyData,
                externalUrls = null
            ),
            "green" to RiskIndicator(
                colorScheme = GREEN,
                colorSchemeV2 = GREEN,
                name = TranslatableString(mapOf("en" to "[postcode] is in Local Alert Level 1")),
                heading = TranslatableString(mapOf("en" to "Heading low")),
                content = TranslatableString(mapOf("en" to "Content low")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = lowPolicyData,
                externalUrls = externalUrls
            ),
            "yellow" to RiskIndicator(
                colorScheme = YELLOW,
                colorSchemeV2 = YELLOW,
                name = TranslatableString(mapOf("en" to "[postcode] is in Local Alert Level 2")),
                heading = TranslatableString(mapOf("en" to "Heading medium")),
                content = TranslatableString(mapOf("en" to "Content medium")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = mediumPolicyData,
                externalUrls = null
            ),
            "amber" to RiskIndicator(
                colorScheme = AMBER,
                colorSchemeV2 = AMBER,
                name = TranslatableString(mapOf("en" to "[postcode] is in Local Alert Level 2")),
                heading = TranslatableString(mapOf("en" to "Heading medium")),
                content = TranslatableString(mapOf("en" to "Content medium")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = mediumPolicyData,
                externalUrls = externalUrls
            ),
            "red" to RiskIndicator(
                colorScheme = RED,
                colorSchemeV2 = RED,
                name = TranslatableString(mapOf("en" to "[postcode] is in Local Alert Level 3")),
                heading = TranslatableString(mapOf("en" to "Heading high")),
                content = TranslatableString(mapOf("en" to "Content high")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = null,
                externalUrls = externalUrls
            ),
            "maroon" to RiskIndicator(
                colorScheme = RED,
                colorSchemeV2 = MAROON,
                name = TranslatableString(mapOf("en" to "[postcode] is in Local Alert Level 4")),
                heading = TranslatableString(mapOf("en" to "Heading Tier 4")),
                content = TranslatableString(mapOf("en" to "Content Tier 4")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = tierFourPolicyData,
                externalUrls = null
            ),
            "black" to RiskIndicator(
                colorScheme = RED,
                colorSchemeV2 = BLACK,
                name = TranslatableString(mapOf("en" to "[postcode] is in Local Alert Level 5")),
                heading = TranslatableString(mapOf("en" to "Heading Tier 5")),
                content = TranslatableString(mapOf("en" to "Content Tier 5")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = tierFivePolicyData,
                externalUrls = externalUrls
            ),
        )
    )

    override suspend fun fetchRiskyPostCodeDistribution(): RiskyPostCodeDistributionResponse =
        MockApiModule.behaviour.invoke { successResponse }
}

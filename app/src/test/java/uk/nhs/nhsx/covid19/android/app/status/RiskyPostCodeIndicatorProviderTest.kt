package uk.nhs.nhsx.covid19.android.app.status

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlData
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlsWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.status.RiskyPostCodeIndicatorProvider.Companion.RISKY_POST_CODE_INDICATOR_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT

class RiskyPostCodeIndicatorProviderTest : ProviderTest<RiskyPostCodeIndicatorProvider, RiskIndicatorWrapper?>() {

    override val getTestSubject = ::RiskyPostCodeIndicatorProvider
    override val property = RiskyPostCodeIndicatorProvider::riskyPostCodeIndicator
    override val key = RISKY_POST_CODE_INDICATOR_KEY
    override val defaultValue: RiskIndicatorWrapper? = null
    override val expectations: List<ProviderTestExpectation<RiskIndicatorWrapper?>> = listOf(
        ProviderTestExpectation(json = riskyPostCodeIndicatorJsonV1, objectValue = riskyPostCodeIndicatorV1),
        ProviderTestExpectation(json = riskyPostCodeIndicatorJsonV2, objectValue = riskyPostCodeIndicatorV2),
        ProviderTestExpectation(json = riskyPostCodeIndicatorJsonV2WithExternalUrls, objectValue = riskyPostCodeIndicatorV2WithExternalUrls),
        ProviderTestExpectation(json = riskyPostCodeIndicatorJsonWithUnknownColorScheme, objectValue = riskyPostCodeIndicatorWithDefaultColorScheme, direction = JSON_TO_OBJECT)
    )

    @Test
    fun `test clear test results`() {
        sharedPreferencesReturns(riskyPostCodeIndicatorJsonV1)

        testSubject.clear()

        assertSharedPreferenceSetsValue(null)
    }

    companion object {
        private val riskyPostCodeIndicatorV1 = RiskIndicatorWrapper(
            riskLevel = "high",
            riskIndicator = RiskIndicator(
                colorScheme = ColorScheme.RED,
                name = TranslatableString(mapOf("en" to "high")),
                heading = TranslatableString(mapOf("en" to "Heading high")),
                content = TranslatableString(mapOf("en" to "Content high")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = null,
                externalUrls = null
            ),
            riskLevelFromLocalAuthority = true
        )

        private val riskyPostCodeIndicatorV2 = RiskIndicatorWrapper(
            riskLevel = "high",
            riskIndicator = RiskIndicator(
                colorScheme = ColorScheme.RED,
                colorSchemeV2 = ColorScheme.MAROON,
                name = TranslatableString(mapOf("en" to "high")),
                heading = TranslatableString(mapOf("en" to "Heading high")),
                content = TranslatableString(mapOf("en" to "Content high")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = null,
                externalUrls = null
            ),
            riskLevelFromLocalAuthority = true
        )

        private val riskyPostCodeIndicatorV2WithExternalUrls = riskyPostCodeIndicatorV2.copy(
            riskLevel = "high",
            riskIndicator = RiskIndicator(
                colorScheme = ColorScheme.RED,
                colorSchemeV2 = ColorScheme.MAROON,
                name = TranslatableString(mapOf("en" to "high")),
                heading = TranslatableString(mapOf("en" to "Heading high")),
                content = TranslatableString(mapOf("en" to "Content high")),
                linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
                policyData = null,
                externalUrls = ExternalUrlsWrapper(
                    title = TranslatableString(mapOf("en" to "Keep your app updated:")),
                    urls = listOf(
                        ExternalUrlData(
                            title = TranslatableString(mapOf("en" to "Check the Google Play Store")),
                            url = TranslatableString(
                                mapOf("en" to "https://a.b.c.d/")
                            )
                        )
                    )
                )
            ),
            riskLevelFromLocalAuthority = true
        )

        private val riskyPostCodeIndicatorWithDefaultColorScheme = riskyPostCodeIndicatorV1.copy(
            riskIndicator = riskyPostCodeIndicatorV1.riskIndicator?.copy(colorScheme = NEUTRAL)
        )

        private const val riskyPostCodeIndicatorJsonV1 =
            """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","name":{"en":"high"},"heading":{"en":"Heading high"},"content":{"en":"Content high"},"linkTitle":{"en":"Restrictions in your area"},"linkUrl":{"en":"https://a.b.c/"}},"riskLevelFromLocalAuthority":true}"""

        private const val riskyPostCodeIndicatorJsonV2 =
            """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","colorSchemeV2":"maroon","name":{"en":"high"},"heading":{"en":"Heading high"},"content":{"en":"Content high"},"linkTitle":{"en":"Restrictions in your area"},"linkUrl":{"en":"https://a.b.c/"}},"riskLevelFromLocalAuthority":true}"""

        private const val riskyPostCodeIndicatorJsonV2WithExternalUrls =
            """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","colorSchemeV2":"maroon","name":{"en":"high"},"heading":{"en":"Heading high"},"content":{"en":"Content high"},"linkTitle":{"en":"Restrictions in your area"},"linkUrl":{"en":"https://a.b.c/"},"externalUrls":{"title":{"en":"Keep your app updated:"},"urls":[{"title":{"en":"Check the Google Play Store"},"url":{"en":"https://a.b.c.d/"}}]}},"riskLevelFromLocalAuthority":true}"""

        private const val riskyPostCodeIndicatorJsonWithUnknownColorScheme =
            """{"riskLevel":"high","riskIndicator":{"colorScheme":"unknown-color-scheme","name":{"en":"high"},"heading":{"en":"Heading high"},"content":{"en":"Content high"},"linkTitle":{"en":"Restrictions in your area"},"linkUrl":{"en":"https://a.b.c/"}},"riskLevelFromLocalAuthority":true}"""
    }
}

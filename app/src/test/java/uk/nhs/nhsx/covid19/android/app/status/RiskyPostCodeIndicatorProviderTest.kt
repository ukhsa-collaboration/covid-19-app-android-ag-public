package uk.nhs.nhsx.covid19.android.app.status

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.util.adapters.ColorSchemeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.PolicyIconAdapter
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RiskyPostCodeIndicatorProviderTest {

    private val moshi = Moshi.Builder()
        .add(PolicyIconAdapter())
        .add(ColorSchemeAdapter())
        .build()

    private val riskyPostCodeIndicatorStorage =
        mockk<RiskyPostCodeIndicatorStorage>(relaxed = true)

    private val testSubject = RiskyPostCodeIndicatorProvider(
        riskyPostCodeIndicatorStorage,
        moshi
    )

    @Test
    fun `verify empty`() {
        every { riskyPostCodeIndicatorStorage.value } returns null

        val actual = testSubject.riskyPostCodeIndicator

        assertNull(actual)
    }

    @Test
    fun `verify serialization without colorSchemeV2`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJsonV1

        val actual = testSubject.riskyPostCodeIndicator

        assertEquals(riskyPostCodeIndicatorV1, actual)
    }

    @Test
    fun `verify deserialization without colorSchemeV2`() {
        testSubject.riskyPostCodeIndicator = riskyPostCodeIndicatorV1

        verify { riskyPostCodeIndicatorStorage.value = riskyPostCodeIndicatorJsonV1 }
    }

    @Test
    fun `verify serialization with colorSchemeV2`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJsonV2

        val actual = testSubject.riskyPostCodeIndicator

        assertEquals(riskyPostCodeIndicatorV2, actual)
    }

    @Test
    fun `verify serialization with colorSchemeV2 and unknown color scheme fall back to neutral`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJsonWithUnknownColorScheme

        val expected = riskyPostCodeIndicatorV1.copy(
            riskIndicator = riskyPostCodeIndicatorV1.riskIndicator?.copy(colorScheme = NEUTRAL)
        )
        val actual = testSubject.riskyPostCodeIndicator

        assertEquals(expected, actual)
    }

    @Test
    fun `verify V2 deserialization with colorSchemeV2`() {
        testSubject.riskyPostCodeIndicator = riskyPostCodeIndicatorV2

        verify { riskyPostCodeIndicatorStorage.value = riskyPostCodeIndicatorJsonV2 }
    }

    @Test
    fun `test clear test results`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJsonV1

        testSubject.clear()

        verify { riskyPostCodeIndicatorStorage.value = null }
    }

    private val riskyPostCodeIndicatorV1 = RiskIndicatorWrapper(
        riskLevel = "high",
        riskIndicator = RiskIndicator(
            colorScheme = ColorScheme.RED,
            name = Translatable(mapOf("en" to "high")),
            heading = Translatable(mapOf("en" to "Heading high")),
            content = Translatable(mapOf("en" to "Content high")),
            linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
            linkUrl = Translatable(mapOf("en" to "https://a.b.c/")),
            policyData = null
        ),
        riskLevelFromLocalAuthority = true
    )

    private val riskyPostCodeIndicatorV2 = RiskIndicatorWrapper(
        riskLevel = "high",
        riskIndicator = RiskIndicator(
            colorScheme = ColorScheme.RED,
            colorSchemeV2 = ColorScheme.MAROON,
            name = Translatable(mapOf("en" to "high")),
            heading = Translatable(mapOf("en" to "Heading high")),
            content = Translatable(mapOf("en" to "Content high")),
            linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
            linkUrl = Translatable(mapOf("en" to "https://a.b.c/")),
            policyData = null
        ),
        riskLevelFromLocalAuthority = true
    )

    private val riskyPostCodeIndicatorJsonV1 =
        """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","name":{"translations":{"en":"high"}},"heading":{"translations":{"en":"Heading high"}},"content":{"translations":{"en":"Content high"}},"linkTitle":{"translations":{"en":"Restrictions in your area"}},"linkUrl":{"translations":{"en":"https://a.b.c/"}}},"riskLevelFromLocalAuthority":true}"""

    private val riskyPostCodeIndicatorJsonV2 =
        """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","colorSchemeV2":"maroon","name":{"translations":{"en":"high"}},"heading":{"translations":{"en":"Heading high"}},"content":{"translations":{"en":"Content high"}},"linkTitle":{"translations":{"en":"Restrictions in your area"}},"linkUrl":{"translations":{"en":"https://a.b.c/"}}},"riskLevelFromLocalAuthority":true}"""

    private val riskyPostCodeIndicatorJsonWithUnknownColorScheme =
        """{"riskLevel":"high","riskIndicator":{"colorScheme":"unknown-color-scheme","name":{"translations":{"en":"high"}},"heading":{"translations":{"en":"Heading high"}},"content":{"translations":{"en":"Content high"}},"linkTitle":{"translations":{"en":"Restrictions in your area"}},"linkUrl":{"translations":{"en":"https://a.b.c/"}}},"riskLevelFromLocalAuthority":true}"""
}

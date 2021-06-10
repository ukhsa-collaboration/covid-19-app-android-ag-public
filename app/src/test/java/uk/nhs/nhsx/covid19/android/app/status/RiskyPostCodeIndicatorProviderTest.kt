package uk.nhs.nhsx.covid19.android.app.status

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.util.adapters.ColorSchemeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.PolicyIconAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.TranslatableStringAdapter
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RiskyPostCodeIndicatorProviderTest {

    private val moshi = Moshi.Builder()
        .add(PolicyIconAdapter())
        .add(ColorSchemeAdapter())
        .add(TranslatableStringAdapter())
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
    fun `verify deserialization without colorSchemeV2`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJsonV1

        val actual = testSubject.riskyPostCodeIndicator

        assertEquals(riskyPostCodeIndicatorV1, actual)
    }

    @Test
    fun `verify serialization without colorSchemeV2`() {
        testSubject.riskyPostCodeIndicator = riskyPostCodeIndicatorV1

        verify { riskyPostCodeIndicatorStorage.value = riskyPostCodeIndicatorJsonV1 }
    }

    @Test
    fun `verify deserialization with colorSchemeV2`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJsonV2

        val actual = testSubject.riskyPostCodeIndicator

        assertEquals(riskyPostCodeIndicatorV2, actual)
    }

    @Test
    fun `verify deserialization with colorSchemeV2 and unknown color scheme fall back to neutral`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJsonWithUnknownColorScheme

        val expected = riskyPostCodeIndicatorV1.copy(
            riskIndicator = riskyPostCodeIndicatorV1.riskIndicator?.copy(colorScheme = NEUTRAL)
        )
        val actual = testSubject.riskyPostCodeIndicator

        assertEquals(expected, actual)
    }

    @Test
    fun `verify V2 serialization with colorSchemeV2`() {
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
            name = TranslatableString(mapOf("en" to "high")),
            heading = TranslatableString(mapOf("en" to "Heading high")),
            content = TranslatableString(mapOf("en" to "Content high")),
            linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
            linkUrl = TranslatableString(mapOf("en" to "https://a.b.c/")),
            policyData = null
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
            policyData = null
        ),
        riskLevelFromLocalAuthority = true
    )

    private val riskyPostCodeIndicatorJsonV1 =
        """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","name":{"en":"high"},"heading":{"en":"Heading high"},"content":{"en":"Content high"},"linkTitle":{"en":"Restrictions in your area"},"linkUrl":{"en":"https://a.b.c/"}},"riskLevelFromLocalAuthority":true}"""

    private val riskyPostCodeIndicatorJsonV2 =
        """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","colorSchemeV2":"maroon","name":{"en":"high"},"heading":{"en":"Heading high"},"content":{"en":"Content high"},"linkTitle":{"en":"Restrictions in your area"},"linkUrl":{"en":"https://a.b.c/"}},"riskLevelFromLocalAuthority":true}"""

    private val riskyPostCodeIndicatorJsonWithUnknownColorScheme =
        """{"riskLevel":"high","riskIndicator":{"colorScheme":"unknown-color-scheme","name":{"en":"high"},"heading":{"en":"Heading high"},"content":{"en":"Content high"},"linkTitle":{"en":"Restrictions in your area"},"linkUrl":{"en":"https://a.b.c/"}},"riskLevelFromLocalAuthority":true}"""
}

package uk.nhs.nhsx.covid19.android.app.status

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.util.adapters.PolicyIconAdapter
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RiskyPostCodeIndicatorProviderTest {

    private val moshi = Moshi.Builder()
        .add(PolicyIconAdapter())
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
    fun `verify serialization`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJson

        val actual = testSubject.riskyPostCodeIndicator

        assertEquals(riskyPostCodeIndicator, actual)
    }

    @Test
    fun `verify deserialization`() {

        testSubject.riskyPostCodeIndicator = riskyPostCodeIndicator

        verify { riskyPostCodeIndicatorStorage.value = riskyPostCodeIndicatorJson }
    }

    @Test
    fun `test clear test results`() {
        every { riskyPostCodeIndicatorStorage.value } returns riskyPostCodeIndicatorJson

        testSubject.clear()

        verify { riskyPostCodeIndicatorStorage.value = null }
    }

    private val riskyPostCodeIndicator = RiskIndicatorWrapper(
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

    private val riskyPostCodeIndicatorJson =
        """{"riskLevel":"high","riskIndicator":{"colorScheme":"red","name":{"translations":{"en":"high"}},"heading":{"translations":{"en":"Heading high"}},"content":{"translations":{"en":"Content high"}},"linkTitle":{"translations":{"en":"Restrictions in your area"}},"linkUrl":{"translations":{"en":"https://a.b.c/"}}},"riskLevelFromLocalAuthority":true}"""
}

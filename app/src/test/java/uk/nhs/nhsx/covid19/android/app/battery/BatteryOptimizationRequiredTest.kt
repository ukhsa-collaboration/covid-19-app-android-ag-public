package uk.nhs.nhsx.covid19.android.app.battery

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryOptimizationRequiredTest {

    private val batteryOptimizationChecker = mockk<BatteryOptimizationChecker>()
    private val batteryOptimizationAcknowledgementProvider = mockk<BatteryOptimizationAcknowledgementProvider>()

    private val testSubject = BatteryOptimizationRequired(
        batteryOptimizationChecker,
        batteryOptimizationAcknowledgementProvider
    )

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `when ignoring battery optimizations and not acknowledged and feature flag enabled then returns false`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns true
        every { batteryOptimizationAcknowledgementProvider.value } returns null
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertFalse(testSubject())
    }

    @Test
    fun `when ignoring battery optimizations and acknowledged and feature flag enabled then returns false`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns true
        every { batteryOptimizationAcknowledgementProvider.value } returns 123L
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertFalse(testSubject())
    }

    @Test
    fun `when ignoring battery optimizations and acknowledged and feature flag disabled then returns false`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns true
        every { batteryOptimizationAcknowledgementProvider.value } returns 123L
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertFalse(testSubject())
    }

    @Test
    fun `when ignoring battery optimizations and not acknowledged and feature flag disabled then returns false`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns true
        every { batteryOptimizationAcknowledgementProvider.value } returns null
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertFalse(testSubject())
    }

    @Test
    fun `when not ignoring battery optimizations and not acknowledged and feature flag enabled then returns true`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns false
        every { batteryOptimizationAcknowledgementProvider.value } returns null
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertTrue(testSubject())
    }

    @Test
    fun `when not ignoring battery optimizations and acknowledged and feature flag enabled then returns false`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns false
        every { batteryOptimizationAcknowledgementProvider.value } returns 123L
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertFalse(testSubject())
    }

    @Test
    fun `when not ignoring battery optimizations and acknowledged and feature flag disabled then returns false`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns false
        every { batteryOptimizationAcknowledgementProvider.value } returns 123L
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertFalse(testSubject())
    }

    @Test
    fun `when not ignoring battery optimizations and not acknowledged and feature flag disabled then returns false`() {
        every { batteryOptimizationChecker.isIgnoringBatteryOptimizations() } returns false
        every { batteryOptimizationAcknowledgementProvider.value } returns null
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        assertFalse(testSubject())
    }
}

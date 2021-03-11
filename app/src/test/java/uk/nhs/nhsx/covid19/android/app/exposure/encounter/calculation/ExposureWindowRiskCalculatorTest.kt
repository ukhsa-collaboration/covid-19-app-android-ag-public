package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowUtils.Companion.getExposureWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExposureWindowRiskCalculatorTest {

    private val riskScoreCalculatorProvider = mockk<RiskScoreCalculatorProvider>()
    private val filterRiskyExposureWindows = mockk<FilterRiskyExposureWindows>()
    private val evaluateMostRelevantExposure = mockk<EvaluateMostRelevantExposure>()
    private val calculateExposureRisk = mockk<CalculateExposureRisk>()
    private val analyticsEventTracker = mockk<AnalyticsEventTracker>(relaxUnitFun = true)

    @Before
    fun setup() {
        every { riskScoreCalculatorProvider.getRiskCalculationVersion() } returns 2
    }

    @Test
    fun `returns null as relevant risk and empty list of exposure windows with risk when filter risky exposure windows returns empty list`() {
        val exposureWindow = getExposureWindow(listOf())
        val expectedExposureWindowWithRisk = exposureWindow.toExposureWindowWithRisk()

        every {
            calculateExposureRisk.invoke(exposureWindow, riskScoreCalculationConfig, riskCalculation)
        } returns expectedRisk
        every {
            filterRiskyExposureWindows.invoke(listOf(expectedExposureWindowWithRisk), riskCalculation.riskThreshold)
        } returns listOf()
        every { evaluateMostRelevantExposure.invoke(emptyList()) } returns null

        val result = riskCalculator(listOf(exposureWindow), riskCalculation, riskScoreCalculationConfig)

        verify(exactly = 1) { analyticsEventTracker.track(ExposureWindowsMatched(0, 1)) }

        assertNull(result.relevantRisk)
        assertTrue(result.exposureWindowsWithRisk.isEmpty())
    }

    @Test
    fun `returns relevant risk and list of exposure windows with risk when filter does not return empty list`() {
        val exposureWindow = getExposureWindow(listOf())
        val expectedExposureWindowsWithRisk = listOf(exposureWindow.toExposureWindowWithRisk())
        val expectedDayRisk = DayRisk(
            startOfDayMillis = exposureWindow.dateMillisSinceEpoch,
            calculatedRisk = expectedExposureWindowsWithRisk[0].calculatedRisk,
            riskCalculationVersion = expectedRiskScoreCalculationVersion,
            matchedKeyCount = 1
        )

        every {
            calculateExposureRisk.invoke(exposureWindow, riskScoreCalculationConfig, riskCalculation)
        } returns expectedRisk
        every {
            filterRiskyExposureWindows.invoke(expectedExposureWindowsWithRisk, riskCalculation.riskThreshold)
        } returns expectedExposureWindowsWithRisk
        every { evaluateMostRelevantExposure.invoke(expectedExposureWindowsWithRisk) } returns expectedDayRisk

        val expected = RiskCalculationResult(
            relevantRisk = expectedDayRisk,
            exposureWindowsWithRisk = expectedExposureWindowsWithRisk
        )
        val actual = riskCalculator(listOf(exposureWindow), riskCalculation, riskScoreCalculationConfig)

        verify(exactly = 1) { analyticsEventTracker.track(ExposureWindowsMatched(1, 0)) }

        assertEquals(expected, actual)
    }

    private val riskScoreCalculationConfig = mockk<RiskScoreCalculatorConfiguration>()
    private val riskCalculation = V2RiskCalculation(
        daysSinceOnsetToInfectiousness = listOf(),
        infectiousnessWeights = listOf(1.0, 1.0, 1.0),
        reportTypeWhenMissing = 0,
        riskThreshold = 0.0
    )
    private val expectedRisk = 100.0
    private val expectedRiskScoreCalculationVersion = 2

    private val riskCalculator = ExposureWindowRiskCalculator(
        filterRiskyExposureWindows,
        evaluateMostRelevantExposure,
        calculateExposureRisk,
        riskScoreCalculatorProvider,
        analyticsEventTracker,
    )

    private fun ExposureWindow.toExposureWindowWithRisk() =
        ExposureWindowWithRisk(
            exposureWindow = this,
            calculatedRisk = expectedRisk,
            riskCalculationVersion = expectedRiskScoreCalculationVersion,
            matchedKeyCount = 1
        )
}

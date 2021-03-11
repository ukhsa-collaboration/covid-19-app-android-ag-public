package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowUtils.Companion.getExposureWindow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowUtils.Companion.getGoogleScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.riskscore.RiskScoreCalculator
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import uk.nhs.riskscore.ScanInstance
import kotlin.test.assertEquals

class CalculateExposureRiskTest {

    private val riskScoreCalculatorProvider = mockk<RiskScoreCalculatorProvider>()

    private val calculateExposureRisk = CalculateExposureRisk(riskScoreCalculatorProvider)

    private val riskScoreCalculator = mockk<RiskScoreCalculator>()

    private val riskScoreCalculatorConfig = mockk<RiskScoreCalculatorConfiguration>()
    private val riskCalculation = V2RiskCalculation(
        daysSinceOnsetToInfectiousness = listOf(),
        infectiousnessWeights = listOf(1.0, 1.0, 1.0),
        reportTypeWhenMissing = 0,
        riskThreshold = 0.0
    )
    private val expectedRiskScore = 2.0

    @Before
    fun setup() {
        every { riskScoreCalculatorProvider.riskScoreCalculator(any()) } returns riskScoreCalculator
        every { riskScoreCalculator.calculate(any()) } returns expectedRiskScore
    }

    @Test
    fun `calls risk score calculator with mapped scan instances for each exposure window`() {
        val expectedAttenuationValue = 55
        val expectedSecondsSinceLastScan = 180
        val scanInstances = listOf(getGoogleScanInstance(expectedAttenuationValue, expectedSecondsSinceLastScan))
        val exposureWindow = getExposureWindow(scanInstances)

        calculateExposureRisk.invoke(exposureWindow, riskScoreCalculatorConfig, riskCalculation)

        val expectedInstances = listOf(ScanInstance(expectedAttenuationValue, expectedSecondsSinceLastScan))

        verify { riskScoreCalculator.calculate(expectedInstances) }
    }

    @Test
    fun `drops any scan instances with seconds since last scan of 0`() {
        val scanInstances = listOf(getGoogleScanInstance(50, 0))
        val exposureWindow = getExposureWindow(scanInstances)

        calculateExposureRisk.invoke(exposureWindow, riskScoreCalculatorConfig, riskCalculation)

        verify { riskScoreCalculator.calculate(listOf()) }
    }

    @Test
    fun `calls risk score calculator provider with config`() {
        calculateExposureRisk.invoke(getExposureWindow(), riskScoreCalculatorConfig, riskCalculation)

        verify { riskScoreCalculatorProvider.riskScoreCalculator(riskScoreCalculatorConfig) }
    }

    @Test
    fun `multiplies risk score calculator result by 60`() {
        val calculatedRisk = calculateExposureRisk.invoke(getExposureWindow(), riskScoreCalculatorConfig, riskCalculation)

        val expectedCalculatedRisk = expectedRiskScore * 60
        assertEquals(expectedCalculatedRisk, calculatedRisk)
    }

    @Test
    fun `multiples risk score calculator result by infectiousness factor`() {
        val infectiousnessWeights = listOf(0.0, 0.4, 1.0)
        val infectiousness = 1
        val calculationConfig = riskCalculation.copy(infectiousnessWeights = infectiousnessWeights)
        val exposureWindow = getExposureWindow(infectiousness = infectiousness)

        val calculatedRisk = calculateExposureRisk.invoke(exposureWindow, riskScoreCalculatorConfig, calculationConfig)

        val expectedCalculatedRisk = expectedRiskScore * 60 * infectiousnessWeights[infectiousness]
        assertEquals(expectedCalculatedRisk, calculatedRisk)
    }

    @Test
    fun `multiples risk score calculator result by first infectiousness weight if infectiousness index out of bounds`() {
        val infectiousnessWeights = listOf(0.0)
        val calculationConfig = riskCalculation.copy(infectiousnessWeights = infectiousnessWeights)
        val exposureWindow = getExposureWindow(infectiousness = 1)

        val calculatedRisk = calculateExposureRisk.invoke(exposureWindow, riskScoreCalculatorConfig, calculationConfig)

        val expectedCalculatedRisk = expectedRiskScore * 60 * infectiousnessWeights[0]
        assertEquals(expectedCalculatedRisk, calculatedRisk)
    }
}

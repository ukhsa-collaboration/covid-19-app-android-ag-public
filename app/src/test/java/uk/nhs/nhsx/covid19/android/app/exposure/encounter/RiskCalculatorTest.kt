package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureInformation.ExposureInformationBuilder
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureNotification
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation
import kotlin.test.assertNull

class RiskCalculatorTest {

    private lateinit var subject: RiskCalculator

    private val riskCalculationConfiguration = getConfigurationWithThreshold().riskCalculation

    @Before
    fun setup() {
        subject = RiskCalculator()
    }

    @Test
    fun `test risk score for no exposures`() {
        val riskScore =
            subject(listOf(getExposureInfoWith(listOf(0, 0, 0))), riskCalculationConfiguration)
        assertNull(riskScore)
    }

    @Test
    fun `test weighting applied to exposure`() {
        val riskScore =
            subject(listOf(getExposureInfoWith(listOf(5, 5, 5))), riskCalculationConfiguration)
        assertNull(riskScore)
    }

    @Test
    fun `test parameterised weighting applied to exposures`() {
        val parameterisedTestCases = listOf(
            Pair(listOf(30, 0, 0), 1800.0),
            Pair(listOf(0, 30, 0), 900.0),
            Pair(listOf(0, 0, 30), 0.0),
            Pair(listOf(5, 5, 5), 450.0),
            Pair(listOf(0, 5, 5), 150.0),
            Pair(listOf(5, 0, 5), 300.0)
        )

        for (durationRiskScorePair in parameterisedTestCases) {
            val riskScore = subject(
                listOf(getExposureInfoWith(durationRiskScorePair.first)),
                getConfigurationWithThreshold(threshold = 0).riskCalculation
            )
            Assert.assertEquals(durationRiskScorePair.second, riskScore!!.second, 0.0)
        }
    }

    @Test
    fun `test greatest risk score amongst exposures is returned`() {
        val shorterExposure = getExposureInfoWith(listOf(5, 0, 0))
        val longerExposure = getExposureInfoWith(listOf(15, 0, 0))

        val riskScore =
            subject(listOf(shorterExposure, longerExposure), riskCalculationConfiguration)
        Assert.assertEquals(900.0, riskScore!!.second, 0.0)
    }

    @Test
    fun `test parameterised greatest risk score amonst exposured is returned`() {
        val shorterExposure = getExposureInfoWith(listOf(5, 0, 0))
        val longerExposure = getExposureInfoWith(listOf(15, 0, 0))

        val parameterisedTestCases = listOf(
            Pair(listOf(longerExposure, shorterExposure), 900.0),
            Pair(listOf(shorterExposure, shorterExposure), 300.0),
            Pair(listOf(longerExposure, longerExposure), 900.0)
        )

        for (durationRiskScorePair in parameterisedTestCases) {
            val riskScore = subject(
                durationRiskScorePair.first,
                getConfigurationWithThreshold(threshold = 0).riskCalculation
            )
            Assert.assertEquals(durationRiskScorePair.second, riskScore!!.second, 0.0)
        }
    }

    @Test
    fun `test most recent risk above threshold is returned`() {
        val exposureInfoList = listOf(
            getExposureInfoWith(listOf(25, 0, 0), 2),
            getExposureInfoWith(listOf(15, 0, 0), 4),
            getExposureInfoWith(listOf(20, 0, 0), 4),
            getExposureInfoWith(listOf(10, 0, 0), 6)

        )
        val riskScore = subject(exposureInfoList, getConfigurationWithThreshold(threshold = 900).riskCalculation)
        Assert.assertEquals(1200.0, riskScore!!.second, 0.0)
        Assert.assertEquals(4, riskScore.first)
    }

    private fun getExposureInfoWith(
        attenuationDurations: List<Int>,
        dateMillis: Long = 0
    ): ExposureInformation {
        return ExposureInformationBuilder()
            .setAttenuationDurations(attenuationDurations.toIntArray())
            .setDateMillisSinceEpoch(dateMillis)
            .build()
    }

    private fun getConfigurationWithThreshold(threshold: Int = 900) =
        ExposureConfigurationResponse(
            exposureNotification = ExposureNotification(
                minimumRiskScore = 11,
                attenuationDurationThresholds = listOf(55, 63),
                attenuationLevelValues = listOf(0, 1, 1, 1, 1, 1, 1, 1),
                daysSinceLastExposureLevelValues = listOf(5, 5, 5, 5, 5, 5, 5, 5),
                durationLevelValues = listOf(0, 0, 0, 1, 1, 1, 1, 0),
                transmissionRiskLevelValues = listOf(1, 3, 4, 5, 6, 7, 8, 6),
                attenuationWeight = 50.0,
                daysSinceLastExposureWeight = 20,
                durationWeight = 50.0,
                transmissionRiskWeight = 50.0
            ),
            riskCalculation = RiskCalculation(
                durationBucketWeights = listOf(1.0, 0.5, 0.0),
                riskThreshold = threshold
            )
        )
}

package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import javax.inject.Inject

class ExposureWindowRiskCalculator @Inject constructor(
    private val evaluateMostRelevantRiskyExposure: EvaluateMostRelevantRiskyExposure,
    private val evaluateIfConsideredRisky: EvaluateIfConsideredRisky,
    private val calculateExposureRisk: CalculateExposureRisk,
    private val riskScoreCalculatorProvider: RiskScoreCalculatorProvider,
    private val analyticsEventTracker: AnalyticsEventTracker,
) {

    operator fun invoke(
        exposureWindows: List<ExposureWindow>,
        riskCalculation: V2RiskCalculation,
        config: RiskScoreCalculatorConfiguration
    ): RiskCalculationResult {
        Timber.d("Exposure windows: $exposureWindows")

        val allExposureWindowsWithRisk = exposureWindows.map { window ->
            val calculatedRisk = calculateExposureRisk(window, config, riskCalculation)

            ExposureWindowWithRisk(
                exposureWindow = window,
                calculatedRisk = calculatedRisk,
                riskCalculationVersion = riskScoreCalculatorProvider.getRiskCalculationVersion(),
                matchedKeyCount = 1,
                isConsideredRisky = evaluateIfConsideredRisky(window, calculatedRisk, riskCalculation.riskThreshold)
            )
        }

        Timber.d("Risk threshold is: ${riskCalculation.riskThreshold}")

        val (riskyExposureWindows, nonRiskyExposureWindows) = allExposureWindowsWithRisk.partition {
            it.isConsideredRisky
        }

        trackExposureWindowsMatchedAnalyticsIfNecessary(
            totalNumberOfExposureWindows = allExposureWindowsWithRisk.size,
            totalRisky = riskyExposureWindows.size
        )

        val mostRelevantExposure = evaluateMostRelevantRiskyExposure(riskyExposureWindows)

        return RiskCalculationResult(
            mostRelevantExposure,
            PartitionExposureWindowsResult(
                riskyExposureWindows,
                nonRiskyExposureWindows
            )
        )
    }

    private fun trackExposureWindowsMatchedAnalyticsIfNecessary(totalNumberOfExposureWindows: Int, totalRisky: Int) {
        if (totalNumberOfExposureWindows > 0) {
            val totalNonRisky = totalNumberOfExposureWindows - totalRisky
            analyticsEventTracker.track(ExposureWindowsMatched(totalRisky, totalNonRisky))
        }
    }
}

data class RiskCalculationResult(
    val relevantRisk: DayRisk?,
    val partitionedExposureWindows: PartitionExposureWindowsResult
)

data class PartitionExposureWindowsResult(
    val riskyExposureWindows: List<ExposureWindowWithRisk>,
    val nonRiskyExposureWindows: List<ExposureWindowWithRisk>
)

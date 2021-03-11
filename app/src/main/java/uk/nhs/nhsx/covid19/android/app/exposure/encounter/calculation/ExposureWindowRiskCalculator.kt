package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import javax.inject.Inject

class ExposureWindowRiskCalculator @Inject constructor(
    private val filterRiskyExposureWindows: FilterRiskyExposureWindows,
    private val evaluateMostRelevantExposure: EvaluateMostRelevantExposure,
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
            ExposureWindowWithRisk(
                exposureWindow = window,
                calculatedRisk = calculateExposureRisk(window, config, riskCalculation),
                riskCalculationVersion = riskScoreCalculatorProvider.getRiskCalculationVersion(),
                matchedKeyCount = 1
            )
        }

        Timber.d("Risk threshold is: ${riskCalculation.riskThreshold}")

        val riskyExposureWindowsWithRisk =
            filterRiskyExposureWindows(allExposureWindowsWithRisk, riskCalculation.riskThreshold)

        trackExposureWindowsMatchedAnalyticsIfNecessary(
            totalNumberOfExposureWindows = allExposureWindowsWithRisk.size,
            totalRisky = riskyExposureWindowsWithRisk.size
        )

        val mostRelevantExposure = evaluateMostRelevantExposure(riskyExposureWindowsWithRisk)

        return RiskCalculationResult(mostRelevantExposure, riskyExposureWindowsWithRisk)
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
    val exposureWindowsWithRisk: List<ExposureWindowWithRisk>
)

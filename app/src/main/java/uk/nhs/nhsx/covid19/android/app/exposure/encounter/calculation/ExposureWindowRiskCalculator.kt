package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import com.google.android.gms.nearby.exposurenotification.ScanInstance as GoogleScanInstance
import uk.nhs.riskscore.ScanInstance as NHSScanInstance

class ExposureWindowRiskCalculator(
    private val clock: Clock,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val riskScoreCalculatorProvider: RiskScoreCalculatorProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val analyticsEventScope: CoroutineScope
) {
    @Inject
    constructor(
        clock: Clock,
        isolationConfigurationProvider: IsolationConfigurationProvider,
        riskScoreCalculatorProvider: RiskScoreCalculatorProvider,
        analyticsEventProcessor: AnalyticsEventProcessor
    ) : this(
        clock,
        isolationConfigurationProvider,
        riskScoreCalculatorProvider,
        analyticsEventProcessor,
        analyticsEventScope = GlobalScope
    )

    operator fun invoke(
        exposureWindows: List<ExposureWindow>,
        riskCalculation: V2RiskCalculation,
        config: RiskScoreCalculatorConfiguration
    ): DayRisk? {
        Timber.d("Exposure windows: $exposureWindows")

        return exposureWindows.map { window ->
            ExposureWindowWithRisk(
                exposureWindow = window,
                startOfDayMillis = window.dateMillisSinceEpoch,
                calculatedRisk = window.riskScore(config, riskCalculation),
                riskCalculationVersion = riskScoreCalculatorProvider.getRiskCalculationVersion(),
                matchedKeyCount = 1
            )
        }
            .onEach {
                Timber.d("ExposureWindowWithRisk: $it isRecentExposure: ${it.isRecentExposure()}")
            }
            .also { listOfExposureWindowsWithRisk ->
                val totalRisky = listOfExposureWindowsWithRisk.count { isAboveRiskThreshold(it, riskCalculation) && it.isRecentExposure() }
                val totalNonRisky = listOfExposureWindowsWithRisk.size - totalRisky
                if (totalRisky > 0 || totalNonRisky > 0) {
                    trackExposureWindowAnalyticsEvent(totalRisky, totalNonRisky)
                }
            }
            .filter { it.isRecentExposure() }
            .also { listOfExposureWindowsWithRisk ->
                logHighestRisk(
                    listOfExposureWindowsWithRisk.map { it.calculatedRisk },
                    riskCalculation
                )
            }
            .filter { isAboveRiskThreshold(it, riskCalculation) }
            .let { listOfExposureWindowsWithRisk ->
                listOfExposureWindowsWithRisk
                    .maxWithOrNull(compareBy({ it.startOfDayMillis }, { it.calculatedRisk }))
                    ?.let { exposureWindowWithRisk ->
                        DayRisk(
                            startOfDayMillis = exposureWindowWithRisk.startOfDayMillis,
                            calculatedRisk = exposureWindowWithRisk.calculatedRisk,
                            riskCalculationVersion = exposureWindowWithRisk.riskCalculationVersion,
                            matchedKeyCount = exposureWindowWithRisk.matchedKeyCount,
                            exposureWindows = listOfExposureWindowsWithRisk.map { it.exposureWindow }
                        )
                    }
            }
    }

    private fun trackExposureWindowAnalyticsEvent(totalRiskyExposures: Int, totalNonRiskyExposures: Int) {
        analyticsEventScope.launch {
            analyticsEventProcessor.track(ExposureWindowsMatched(totalRiskyExposures, totalNonRiskyExposures))
        }
    }

    private fun logHighestRisk(
        calculatedRisks: List<Double>,
        riskCalculation: V2RiskCalculation
    ) {
        calculatedRisks.maxOrNull()
            ?.let {
                Timber.d("Calculated risk: $it threshold: ${riskCalculation.riskThreshold}")
            }
    }

    private fun ExposureWindow.riskScore(
        config: RiskScoreCalculatorConfiguration,
        riskCalculation: V2RiskCalculation
    ): Double {
        val scanInstances = scanInstances.map { it.toNHSScanInstance() }
        val riskScoreCalculator = riskScoreCalculatorProvider.riskScoreCalculator(config)
        return 60 * riskScoreCalculator.calculate(scanInstances) * infectiousnessFactor(
            riskCalculation
        )
    }

    private fun ExposureWindow.infectiousnessFactor(riskCalculation: V2RiskCalculation) =
        riskCalculation.infectiousnessWeights.getOrNull(infectiousness)
            ?: riskCalculation.infectiousnessWeights[0]

    private fun ExposureWindowWithRisk.isRecentExposure(): Boolean {
        val isolationPeriodAgo = LocalDate.now(clock).minusDays(isolationLength()).atStartOfDay()
        return encounterDate()
            .isBefore(isolationPeriodAgo)
            .not()
    }

    private fun isAboveRiskThreshold(
        exposureWindowWithRisk: ExposureWindowWithRisk,
        riskCalculation: V2RiskCalculation
    ) = exposureWindowWithRisk.calculatedRisk >= riskCalculation.riskThreshold

    private fun isolationLength() = isolationConfigurationProvider.durationDays.contactCase.toLong()

    private fun ExposureWindowWithRisk.encounterDate() =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(startOfDayMillis), ZoneOffset.UTC)

    data class ExposureWindowWithRisk(
        val exposureWindow: ExposureWindow,
        val startOfDayMillis: Long,
        val calculatedRisk: Double,
        val riskCalculationVersion: Int,
        val matchedKeyCount: Int
    )
}

private fun GoogleScanInstance.toNHSScanInstance() =
    NHSScanInstance(
        attenuationValue = minAttenuationDb,
        secondsSinceLastScan = secondsSinceLastScan
    )

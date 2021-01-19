package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import timber.log.Timber
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

class ExposureWindowRiskCalculator @Inject constructor(
    private val clock: Clock,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val riskScoreCalculatorProvider: RiskScoreCalculatorProvider
) {

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
            .filter { it.isRecentExposure() }
            .also { listOfExposureWindowsWithRisk ->
                logHighestRisk(
                    listOfExposureWindowsWithRisk.map { it.calculatedRisk },
                    riskCalculation
                )
            }
            .filter { it.calculatedRisk >= riskCalculation.riskThreshold }
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

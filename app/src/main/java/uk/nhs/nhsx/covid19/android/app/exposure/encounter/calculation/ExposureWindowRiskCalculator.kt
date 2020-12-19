package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData.ExposureWindowWithRisk
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW
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
    private val riskScoreCalculatorProvider: RiskScoreCalculatorProvider,
    private val submitEpidemiologyData: SubmitEpidemiologyData,
    private val epidemiologyEventProvider: EpidemiologyEventProvider
) {

    operator fun invoke(
        exposureWindows: List<ExposureWindow>,
        riskCalculation: V2RiskCalculation,
        config: RiskScoreCalculatorConfiguration
    ): DayRisk? {
        Timber.d("Exposure windows: $exposureWindows")

        return exposureWindows.map { window ->
            ExposureWindowWithRisk(
                dayRisk = DayRisk(
                    startOfDayMillis = window.dateMillisSinceEpoch,
                    calculatedRisk = window.riskScore(config, riskCalculation),
                    riskCalculationVersion = riskScoreCalculatorProvider.getRiskCalculationVersion()
                ),
                exposureWindow = window
            )
        }
            .also { exposureWindowsWithRisk ->
                exposureWindowsWithRisk.forEach {
                    Timber.d("DayRisk: ${it.dayRisk} for window: ${it.exposureWindow} isRecentExposure: ${it.dayRisk.isRecentExposure()}")
                }
            }
            .filter { it.dayRisk.isRecentExposure() }
            .also { exposureWindowsWithRisk ->
                logHighestRisk(
                    exposureWindowsWithRisk.map { it.dayRisk },
                    riskCalculation
                )
            }
            .filter { it.dayRisk.calculatedRisk >= riskCalculation.riskThreshold }
            .also { exposureWindowsWithRisk ->
                exposureWindowsWithRisk
                    .map { it.toEpidemiologyEvent() }
                    .apply {
                        storeEpidemiologyEvents(this)
                        submitEpidemiologyEvents(this)
                    }
            }
            .map { it.dayRisk }
            .maxWith(compareBy({ it.startOfDayMillis }, { it.calculatedRisk }))
    }

    private fun storeEpidemiologyEvents(epidemiologyEvents: List<EpidemiologyEvent>) {
        if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS) && epidemiologyEvents.isNotEmpty()) {
            epidemiologyEventProvider.add(epidemiologyEvents)
        }
    }

    private fun submitEpidemiologyEvents(epidemiologyEvents: List<EpidemiologyEvent>) {
        runCatching {
            submitEpidemiologyData(epidemiologyEvents, epidemiologyEventType = EXPOSURE_WINDOW)
        }.getOrElse {
            Timber.e(it, "Epidemiology submission failed")
        }
    }

    private fun logHighestRisk(
        risks: List<DayRisk>,
        riskCalculation: V2RiskCalculation
    ) {
        risks.maxBy { it.calculatedRisk }
            ?.let {
                Timber.d("Calculated risk: ${it.calculatedRisk} threshold: ${riskCalculation.riskThreshold}")
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

    private fun DayRisk.isRecentExposure(): Boolean {
        val isolationPeriodAgo = LocalDate.now(clock).minusDays(isolationLength()).atStartOfDay()
        return encounterDate()
            .isBefore(isolationPeriodAgo)
            .not()
    }

    private fun isolationLength() = isolationConfigurationProvider.durationDays.contactCase.toLong()

    private fun DayRisk.encounterDate() =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(startOfDayMillis), ZoneOffset.UTC)
}

private fun GoogleScanInstance.toNHSScanInstance() =
    NHSScanInstance(
        attenuationValue = minAttenuationDb,
        secondsSinceLastScan = secondsSinceLastScan
    )

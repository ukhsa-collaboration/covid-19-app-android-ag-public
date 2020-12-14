package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData.ExposureWindowWithRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.convert
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
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
    private val submitEpidemiologyData: SubmitEpidemiologyData,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val submitEpidemiologyDataScope: CoroutineScope
) {

    @Inject
    constructor(
        clock: Clock,
        isolationConfigurationProvider: IsolationConfigurationProvider,
        riskScoreCalculatorProvider: RiskScoreCalculatorProvider,
        submitEpidemiologyData: SubmitEpidemiologyData,
        epidemiologyEventProvider: EpidemiologyEventProvider
    ) : this(
        clock,
        isolationConfigurationProvider,
        riskScoreCalculatorProvider,
        submitEpidemiologyData,
        epidemiologyEventProvider,
        submitEpidemiologyDataScope = GlobalScope
    )

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
            .also {
                it.forEach { exposureWindowWithRisk ->
                    Timber.d("DayRisk: ${exposureWindowWithRisk.dayRisk} for window: ${exposureWindowWithRisk.exposureWindow} isRecentExposure: ${exposureWindowWithRisk.dayRisk.isRecentExposure()}")
                }
            }
            .filter { it.dayRisk.isRecentExposure() }
            .also {
                logHighestRisk(
                    it.map { exposureWindowWithRisk -> exposureWindowWithRisk.dayRisk },
                    riskCalculation
                )
            }
            .filter { it.dayRisk.calculatedRisk >= riskCalculation.riskThreshold }
            .also { list ->
                if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS)) {
                    list.map { it.convert(EpidemiologyEventType.EXPOSURE_WINDOW) }
                        .apply { epidemiologyEventProvider.add(this) }
                }
            }
            .also {
                submitEpidemiologyDataScope.launch {
                    runCatching {
                        submitEpidemiologyData(it)
                    }.getOrElse {
                        Timber.e(it, "Epidemiology submission failed")
                    }
                }
            }
            .map { it.dayRisk }
            .maxWith(compareBy({ it.startOfDayMillis }, { it.calculatedRisk }))
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

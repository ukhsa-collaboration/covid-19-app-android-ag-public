package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.MAX_TRANSMISSION_RISK_LEVEL
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation

class RiskCalculator(
    private val infectiousnessFactorCalculator: InfectiousnessFactorCalculator = InfectiousnessFactorCalculator()
) {

    operator fun invoke(
        exposureInfos: List<ExposureInformation>,
        riskCalculation: RiskCalculation
    ): DayRisk? {
        Timber.d("DurationBucketWeights: ${riskCalculation.durationBucketWeights}")

        return exposureInfos.groupBy { it.dateMillisSinceEpoch }
            .toSortedMap(Comparator { o1: Long, o2: Long -> if (o1 < o2) 1 else if (o1 == o2) 0 else -1 })
            .map { (startOfDayMillis, exposures) ->
                val maxRisk = exposures.map { exposureInformation ->
                    risk(
                        exposureInformation,
                        riskCalculation
                    )
                }.max() ?: 0.0
                DayRisk(
                    startOfDayMillis,
                    maxRisk
                )
            }.firstOrNull { it.calculatedRisk >= riskCalculation.riskThreshold }
    }

    private fun risk(
        exposureInformation: ExposureInformation,
        riskCalculation: RiskCalculation
    ): Double {
        val weightedDurations = weightedDurations(exposureInformation, riskCalculation)
        val infectiousnessFactor = infectiousnessFactor(exposureInformation)
        return infectiousnessFactor * weightedDurations.sum()
    }

    private fun infectiousnessFactor(exposureInformation: ExposureInformation): Double {
        val daysFromOnset = MAX_TRANSMISSION_RISK_LEVEL - exposureInformation.transmissionRiskLevel
        return infectiousnessFactorCalculator.infectiousnessFactor(daysFromOnset)
    }

    private fun weightedDurations(
        exposureInformation: ExposureInformation,
        riskCalculation: RiskCalculation
    ): List<Double> {
        val durations =
            exposureInformation.attenuationDurationsInMinutes.map { minutesToSeconds(it) }
        Timber.d("Durations: $durations")
        return durations.zip(riskCalculation.durationBucketWeights)
            .map { it.first * it.second }
    }

    private fun minutesToSeconds(minutes: Int) = minutes * 60.0
}

data class DayRisk(val startOfDayMillis: Long, val calculatedRisk: Double)

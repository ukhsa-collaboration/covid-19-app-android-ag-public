package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation
import kotlin.math.max

class RiskCalculator {

    operator fun invoke(
        exposureInfos: List<ExposureInformation>,
        riskCalculation: RiskCalculation
    ): Pair<Long, Double>? {
        Timber.d("DurationBucketWeights: ${riskCalculation.durationBucketWeights}")

        return exposureInfos.groupBy { it.dateMillisSinceEpoch }
            .toSortedMap(Comparator { o1: Long, o2: Long -> if (o1 < o2) 1 else if (o1 == o2) 0 else -1 })
            .map { entry ->
                val maxRisk = entry.value.map { exposureInformation ->
                    risk(
                        exposureInformation,
                        riskCalculation
                    )
                }.max() ?: 0.0
                entry.key to maxRisk
            }.firstOrNull { it.second >= riskCalculation.riskThreshold }
    }

    private fun risk(
        exposureInformation: ExposureInformation,
        riskCalculation: RiskCalculation
    ): Double {
        val durations =
            exposureInformation.attenuationDurationsInMinutes.map { minutesToSeconds(it) }
        Timber.d("Durations: $durations")
        val weightedDurations =
            durations.zip(riskCalculation.durationBucketWeights)
                .map { it.first * it.second }
        return weightedDurations.reduce { acc, d -> acc + d }
    }

    private fun minutesToSeconds(minutes: Int) = minutes * 60.0
}

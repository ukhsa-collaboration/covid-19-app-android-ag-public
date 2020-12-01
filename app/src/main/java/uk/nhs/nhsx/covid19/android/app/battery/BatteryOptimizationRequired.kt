package uk.nhs.nhsx.covid19.android.app.battery

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import javax.inject.Inject

class BatteryOptimizationRequired @Inject constructor(
    private val batteryOptimizationChecker: BatteryOptimizationChecker,
    private val batteryOptimizationAcknowledgementProvider: BatteryOptimizationAcknowledgementProvider
) {

    operator fun invoke() =
        !batteryOptimizationChecker.isIgnoringBatteryOptimizations() &&
            batteryOptimizationAcknowledgementProvider.value == null &&
            RuntimeBehavior.isFeatureEnabled(FeatureFlag.BATTERY_OPTIMIZATION)
}

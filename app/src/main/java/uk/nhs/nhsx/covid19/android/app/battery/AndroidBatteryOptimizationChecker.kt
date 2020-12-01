package uk.nhs.nhsx.covid19.android.app.battery

import android.content.Context
import android.os.PowerManager

class AndroidBatteryOptimizationChecker constructor(
    private val context: Context
) : BatteryOptimizationChecker {

    override fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}

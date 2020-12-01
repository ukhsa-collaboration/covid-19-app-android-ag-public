package uk.nhs.nhsx.covid19.android.app.battery

interface BatteryOptimizationChecker {

    fun isIgnoringBatteryOptimizations(): Boolean
}

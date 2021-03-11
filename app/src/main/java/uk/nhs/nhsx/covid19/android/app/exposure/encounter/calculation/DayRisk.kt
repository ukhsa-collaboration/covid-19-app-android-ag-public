package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

data class DayRisk(
    val startOfDayMillis: Long,
    val calculatedRisk: Double,
    val riskCalculationVersion: Int,
    val matchedKeyCount: Int
)

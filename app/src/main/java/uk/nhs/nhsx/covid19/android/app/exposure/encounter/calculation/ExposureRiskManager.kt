package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

interface ExposureRiskManager {
    suspend fun getRisk(token: String): DayRisk?
}

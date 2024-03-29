package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IsolationConfigurationResponse(
    @Json(name = "england")
    val englandConfiguration: CountrySpecificConfiguration,
    @Json(name = "wales_v2")
    val walesConfiguration: CountrySpecificConfiguration,
)

@JsonClass(generateAdapter = true)
data class CountrySpecificConfiguration(
    val contactCase: Int,
    val indexCaseSinceSelfDiagnosisOnset: Int,
    val indexCaseSinceSelfDiagnosisUnknownOnset: Int,
    val maxIsolation: Int,
    val indexCaseSinceTestResultEndDate: Int,
    val pendingTasksRetentionPeriod: Int,
    val testResultPollingTokenRetentionPeriod: Int
)

@JsonClass(generateAdapter = true)
data class DurationDays(
    val england: CountrySpecificConfiguration = CountrySpecificConfiguration(
        contactCase = 11,
        indexCaseSinceSelfDiagnosisOnset = 6,
        indexCaseSinceSelfDiagnosisUnknownOnset = 4,
        maxIsolation = 16,
        indexCaseSinceTestResultEndDate = 6,
        pendingTasksRetentionPeriod = 14,
        testResultPollingTokenRetentionPeriod = 28
    ),
    val wales: CountrySpecificConfiguration = CountrySpecificConfiguration(
        contactCase = 11,
        indexCaseSinceSelfDiagnosisOnset = 5,
        indexCaseSinceSelfDiagnosisUnknownOnset = 5,
        maxIsolation = 16,
        indexCaseSinceTestResultEndDate = 5,
        pendingTasksRetentionPeriod = 14,
        testResultPollingTokenRetentionPeriod = 28
    )
)

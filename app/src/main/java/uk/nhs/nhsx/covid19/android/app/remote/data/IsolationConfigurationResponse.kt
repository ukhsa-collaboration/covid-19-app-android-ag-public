package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IsolationConfigurationResponse(
    val durationDays: DurationDays
)

@JsonClass(generateAdapter = true)
data class DurationDays(
    val contactCase: Int = 11,
    val indexCaseSinceSelfDiagnosisOnset: Int = 11,
    val indexCaseSinceSelfDiagnosisUnknownOnset: Int = 9,
    val maxIsolation: Int = 21,
    val pendingTasksRetentionPeriod: Int = 14,
    val indexCaseSinceTestResultEndDate: Int = 11
)

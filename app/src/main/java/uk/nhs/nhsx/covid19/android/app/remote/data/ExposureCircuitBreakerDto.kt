package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult

@JsonClass(generateAdapter = true)
data class ExposureCircuitBreakerRequest(
    val matchedKeyCount: Int,
    val daysSinceLastExposure: Int,
    val maximumRiskScore: Double,
    val riskCalculationVersion: Int
)

@JsonClass(generateAdapter = true)
data class ExposureCircuitBreakerResponse(
    val approvalToken: String,
    val approval: CircuitBreakerResult
)

@JsonClass(generateAdapter = true)
data class ExposureCircuitBreakerPollingResponse(
    val approval: CircuitBreakerResult
)

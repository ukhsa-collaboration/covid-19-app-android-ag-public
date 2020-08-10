package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult

@JsonClass(generateAdapter = true)
data class RiskyVenuesCircuitBreakerRequest(
    val venueId: String
)

@JsonClass(generateAdapter = true)
data class RiskyVenuesCircuitBreakerResponse(
    val approvalToken: String,
    val approval: CircuitBreakerResult
)

@JsonClass(generateAdapter = true)
data class RiskyVenuesCircuitBreakerPollingResponse(
    val approval: CircuitBreakerResult
)

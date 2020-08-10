package uk.nhs.nhsx.covid19.android.app.common

import com.squareup.moshi.Json

enum class CircuitBreakerResult {
    @Json(name = "yes")
    YES,
    @Json(name = "no")
    NO,
    @Json(name = "pending")
    PENDING
}

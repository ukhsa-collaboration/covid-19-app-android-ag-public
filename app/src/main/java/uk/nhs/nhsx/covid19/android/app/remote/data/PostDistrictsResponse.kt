package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostDistrictsResponse(val postDistricts: Map<String, RiskLevel>)

enum class RiskLevel {
    @Json(name = "L")
    LOW,

    @Json(name = "M")
    MEDIUM,

    @Json(name = "H")
    HIGH
}

package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class EpidemiologyData(
    val metadata: Metadata,
    val events: List<EpidemiologyEvent>
)

@JsonClass(generateAdapter = true)
data class EpidemiologyEvent(
    val type: EpidemiologyEventType,
    val version: Int,
    val payload: EpidemiologyEventPayload
)

@JsonClass(generateAdapter = true)
data class EpidemiologyEventPayload(
    val date: Instant,
    val infectiousness: Infectiousness,
    val scanInstances: List<EpidemiologyEventPayloadScanInstance>,
    val riskScore: Double,
    val riskCalculationVersion: Int
)

@JsonClass(generateAdapter = true)
data class EpidemiologyEventPayloadScanInstance(
    val minimumAttenuation: Int,
    val secondsSinceLastScan: Int,
    val typicalAttenuation: Int
)

enum class Infectiousness {
    @Json(name = "none")
    NONE,

    @Json(name = "standard")
    STANDARD,

    @Json(name = "high")
    HIGH;

    companion object {
        fun fromInt(intValue: Int) =
            when (intValue) {
                0 -> NONE
                1 -> STANDARD
                2 -> HIGH
                else -> NONE
            }
    }
}

enum class EpidemiologyEventType {
    @Json(name = "exposureWindow")
    EXPOSURE_WINDOW,

    @Json(name = "exposureWindowPositiveTest")
    EXPOSURE_WINDOW_POSITIVE_TEST
}

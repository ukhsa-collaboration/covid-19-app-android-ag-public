package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class RiskyVenuesResponse(val venues: List<RiskyVenue>)

@JsonClass(generateAdapter = true)
data class RiskyVenue(val id: String, val riskyWindow: RiskyWindow)

@JsonClass(generateAdapter = true)
data class RiskyWindow(val from: Instant, @Json(name = "until") val to: Instant)

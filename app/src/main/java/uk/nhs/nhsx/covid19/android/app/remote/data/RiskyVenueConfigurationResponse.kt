package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RiskyVenueConfigurationResponse(
    val durationDays: RiskyVenueConfigurationDurationDays
)

@JsonClass(generateAdapter = true)
data class RiskyVenueConfigurationDurationDays(
    val optionToBookATest: Int = 10
)

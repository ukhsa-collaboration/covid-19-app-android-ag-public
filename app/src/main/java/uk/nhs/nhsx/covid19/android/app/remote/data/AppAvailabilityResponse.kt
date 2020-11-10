package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.common.Translatable

@JsonClass(generateAdapter = true)
data class AppAvailabilityResponse(
    val minimumAppVersion: MinimumAppVersion,
    @Json(name = "minimumSDKVersion")
    val minimumSdkVersion: MinimumSdkVersion,
    val recommendedAppVersion: RecommendedAppVersion
)

@JsonClass(generateAdapter = true)
data class MinimumAppVersion(
    val description: Translatable,
    val value: Int
)

@JsonClass(generateAdapter = true)
data class MinimumSdkVersion(
    val description: Translatable,
    val value: Int
)

@JsonClass(generateAdapter = true)
data class RecommendedAppVersion(
    val description: Translatable,
    val value: Int,
    val title: Translatable
)

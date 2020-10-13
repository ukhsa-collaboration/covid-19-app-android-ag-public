package uk.nhs.nhsx.covid19.android.app.remote.data

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.common.Translatable

@Parcelize
@JsonClass(generateAdapter = true)
data class RiskIndicator(
    val colorScheme: ColorScheme,
    val name: Translatable,
    val heading: Translatable,
    val content: Translatable,
    val linkTitle: Translatable,
    val linkUrl: Translatable
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class RiskIndicatorWrapper(
    val riskLevel: String? = null,
    val riskIndicator: RiskIndicator? = null,
    val oldRiskLevel: RiskLevel? = null
) : Parcelable

@Parcelize
enum class ColorScheme : Parcelable {
    @Json(name = "neutral")
    NEUTRAL,

    @Json(name = "green")
    GREEN,

    @Json(name = "yellow")
    YELLOW,

    @Json(name = "amber")
    AMBER,

    @Json(name = "red")
    RED
}

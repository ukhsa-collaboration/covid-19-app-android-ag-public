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
    val linkUrl: Translatable,
    val policyData: PolicyData?
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class PolicyData(
    val heading: Translatable,
    val content: Translatable,
    val footer: Translatable,
    val localAuthorityRiskTitle: Translatable,
    val policies: List<Policy>
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class Policy(
    val policyIcon: PolicyIcon,
    val policyHeading: Translatable,
    val policyContent: Translatable
) : Parcelable

@Parcelize
enum class PolicyIcon(
    val jsonName: String
) : Parcelable {
    DEFAULT("default-icon"),

    MEETING_PEOPLE("meeting-people"),

    BARS_AND_PUBS("bars-and-pubs"),

    WORSHIP("worship"),

    OVERNIGHT_STAYS("overnight-stays"),

    EDUCATION("education"),

    TRAVELLING("travelling"),

    EXERCISE("exercise"),

    WEDDINGS_AND_FUNERALS("weddings-and-funerals")
}

@Parcelize
@JsonClass(generateAdapter = true)
data class RiskIndicatorWrapper(
    val riskLevel: String? = null,
    val riskIndicator: RiskIndicator? = null,
    val riskLevelFromLocalAuthority: Boolean = false
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

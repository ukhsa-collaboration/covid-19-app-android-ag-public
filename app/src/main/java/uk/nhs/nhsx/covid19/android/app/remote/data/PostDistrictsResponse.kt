package uk.nhs.nhsx.covid19.android.app.remote.data

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
data class PostDistrictsResponse(val postDistricts: Map<String, RiskLevel>)

@Parcelize
enum class RiskLevel : Parcelable {
    @Json(name = "L")
    LOW,

    @Json(name = "M")
    MEDIUM,

    @Json(name = "H")
    HIGH
}

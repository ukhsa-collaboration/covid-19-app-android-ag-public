package uk.nhs.nhsx.covid19.android.app.qrcode

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Venue(
    val id: String,
    @Json(name = "opn")
    val organizationPartName: String,
    @Json(name = "pc")
    val postCode: String? = null
)

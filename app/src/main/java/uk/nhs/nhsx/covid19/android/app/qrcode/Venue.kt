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
) {
    val formattedPostCode: String?
        get() {
            if (postCode == null) return null

            if (postCode.length > 4 && !postCode.contains(" ")) {
                val sb = StringBuilder(postCode)
                val position = sb.length - 3
                sb.insert(position, " ")
                return sb.toString()
            }
            return postCode
        }
}

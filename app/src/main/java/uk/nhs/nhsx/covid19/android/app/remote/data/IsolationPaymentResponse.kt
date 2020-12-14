package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class IsolationPaymentCountry {
    @Json(name = "England")
    ENGLAND,

    @Json(name = "Wales")
    WALES
}

@JsonClass(generateAdapter = true)
data class IsolationPaymentCreateTokenResponse(
    val isEnabled: Boolean,
    val ipcToken: String?
)

@JsonClass(generateAdapter = true)
data class IsolationPaymentUrlResponse(
    val websiteUrlWithQuery: String
)

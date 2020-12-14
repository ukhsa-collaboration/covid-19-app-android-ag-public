package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IsolationPaymentCreateTokenRequest(
    val country: IsolationPaymentCountry
)

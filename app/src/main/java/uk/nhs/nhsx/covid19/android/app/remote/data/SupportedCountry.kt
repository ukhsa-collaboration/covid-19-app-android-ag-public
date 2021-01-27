package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Json

enum class SupportedCountry {
    @Json(name = "England")
    ENGLAND,

    @Json(name = "Wales")
    WALES
}

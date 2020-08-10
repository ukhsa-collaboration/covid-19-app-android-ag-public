package uk.nhs.nhsx.covid19.android.app.report.output

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Screenshot(
    val fileName: String,
    val tags: List<String>
)

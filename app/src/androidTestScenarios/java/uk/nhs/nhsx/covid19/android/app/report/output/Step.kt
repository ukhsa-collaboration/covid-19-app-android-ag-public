package uk.nhs.nhsx.covid19.android.app.report.output

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Step(
    val name: String,
    val description: String,
    val screenshots: List<Screenshot>
)

package uk.nhs.nhsx.covid19.android.app.report.output

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Report(
    val description: String,
    val kind: String,
    val name: String,
    val scenario: String,
    val steps: List<Step>
)

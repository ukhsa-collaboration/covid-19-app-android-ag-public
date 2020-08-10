package uk.nhs.nhsx.covid19.android.app.report.output

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.report.Reporter

@JsonClass(generateAdapter = true)
data class Report(
    val description: String,
    val kind: Reporter.Kind,
    val name: String,
    val scenario: String,
    val steps: List<Step>
)

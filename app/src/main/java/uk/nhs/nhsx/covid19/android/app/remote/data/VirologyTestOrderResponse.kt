package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VirologyTestOrderResponse(
    val websiteUrlWithQuery: String,
    val tokenParameterValue: String,
    val testResultPollingToken: String,
    val diagnosisKeySubmissionToken: String
)

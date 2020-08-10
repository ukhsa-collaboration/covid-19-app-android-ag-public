package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TemporaryExposureKeysPayload(
    val diagnosisKeySubmissionToken: String,
    val temporaryExposureKeys: List<NHSTemporaryExposureKey>
)

@JsonClass(generateAdapter = true)
data class NHSTemporaryExposureKey(
    val key: String,
    val rollingStartNumber: Int,
    val rollingPeriod: Int = 144
)

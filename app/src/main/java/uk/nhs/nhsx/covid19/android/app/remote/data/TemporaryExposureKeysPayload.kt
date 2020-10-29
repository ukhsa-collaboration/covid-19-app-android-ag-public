package uk.nhs.nhsx.covid19.android.app.remote.data

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
data class TemporaryExposureKeysPayload(
    val diagnosisKeySubmissionToken: String,
    val temporaryExposureKeys: List<NHSTemporaryExposureKey>
)

@JsonClass(generateAdapter = true)
@Parcelize
data class NHSTemporaryExposureKey(
    val key: String,
    val rollingStartNumber: Int,
    val rollingPeriod: Int = 144,
    val transmissionRiskLevel: Int? = null,
    val daysSinceOnsetOfSymptoms: Int? = null
) : Parcelable

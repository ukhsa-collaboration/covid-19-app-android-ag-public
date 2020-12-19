package uk.nhs.nhsx.covid19.android.app.remote.data

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
data class EmptySubmissionRequest(
    val source: EmptySubmissionSource
)

@Parcelize
enum class EmptySubmissionSource : Parcelable {
    @Json(name = "circuitBreaker")
    CIRCUIT_BREAKER,

    @Json(name = "keySubmission")
    KEY_SUBMISSION,

    @Json(name = "exposureWindow")
    EXPOSURE_WINDOW,

    @Json(name = "exposureWindowAfterPositive")
    EXPOSURE_WINDOW_AFTER_POSITIVE
}

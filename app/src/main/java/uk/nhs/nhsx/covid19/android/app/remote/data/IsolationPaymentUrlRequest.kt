package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class IsolationPaymentUrlRequest(
    val ipcToken: String,
    val riskyEncounterDate: Instant,
    val isolationPeriodEndDate: Instant
)

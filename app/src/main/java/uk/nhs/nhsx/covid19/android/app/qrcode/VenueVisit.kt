package uk.nhs.nhsx.covid19.android.app.qrcode

import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class VenueVisit(val venue: Venue, val from: Instant, val to: Instant, val wasInRiskyList: Boolean = false)

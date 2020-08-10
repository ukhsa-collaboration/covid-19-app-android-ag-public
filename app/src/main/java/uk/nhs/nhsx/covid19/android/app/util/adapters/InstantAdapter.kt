package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantAdapter {
    @ToJson
    fun toJson(instant: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }

    @FromJson
    fun fromJson(instant: String): Instant {
        return try {
            Instant.parse(instant)
        } catch (exception: Exception) {
            throw JsonDataException("Wrong instant format: $instant")
        }
    }
}

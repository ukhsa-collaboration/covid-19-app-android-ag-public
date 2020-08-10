package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import java.time.LocalDate

class LocalDateAdapter {
    @ToJson
    fun toJson(localDate: LocalDate): String {
        return localDate.toString()
    }

    @FromJson
    fun fromJson(localDate: String): LocalDate {
        return try {
            LocalDate.parse(localDate)
        } catch (exception: Exception) {
            throw JsonDataException("Wrong LocalDate format: $localDate")
        }
    }
}

package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL

class ColorSchemeAdapter {
    @ToJson
    fun toJson(colorScheme: ColorScheme): String {
        return colorScheme.jsonName
    }

    @FromJson
    fun fromJson(colorScheme: String): ColorScheme {
        return ColorScheme.values().firstOrNull { it.jsonName == colorScheme } ?: NEUTRAL
    }
}

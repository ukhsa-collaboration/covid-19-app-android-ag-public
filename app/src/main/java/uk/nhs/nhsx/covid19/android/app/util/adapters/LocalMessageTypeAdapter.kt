package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType.UNKNOWN

class LocalMessageTypeAdapter {
    @ToJson
    fun toJson(localMessageType: LocalMessageType): String {
        return localMessageType.jsonName
    }

    @FromJson
    fun fromJson(localMessageType: String): LocalMessageType {
        return LocalMessageType.values().firstOrNull { it.jsonName == localMessageType } ?: UNKNOWN
    }
}

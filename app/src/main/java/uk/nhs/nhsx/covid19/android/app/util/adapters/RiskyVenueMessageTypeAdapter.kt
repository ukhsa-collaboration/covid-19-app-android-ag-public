package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM

class RiskyVenueMessageTypeAdapter {
    @ToJson
    fun toJson(messageType: RiskyVenueMessageType): String {
        return messageType.jsonName
    }

    @FromJson
    fun fromJson(messageType: String): RiskyVenueMessageType {
        return RiskyVenueMessageType.values().firstOrNull { it.jsonName == messageType } ?: INFORM
    }
}

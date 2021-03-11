package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM

class RiskyVenueMessageTypeAdapter {
    @ToJson
    fun toJson(messageType: MessageType): String {
        return messageType.jsonName
    }

    @FromJson
    fun fromJson(messageType: String): MessageType {
        return MessageType.values().firstOrNull { it.jsonName == messageType } ?: INFORM
    }
}

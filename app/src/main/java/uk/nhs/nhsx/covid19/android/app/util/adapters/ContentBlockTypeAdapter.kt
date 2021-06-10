package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.UNKNOWN

class ContentBlockTypeAdapter {
    @ToJson
    fun toJson(contentBlockType: ContentBlockType): String {
        return contentBlockType.jsonName
    }

    @FromJson
    fun fromJson(contentBlockType: String): ContentBlockType {
        return ContentBlockType.values().firstOrNull { it.jsonName == contentBlockType } ?: UNKNOWN
    }
}

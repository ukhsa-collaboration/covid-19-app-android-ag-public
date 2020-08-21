package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.common.Translatable

class TranslatableAdapter {

    @ToJson
    fun toJson(translatable: Translatable): Map<String, String> {
        return translatable.translations
    }

    @FromJson
    fun fromJson(translatableJson: Map<String, String>): Translatable {
        return Translatable(translatableJson)
    }
}

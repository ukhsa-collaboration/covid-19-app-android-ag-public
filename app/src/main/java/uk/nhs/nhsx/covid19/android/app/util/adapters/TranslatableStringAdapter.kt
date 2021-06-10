package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString

class TranslatableStringAdapter {

    @ToJson
    fun toJson(translatableString: TranslatableString): Map<String, String> {
        return translatableString.translations
    }

    @FromJson
    fun fromJson(translatableJson: Map<String, String>): TranslatableString {
        return TranslatableString(translatableJson)
    }
}

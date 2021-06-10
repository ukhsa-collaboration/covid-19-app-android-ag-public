package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableLocalMessage

class TranslatableLocalMessageAdapter {

    @ToJson
    fun toJson(translatable: TranslatableLocalMessage): Map<String, LocalMessageTranslation> {
        return translatable.translations
    }

    @FromJson
    fun fromJson(translatableJson: Map<String, LocalMessageTranslation>): TranslatableLocalMessage {
        return TranslatableLocalMessage(translatableJson)
    }
}

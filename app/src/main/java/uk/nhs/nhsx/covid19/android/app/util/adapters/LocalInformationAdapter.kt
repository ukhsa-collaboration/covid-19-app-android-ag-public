package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Notification
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Unknown
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType.NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType.UNKNOWN
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableNotificationMessage

class LocalInformationAdapter {

    @ToJson
    fun toJson(localInformation: LocalInformation): LocalMessage {
        return when (localInformation) {
            is Notification -> LocalMessage(
                type = NOTIFICATION,
                updated = localInformation.updated,
                contentVersion = localInformation.contentVersion,
                translations = localInformation.translations.translations.mapValues {
                    with(it.value) { LocalMessageTranslation(head = head, body = body, content = content) }
                }
            )
            else -> LocalMessage(type = UNKNOWN, null, null, null)
        }
    }

    @FromJson
    fun fromJson(localMessage: LocalMessage): LocalInformation {
        with(localMessage) {
            if (type == NOTIFICATION && updated != null && contentVersion != null && translations != null) {
                return Notification(
                    updated,
                    contentVersion,
                    translations = translatableNotificationMessage(translations)
                )
            }
        }
        return Unknown
    }

    private fun translatableNotificationMessage(translations: Map<String, LocalMessageTranslation>): TranslatableNotificationMessage {
        val validTranslations = translations.filterValues {
            it.head != null && it.body != null && it.content != null
        }.mapValues {
            with(it.value) {
                NotificationMessage(head = head!!, body = body!!, content = content!!)
            }
        }
        return TranslatableNotificationMessage(validTranslations)
    }
}

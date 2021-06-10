package uk.nhs.nhsx.covid19.android.app.status

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType.NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import javax.inject.Inject

class GetFirstMessageOfTypeNotification @Inject constructor(
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val substitutePlaceholdersInMessageWithId: SubstitutePlaceholdersInMessageWithId
) {
    suspend operator fun invoke(localMessagesResponse: LocalMessagesResponse?): MessageWithId? {
        if (localMessagesResponse == null) return null

        val localAuthority = localAuthorityProvider.value
        val messageIdsForLocalAuthority = localMessagesResponse.localAuthorities[localAuthority]
        val messageIdsForWildcard = localMessagesResponse.localAuthorities[WILDCARD]

        if ((containsNoMessageIds(messageIdsForLocalAuthority)) && containsNoMessageIds(messageIdsForWildcard)) {
            Timber.d("No messages for local authority \"$localAuthority\"")
            return null
        }

        val message = localMessagesResponse.firstNotificationMessageMatchingIds(messageIdsForLocalAuthority)
            ?: localMessagesResponse.firstNotificationMessageMatchingIds(messageIdsForWildcard)

        return message
            ?.let { substitutePlaceholdersInMessageWithId(it) }
    }

    private fun containsNoMessageIds(messageIds: List<String>?) =
        messageIds == null || messageIds.isEmpty()

    private fun LocalMessagesResponse.firstNotificationMessageMatchingIds(
        messageIdsForLocalAuthority: List<String>?
    ): MessageWithId? =
        if (messageIdsForLocalAuthority == null) null
        else messageIdsForLocalAuthority
            .firstOrNull { messages.containsKey(it) && messages[it]!!.type == NOTIFICATION }
            ?.let { MessageWithId(messageId = it, message = messages[it]!!) }

    companion object {
        private const val WILDCARD = "*"
    }
}

data class MessageWithId(val messageId: String, val message: LocalMessage)

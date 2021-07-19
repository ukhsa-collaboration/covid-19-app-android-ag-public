package uk.nhs.nhsx.covid19.android.app.status

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.GetLocalAuthorityName
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import javax.inject.Inject

class SubstitutePlaceholdersInNotificationWithId @Inject constructor(
    private val postCodeProvider: PostCodeProvider,
    private val getLocalAuthorityName: GetLocalAuthorityName
) {
    suspend operator fun invoke(notificationWithId: NotificationWithId): NotificationWithId? {
        val postCode = postCodeProvider.value
        val localAuthorityName = getLocalAuthorityName()

        if (postCode == null || localAuthorityName == null) {
            Timber.d("Post code or local authority name not found")
            return null
        }

        return notificationWithId.copy(
            message = notificationWithId.message.copy(
                translations = notificationWithId.message.translations.replacePlaceholders(
                    postCode,
                    localAuthorityName
                )
            )
        )
    }
}

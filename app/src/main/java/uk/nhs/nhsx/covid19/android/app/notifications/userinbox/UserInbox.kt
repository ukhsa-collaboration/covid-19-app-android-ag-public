package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserInbox @Inject constructor(
    private val fetchUserInboxItem: FetchUserInboxItem,
    migrateRiskyVenueIdProvider: MigrateRiskyVenueIdProvider,
) {
    init {
        migrateRiskyVenueIdProvider()
    }

    fun fetchInbox(): UserInboxItem? = fetchUserInboxItem()
}

sealed class UserInboxItem {
    object ShowTestResult : UserInboxItem()
    object ShowUnknownTestResult : UserInboxItem()
    object ContinueInitialKeySharing : UserInboxItem()
    object ShowKeySharingReminder : UserInboxItem()
    data class ShowIsolationExpiration(val expirationDate: LocalDate) : UserInboxItem()
    data class ShowVenueAlert(val venueId: String, val messageType: MessageType) : UserInboxItem()
    object ShowEncounterDetection : UserInboxItem()
}

@Deprecated("Not used anymore since 4.6. Use RiskyVenueProvider instead.")
class RiskyVenueIdProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs = sharedPreferences.with<String>(RISKY_VENUE_ID)

    var value: String? by prefs

    companion object {
        const val RISKY_VENUE_ID = "RISKY_VENUE_ID"
    }
}

class ShouldShowEncounterDetectionActivityProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs = sharedPreferences.with<Boolean>(SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY)

    var value: Boolean? by prefs

    companion object {
        const val SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY = "SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY"
    }
}

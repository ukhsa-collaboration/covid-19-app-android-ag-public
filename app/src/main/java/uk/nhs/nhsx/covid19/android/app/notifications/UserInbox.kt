package uk.nhs.nhsx.covid19.android.app.notifications

import android.content.SharedPreferences
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with

sealed class UserInboxItem {
    object ShowTestResult : UserInboxItem()
}

sealed class AddableUserInboxItem : UserInboxItem() {
    data class ShowIsolationExpiration(val expirationDate: LocalDate) : AddableUserInboxItem()
    data class ShowVenueAlert(val venueId: String, val messageType: MessageType) : AddableUserInboxItem()
    object ShowEncounterDetection : AddableUserInboxItem()
}

@Singleton
class UserInbox @Inject constructor(
    private val isolationExpirationDateProvider: IsolationExpirationDateProvider,
    @Suppress("DEPRECATION") private val riskyVenueIdProvider: RiskyVenueIdProvider,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider
) {

    init {
        migrateRiskyVenueIdProvider()
    }

    private fun migrateRiskyVenueIdProvider() {
        riskyVenueIdProvider.value?.let {
            riskyVenueAlertProvider.riskyVenueAlert = RiskyVenueAlert(
                id = it,
                messageType = INFORM
            )
            riskyVenueIdProvider.value = null
        }
    }

    internal var listeners = mutableListOf<() -> Unit>()

    fun registerListener(onUserInboxChanged: () -> Unit) {
        listeners.add(onUserInboxChanged)
    }

    fun unregisterListener(onUserInboxChanged: () -> Unit) {
        listeners.remove(onUserInboxChanged)
    }

    fun notifyChanges() {
        listeners.forEach { it() }
    }

    fun addUserInboxItem(item: AddableUserInboxItem) {
        when (item) {
            is ShowIsolationExpiration ->
                isolationExpirationDateProvider.value = item.expirationDate.toString()
            is ShowVenueAlert ->
                riskyVenueAlertProvider.riskyVenueAlert = item.toRiskyVenueAlert()
            is ShowEncounterDetection ->
                shouldShowEncounterDetectionActivityProvider.value = true
        }
        notifyChanges()
    }

    fun fetchInbox(): UserInboxItem? {
        if (isolationExpirationDateProvider.value != null) {
            return ShowIsolationExpiration(LocalDate.parse(isolationExpirationDateProvider.value))
        }
        if (unacknowledgedTestResultsProvider.testResults.isNotEmpty()) {
            return ShowTestResult
        }
        if (shouldShowEncounterDetectionActivityProvider.value == true) {
            return ShowEncounterDetection
        }
        return riskyVenueAlertProvider.riskyVenueAlert?.toShowVenueAlert()
    }

    fun clearItem(item: AddableUserInboxItem) {
        when (item) {
            is ShowIsolationExpiration -> isolationExpirationDateProvider.value = null
            is ShowVenueAlert -> riskyVenueAlertProvider.riskyVenueAlert = null
            is ShowEncounterDetection -> shouldShowEncounterDetectionActivityProvider.value = null
        }
        notifyChanges()
    }

    private fun ShowVenueAlert.toRiskyVenueAlert() =
        RiskyVenueAlert(
            id = venueId,
            messageType = messageType
        )

    private fun RiskyVenueAlert.toShowVenueAlert() =
        ShowVenueAlert(
            venueId = id,
            messageType = messageType
        )
}

class IsolationExpirationDateProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs = sharedPreferences.with<String>(ISOLATION_EXPIRATION_DATE)

    var value: String? by prefs

    companion object {
        private const val ISOLATION_EXPIRATION_DATE = "ISOLATION_EXPIRATION_DATE"
    }
}

@Deprecated("Not used anymore since 4.6. Use RiskyVenueProvider instead.")
class RiskyVenueIdProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs = sharedPreferences.with<String>(RISKY_VENUE_ID)

    var value: String? by prefs

    companion object {
        private const val RISKY_VENUE_ID = "RISKY_VENUE_ID"
    }
}

class ShouldShowEncounterDetectionActivityProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs =
        sharedPreferences.with<Boolean>(SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY)

    var value: Boolean? by prefs

    companion object {
        private const val SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY =
            "SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY"
    }
}

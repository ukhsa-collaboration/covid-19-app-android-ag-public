package uk.nhs.nhsx.covid19.android.app.notifications

import android.content.SharedPreferences
import dagger.Lazy
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlow
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.Initial
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.None
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.Reminder
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ContinueInitialKeySharing
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowKeySharingReminder
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.DoNotNotify
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.Notify
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed class UserInboxItem {
    object ShowTestResult : UserInboxItem()
    object ContinueInitialKeySharing : UserInboxItem()
    object ShowKeySharingReminder : UserInboxItem()
    data class ShowIsolationExpiration(val expirationDate: LocalDate) : UserInboxItem()
}

sealed class AddableUserInboxItem : UserInboxItem() {
    data class ShowVenueAlert(val venueId: String, val messageType: MessageType) : AddableUserInboxItem()
    object ShowEncounterDetection : AddableUserInboxItem()
}

@Singleton
class UserInbox @Inject constructor(
    @Suppress("DEPRECATION") private val riskyVenueIdProvider: RiskyVenueIdProvider,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val shouldEnterShareKeysFlow: ShouldEnterShareKeysFlow,
    private val shouldNotifyStateExpiration: Lazy<ShouldNotifyStateExpiration>
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
            is ShowVenueAlert ->
                riskyVenueAlertProvider.riskyVenueAlert = item.toRiskyVenueAlert()
            is ShowEncounterDetection ->
                shouldShowEncounterDetectionActivityProvider.value = true
        }
        notifyChanges()
    }

    fun fetchInbox(): UserInboxItem? {
        if (unacknowledgedTestResultsProvider.testResults.isNotEmpty()) {
            return ShowTestResult
        }
        val showIsolationExpiration = getShowIsolationExpirationItem()
        if (showIsolationExpiration != null) {
            return showIsolationExpiration
        }
        if (shouldShowEncounterDetectionActivityProvider.value == true) {
            return ShowEncounterDetection
        }
        val showVenueAlert = riskyVenueAlertProvider.riskyVenueAlert?.toShowVenueAlert()
        if (showVenueAlert != null) {
            return showVenueAlert
        }
        val shareKeysFlow = getShareKeysFlowInboxItem()
        if (shareKeysFlow != null) {
            return shareKeysFlow
        }

        return null
    }

    private fun getShowIsolationExpirationItem(): ShowIsolationExpiration? =
        when (val result = shouldNotifyStateExpiration.get().invoke()) {
            is Notify -> ShowIsolationExpiration(result.expiryDate)
            DoNotNotify -> null
        }

    private fun getShareKeysFlowInboxItem(): UserInboxItem? =
        when (shouldEnterShareKeysFlow()) {
            Initial -> ContinueInitialKeySharing
            Reminder -> ShowKeySharingReminder
            None -> null
        }

    fun clearItem(item: AddableUserInboxItem) {
        when (item) {
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

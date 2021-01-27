package uk.nhs.nhsx.covid19.android.app.notifications

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed class UserInboxItem {
    object ShowTestResult : UserInboxItem()
}

sealed class AddableUserInboxItem : UserInboxItem() {
    data class ShowIsolationExpiration(val expirationDate: LocalDate) : AddableUserInboxItem()
    data class ShowVenueAlert(val venueId: String) : AddableUserInboxItem()
    object ShowEncounterDetection : AddableUserInboxItem()
}

@Singleton
class UserInbox @Inject constructor(
    private val isolationExpirationDateProvider: IsolationExpirationDateProvider,
    private val riskyVenueIdProvider: RiskyVenueIdProvider,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider
) {

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
                riskyVenueIdProvider.value = item.venueId
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
        if (shouldShowEncounterDetectionActivityProvider.value != null &&
            shouldShowEncounterDetectionActivityProvider.value == true
        ) {
            return ShowEncounterDetection
        }
        val venueId = riskyVenueIdProvider.value
        if (venueId != null) {
            return ShowVenueAlert(venueId)
        }
        return null
    }

    fun clearItem(item: AddableUserInboxItem) {
        when (item) {
            is ShowIsolationExpiration -> isolationExpirationDateProvider.value = null
            is ShowVenueAlert -> riskyVenueIdProvider.value = null
            is ShowEncounterDetection -> shouldShowEncounterDetectionActivityProvider.value = null
        }
        notifyChanges()
    }
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

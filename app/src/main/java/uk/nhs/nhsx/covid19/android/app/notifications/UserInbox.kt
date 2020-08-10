package uk.nhs.nhsx.covid19.android.app.notifications

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed class UserInboxItem {
    data class ShowIsolationExpiration(val expirationDate: LocalDate) : UserInboxItem()
    object ShowTestResult : UserInboxItem()
    data class ShowVenueAlert(val venueId: String) : UserInboxItem()
    object ShowEncounterDetection : UserInboxItem()
}

interface UserInbox {
    fun addUserInboxItem(item: UserInboxItem)
    fun fetchInbox(): UserInboxItem?
    fun clearItem(item: UserInboxItem)
    fun registerListener(onUserInboxChanged: () -> Unit)
    fun unregisterListener(onUserInboxChanged: () -> Unit)
}

@Singleton
class AndroidUserInbox @Inject constructor(val sharedPreferences: SharedPreferences) : UserInbox {

    private val isolationExpirationDatePrefs = sharedPreferences.with<String>(ISOLATION_EXPIRATION_DATE)
    private var isolationExpirationDate: String? by isolationExpirationDatePrefs

    private val testResultPrefs = sharedPreferences.with<Boolean>(SHOULD_SHOW_TEST_RESULT)
    private var testResult: Boolean? by testResultPrefs

    private val riskyVenueIdPrefs = sharedPreferences.with<String>(RISKY_VENUE_ID)
    private var riskyVenueId: String? by riskyVenueIdPrefs

    private val encounterDetectionPrefs = sharedPreferences.with<Boolean>(SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY)
    private var encounterDetection: Boolean? by encounterDetectionPrefs

    private var listeners = mutableListOf<() -> Unit>()

    override fun registerListener(onUserInboxChanged: () -> Unit) {
        listeners.add(onUserInboxChanged)
    }

    override fun unregisterListener(onUserInboxChanged: () -> Unit) {
        listeners.remove(onUserInboxChanged)
    }

    override fun addUserInboxItem(item: UserInboxItem) {
        when (item) {
            is ShowIsolationExpiration -> isolationExpirationDate = item.expirationDate.toString()
            is ShowTestResult -> testResult = true
            is ShowVenueAlert -> riskyVenueId = item.venueId
            is ShowEncounterDetection -> encounterDetection = true
        }
        listeners.forEach { it() }
    }

    override fun fetchInbox(): UserInboxItem? {
        if (isolationExpirationDate != null) {
            return ShowIsolationExpiration(LocalDate.parse(isolationExpirationDate))
        }
        if (testResult != null && testResult == true) {
            return ShowTestResult
        }
        if (encounterDetection != null && encounterDetection == true) {
            return ShowEncounterDetection
        }
        val venueId = riskyVenueId
        if (venueId != null) {
            return ShowVenueAlert(venueId)
        }
        return null
    }

    override fun clearItem(item: UserInboxItem) {
        when (item) {
            is ShowIsolationExpiration -> isolationExpirationDate = null
            is ShowTestResult -> testResult = null
            is ShowVenueAlert -> riskyVenueId = null
            is ShowEncounterDetection -> encounterDetection = null
        }
        listeners.forEach { it() }
    }

    companion object {
        const val SHOULD_SHOW_TEST_RESULT = "SHOULD_SHOW_TEST_RESULT"
        const val ISOLATION_EXPIRATION_DATE = "ISOLATION_EXPIRATION_DATE"
        const val SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY = "SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY"
        const val RISKY_VENUE_ID = "RISKY_VENUE_ID"
    }
}

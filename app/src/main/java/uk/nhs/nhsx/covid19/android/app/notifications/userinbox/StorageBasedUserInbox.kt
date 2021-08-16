package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider.Companion.RISKY_VENUE
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageBasedUserInbox @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : OnSharedPreferenceChangeListener {

    private var storageChangeListener: UserInboxStorageChangeListener? = null

    fun setStorageChangeListener(listener: UserInboxStorageChangeListener) {
        storageChangeListener = listener
        startListeningToChanges()
    }

    fun removeStorageChangeListener() {
        stopListeningToChanges()
        storageChangeListener = null
    }

    private fun startListeningToChanges() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun stopListeningToChanges() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun notifyChanges() {
        storageChangeListener?.notifyChanged()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == RISKY_VENUE ||
            key == ShouldShowEncounterDetectionActivityProvider.SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY ||
            key == ReceivedUnknownTestResultProvider.RECEIVED_UNKNOWN_TEST_RESULT
        ) {
            notifyChanges()
        }
    }
}

interface UserInboxStorageChangeListener {
    fun notifyChanged()
}

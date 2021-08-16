package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import android.content.SharedPreferences
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider.Companion.RISKY_VENUE
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider.Companion.SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY

class StorageBasedUserInboxTest {

    private val sharedPreferences = mockk<SharedPreferences>(relaxUnitFun = true)

    private fun createUserInboxStorageListener() = StorageBasedUserInbox(sharedPreferences)

    @Test
    fun `setting UserInboxStorageChangeListener starts listening to storage changes`() {
        val storageChangeListener = mockk<UserInboxStorageChangeListener>()

        val testSubject = createUserInboxStorageListener()

        testSubject.setStorageChangeListener(storageChangeListener)

        verify { sharedPreferences.registerOnSharedPreferenceChangeListener(testSubject) }
    }

    @Test
    fun `removing UserInboxStorageChangeListener stops listening to storage changes`() {
        val storageChangeListener = mockk<UserInboxStorageChangeListener>()

        val testSubject = createUserInboxStorageListener()
        testSubject.setStorageChangeListener(storageChangeListener)

        testSubject.removeStorageChangeListener()

        verify { sharedPreferences.unregisterOnSharedPreferenceChangeListener(testSubject) }
    }

    @Test
    fun `notifyChanges invokes UserInboxStorageChangeListener`() {
        val storageChangeListener = mockk<UserInboxStorageChangeListener>(relaxed = true)

        val testSubject = createUserInboxStorageListener()
        testSubject.setStorageChangeListener(storageChangeListener)

        testSubject.notifyChanges()

        verify { storageChangeListener.notifyChanged() }
    }

    @Test
    fun `trigger callback if shared preference with key RISKY_VENUE changed`() {
        val storageChangeListener = mockk<UserInboxStorageChangeListener>(relaxed = true)

        val testSubject = createUserInboxStorageListener()
        testSubject.setStorageChangeListener(storageChangeListener)

        testSubject.onSharedPreferenceChanged(sharedPreferences, RISKY_VENUE)

        verify { storageChangeListener.notifyChanged() }
    }

    @Test
    fun `trigger callback if shared preference with key SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY changed`() {
        val storageChangeListener = mockk<UserInboxStorageChangeListener>(relaxed = true)

        val testSubject = createUserInboxStorageListener()
        testSubject.setStorageChangeListener(storageChangeListener)

        testSubject.onSharedPreferenceChanged(sharedPreferences, SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY)

        verify { storageChangeListener.notifyChanged() }
    }

    @Test
    fun `do not trigger callback if shared preference key is neither SHOULD_SHOW_ENCOUNTER_DETECTION_ACTIVITY nor RISKY_VENUE_ID`() {
        val storageChangeListener = mockk<UserInboxStorageChangeListener>(relaxed = true)

        val testSubject = createUserInboxStorageListener()
        testSubject.setStorageChangeListener(storageChangeListener)

        testSubject.onSharedPreferenceChanged(sharedPreferences, "SOME_OTHER_KEY")

        verify(exactly = 0) { storageChangeListener.notifyChanged() }
    }
}

package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

@Deprecated("Use ContactTracingActivationReminderProvider. This is only for migration.")
class ResumeContactTracingNotificationTimeProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Long>(VALUE_KEY)

    var value: Long? by prefs

    companion object {
        private const val VALUE_KEY = "RESUME_CONTACT_TRACING_NOTIFICATION_TIME"
    }
}

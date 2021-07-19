package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

class ContactTracingActivationReminderProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {
    var reminder: ContactTracingActivationReminder? by storage(VALUE_KEY)

    companion object {
        const val VALUE_KEY = "CONTACT_TRACING_ACTIVATION_REMINDER_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class ContactTracingActivationReminder(
    val alarmTime: Long,
    val additionalReminderCount: Int = 0
) {
    fun hasAlreadyScheduledAdditionalReminder() = additionalReminderCount > 0
}

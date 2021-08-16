package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class KeySharingInfoProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    var keySharingInfo: KeySharingInfo? by storage(VALUE_KEY)

    fun reset() = synchronized(lock) {
        keySharingInfo = null
    }

    fun setNotificationSentDate(notificationSentDate: Instant) =
        synchronized(lock) {
            keySharingInfo = keySharingInfo?.copy(notificationSentDate = notificationSentDate)
        }

    fun setHasDeclinedSharingKeys() =
        synchronized(lock) {
            keySharingInfo = keySharingInfo?.copy(hasDeclinedSharingKeys = true)
        }

    companion object {
        const val VALUE_KEY = "KEY_SHARING_INFO"
    }
}

@JsonClass(generateAdapter = true)
data class KeySharingInfo(
    val diagnosisKeySubmissionToken: String,
    val acknowledgedDate: Instant,
    val notificationSentDate: Instant? = null,
    val hasDeclinedSharingKeys: Boolean = false
) {
    fun wasAcknowledgedMoreThan24HoursAgo(clock: Clock): Boolean {
        val exactly24HoursAgo = Instant.now(clock).minus(24, ChronoUnit.HOURS)
        return acknowledgedDate.isBefore(exactly24HoursAgo)
    }
}

package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class KeySharingInfoProvider @Inject constructor(
    private val keySharingInfoStorage: KeySharingInfoJsonStorage,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(KeySharingInfo::class.java)

    private val lock = Object()

    var keySharingInfo: KeySharingInfo?
        get() {
            return synchronized(lock) {
                keySharingInfoStorage.value?.let {
                    runCatching {
                        adapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            null
                        } // TODO add crash analytics and come up with a more sophisticated solution
                }
            }
        }
        set(value) {
            return synchronized(lock) {
                keySharingInfoStorage.value = adapter.toJson(value)
            }
        }

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
}

class KeySharingInfoJsonStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

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

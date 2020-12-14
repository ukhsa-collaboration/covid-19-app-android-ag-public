package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import uk.nhs.nhsx.covid19.android.app.util.lastDateFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class LastDownloadedKeyTimeProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private var timestamp by sharedPreferences.with<Long>(LAST_DOWNLOADED_KEY)

    fun getLatestStoredTime(): LocalDateTime? = timestamp.toLocalDateTime()

    fun saveLastStoredTime(dateTime: String) {
        timestamp = LocalDateTime.parse(dateTime, lastDateFormatter).toMillis()
    }

    private fun Long?.toLocalDateTime(): LocalDateTime? =
        this?.let {
            val instant: Instant = Instant.ofEpochMilli(this)
            instant.atZone(ZoneOffset.UTC).toLocalDateTime()
        }

    private fun LocalDateTime.toMillis(): Long =
        this.toInstant(ZoneOffset.UTC).toEpochMilli()

    companion object {
        const val LAST_DOWNLOADED_KEY = "LAST_DOWNLOADED_KEY"
    }
}

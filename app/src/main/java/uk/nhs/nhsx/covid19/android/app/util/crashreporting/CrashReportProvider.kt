package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class CrashReportProvider @Inject constructor(
    private val crashReportStorage: SynchronousCrashReportStorage,
    private val moshi: Moshi
) {
    private val lock = Object()

    var crashReport: CrashReport?
        get() = synchronized(lock) {
            crashReportStorage.value?.let {
                runCatching {
                    moshi.adapter(CrashReport::class.java).fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        null
                    }
            }
        }
        set(value) = synchronized(lock) {
            crashReportStorage.value =
                moshi.adapter(CrashReport::class.java).toJson(value)
        }

    fun clear() = synchronized(lock) {
        crashReportStorage.value = null
    }
}

class SynchronousCrashReportStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY, commit = true)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "CRASH_REPORT_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class CrashReport(val exception: String, val threadName: String, val stackTrace: String)

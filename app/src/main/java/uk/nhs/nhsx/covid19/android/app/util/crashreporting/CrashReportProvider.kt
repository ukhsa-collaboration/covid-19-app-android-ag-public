package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

class CrashReportProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var crashReport: CrashReport? by storage(VALUE_KEY)

    fun clear() {
        crashReport = null
    }

    companion object {
        const val VALUE_KEY = "CRASH_REPORT_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class CrashReport(val exception: String, val threadName: String, val stackTrace: String)

package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class AnalyticsEventsStorage @Inject constructor(
    sharedPreferences: SharedPreferences,
    moshi: Moshi
) {

    private val type = Types.newParameterizedType(
        List::class.java,
        AnalyticsPayload::class.java
    )
    private val adapter = moshi.adapter<List<AnalyticsPayload>>(type)

    private val analyticsEventsPref = sharedPreferences.with<String>(ANALYTICS_EVENT_KEY)

    private var analyticsEventsJson by analyticsEventsPref

    var value: List<AnalyticsPayload>?
        get() = analyticsEventsJson?.let { adapter.fromJson(it) }
        set(value) {
            analyticsEventsJson = adapter.toJson(value)
        }

    companion object {
        private const val ANALYTICS_EVENT_KEY = "ANALYTICS_EVENT_KEY"
    }
}

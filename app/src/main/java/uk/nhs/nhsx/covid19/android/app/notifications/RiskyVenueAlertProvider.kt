package uk.nhs.nhsx.covid19.android.app.notifications

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class RiskyVenueAlertProvider @Inject constructor(
    private val riskyVenueAlertStorage: RiskyVenueAlertStorage,
    moshi: Moshi
) {

    private val riskyVenueAlertStorageSerializationAdapter =
        moshi.adapter(RiskyVenueAlert::class.java)

    var riskyVenueAlert: RiskyVenueAlert?
        get() =
            riskyVenueAlertStorage.value?.let {
                runCatching {
                    riskyVenueAlertStorageSerializationAdapter.fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        null
                    } // TODO add crash analytics and come up with a more sophisticated solution
            }
        set(value) {
            if (value == null) {
                riskyVenueAlertStorage.value = null
            } else {
                riskyVenueAlertStorage.value = riskyVenueAlertStorageSerializationAdapter.toJson(value)
            }
        }
}

class RiskyVenueAlertStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs = sharedPreferences.with<String>(RISKY_VENUE_ID)

    var value: String? by prefs

    companion object {
        const val RISKY_VENUE_ID = "RISKY_VENUE"
    }
}

@JsonClass(generateAdapter = true)
data class RiskyVenueAlert(
    val id: String,
    val messageType: RiskyVenueMessageType
)

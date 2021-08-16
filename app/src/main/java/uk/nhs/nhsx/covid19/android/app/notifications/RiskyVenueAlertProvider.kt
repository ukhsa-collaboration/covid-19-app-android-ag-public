package uk.nhs.nhsx.covid19.android.app.notifications

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

class RiskyVenueAlertProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var riskyVenueAlert: RiskyVenueAlert? by storage(RISKY_VENUE)

    companion object {
        const val RISKY_VENUE = "RISKY_VENUE"
    }
}

@JsonClass(generateAdapter = true)
data class RiskyVenueAlert(
    val id: String,
    val messageType: RiskyVenueMessageType
)

package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import javax.inject.Inject

class MigrateRiskyVenueIdProvider @Inject constructor(
    @Suppress("DEPRECATION") private val riskyVenueIdProvider: RiskyVenueIdProvider,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
) {
    operator fun invoke() {
        riskyVenueIdProvider.value?.let {
            riskyVenueAlertProvider.riskyVenueAlert = RiskyVenueAlert(id = it, messageType = INFORM)
            riskyVenueIdProvider.value = null
        }
    }
}

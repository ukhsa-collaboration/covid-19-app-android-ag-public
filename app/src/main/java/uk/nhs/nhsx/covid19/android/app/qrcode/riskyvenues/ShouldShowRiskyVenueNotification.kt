package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import javax.inject.Inject

class ShouldShowRiskyVenueNotification @Inject constructor(
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
) {
    operator fun invoke(messageTypeOfNewRiskyVenueVisit: RiskyVenueMessageType): Boolean {
        val unacknowledgedRiskyVenueAlert = riskyVenueAlertProvider.riskyVenueAlert ?: return true

        return unacknowledgedRiskyVenueAlert.messageType == INFORM && messageTypeOfNewRiskyVenueVisit == BOOK_TEST
    }
}

package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import javax.inject.Inject

class ShouldShowRiskyVenueNotification @Inject constructor(
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
) {
    operator fun invoke(messageTypeOfNewRiskyVenueVisit: MessageType): Boolean {
        val unacknowledgedRiskyVenueAlert = riskyVenueAlertProvider.riskyVenueAlert ?: return true

        return unacknowledgedRiskyVenueAlert.messageType == INFORM && messageTypeOfNewRiskyVenueVisit == BOOK_TEST
    }
}

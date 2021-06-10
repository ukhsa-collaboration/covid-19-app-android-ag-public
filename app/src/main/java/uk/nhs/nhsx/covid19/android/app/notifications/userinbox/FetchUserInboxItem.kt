package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlow
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.Initial
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.None
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.Reminder
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ContinueInitialKeySharing
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowKeySharingReminder
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowUnknownTestResult
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.DoNotNotify
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.Notify
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.testordering.unknownresult.ReceivedUnknownTestResultProvider
import javax.inject.Inject

class FetchUserInboxItem @Inject constructor(
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val shouldNotifyStateExpiration: ShouldNotifyStateExpiration,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val shouldEnterShareKeysFlow: ShouldEnterShareKeysFlow,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
    private val receivedUnknownTestResultProvider: ReceivedUnknownTestResultProvider
) {

    operator fun invoke(): UserInboxItem? {
        if (unacknowledgedTestResultsProvider.testResults.isNotEmpty()) {
            return ShowTestResult
        }
        if (receivedUnknownTestResultProvider.value == true) {
            return ShowUnknownTestResult
        }
        val showIsolationExpiration = getShowIsolationExpirationItem()
        if (showIsolationExpiration != null) {
            return showIsolationExpiration
        }
        if (shouldShowEncounterDetectionActivityProvider.value == true) {
            return ShowEncounterDetection
        }
        val showVenueAlert = riskyVenueAlertProvider.riskyVenueAlert?.toShowVenueAlert()
        if (showVenueAlert != null) {
            return showVenueAlert
        }
        val shareKeysFlow = getShareKeysFlowInboxItem()
        if (shareKeysFlow != null) {
            return shareKeysFlow
        }
        return null
    }

    private fun getShowIsolationExpirationItem(): ShowIsolationExpiration? =
        when (val result = shouldNotifyStateExpiration()) {
            is Notify -> ShowIsolationExpiration(result.expiryDate)
            DoNotNotify -> null
        }

    private fun getShareKeysFlowInboxItem(): UserInboxItem? =
        when (shouldEnterShareKeysFlow()) {
            Initial -> ContinueInitialKeySharing
            Reminder -> ShowKeySharingReminder
            None -> null
        }

    private fun RiskyVenueAlert.toShowVenueAlert() =
        ShowVenueAlert(
            venueId = id,
            messageType = messageType
        )
}

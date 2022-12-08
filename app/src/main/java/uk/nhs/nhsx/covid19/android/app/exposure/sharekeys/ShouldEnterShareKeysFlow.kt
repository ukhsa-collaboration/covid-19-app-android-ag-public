package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.KeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.Initial
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.None
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.Reminder
import java.time.Clock
import javax.inject.Inject

class ShouldEnterShareKeysFlow @Inject constructor(
    private val canShareKeys: CanShareKeys,
    private val clock: Clock
) {
    operator fun invoke(): ShouldEnterShareKeysFlowResult {
        return when (val canShareKeysResult = canShareKeys()) {
            NoKeySharingPossible -> None
            is KeySharingPossible -> evaluateKeySharingEntryPoint(canShareKeysResult.keySharingInfo)
        }
    }

    private fun evaluateKeySharingEntryPoint(keySharingInfo: KeySharingInfo): ShouldEnterShareKeysFlowResult {
        return if (keySharingInfo.hasDeclinedSharingKeys) {
            if (keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(clock)) {
                Reminder
            } else {
                None
            }
        } else if (!keySharingInfo.isSelfReporting) {
            Initial
        } else {
            None
        }
    }
}

sealed class ShouldEnterShareKeysFlowResult {
    object Initial : ShouldEnterShareKeysFlowResult()
    object Reminder : ShouldEnterShareKeysFlowResult()
    object None : ShouldEnterShareKeysFlowResult()
}

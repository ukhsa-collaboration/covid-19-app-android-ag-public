package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import dagger.Lazy
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.KeySharingPossible
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import javax.inject.Inject

class CanShareKeys @Inject constructor(
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val isolationStateMachine: Lazy<IsolationStateMachine>,
    private val calculateKeySubmissionDateRange: CalculateKeySubmissionDateRange,
) {
    operator fun invoke(): CanShareKeysResult {
        val keySharingInfo = keySharingInfoProvider.keySharingInfo ?: return NoKeySharingPossible
        val symptomsOnsetDate = isolationStateMachine.get().readState().symptomsOnsetDate ?: return NoKeySharingPossible
        val dateRange = calculateKeySubmissionDateRange(keySharingInfo.acknowledgedDate, symptomsOnsetDate)
        return if (dateRange.containsAtLeastOneDay()) {
            KeySharingPossible(keySharingInfo)
        } else NoKeySharingPossible
    }

    sealed class CanShareKeysResult {
        data class KeySharingPossible(val keySharingInfo: KeySharingInfo) : CanShareKeysResult()
        object NoKeySharingPossible : CanShareKeysResult()
    }
}

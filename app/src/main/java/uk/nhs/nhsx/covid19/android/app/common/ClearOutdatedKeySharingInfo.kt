package uk.nhs.nhsx.covid19.android.app.common

import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import javax.inject.Inject

class ClearOutdatedKeySharingInfo @Inject constructor(
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val canShareKeys: CanShareKeys,
) {
    operator fun invoke() {
        if (canShareKeys() == NoKeySharingPossible) {
            keySharingInfoProvider.reset()
        }
    }
}

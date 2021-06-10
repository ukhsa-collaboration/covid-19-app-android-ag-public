package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey

interface ShareKeysNavigationTarget

sealed class ShareKeysNavigateTo : ShareKeysNavigationTarget {
    data class SubmitKeysProgressActivity(
        val temporaryExposureKeys: List<NHSTemporaryExposureKey>,
        val diagnosisKeySubmissionToken: String
    ) : ShareKeysNavigateTo()

    data class ShareKeysResultActivity(val bookFollowUpTest: Boolean) : ShareKeysNavigateTo()
    object StatusActivity : ShareKeysNavigateTo()
    object Finish : ShareKeysNavigateTo()
}

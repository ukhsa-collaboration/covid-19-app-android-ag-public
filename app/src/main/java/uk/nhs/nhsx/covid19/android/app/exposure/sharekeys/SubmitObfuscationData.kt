package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import javax.inject.Inject

class SubmitObfuscationData @Inject constructor(
    private val submitEmptyData: SubmitEmptyData,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
) {

    operator fun invoke() {
        submitEmptyData()
        submitFakeExposureWindows()
    }
}

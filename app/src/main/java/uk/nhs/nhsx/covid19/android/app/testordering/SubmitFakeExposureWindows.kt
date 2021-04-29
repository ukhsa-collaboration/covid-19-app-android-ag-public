package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import javax.inject.Inject

class SubmitFakeExposureWindows @Inject constructor(
    private val submitEmptyData: SubmitEmptyData,
    private val getEmptyExposureWindowSubmissionCount: GetEmptyExposureWindowSubmissionCount
) {
    operator fun invoke(numberOfExposureWindowsSent: Int = 0) {
        submitEmptyData(getEmptyExposureWindowSubmissionCount(numberOfExposureWindowsSent))
    }
}

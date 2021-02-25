package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource
import javax.inject.Inject

class SubmitFakeExposureWindows @Inject constructor(
    private val submitEmptyData: SubmitEmptyData,
    private val getEmptyExposureWindowSubmissionCount: GetEmptyExposureWindowSubmissionCount
) {
    operator fun invoke(emptySubmissionSource: EmptySubmissionSource, numberOfExposureWindowsSent: Int = 0) {
        submitEmptyData(emptySubmissionSource, getEmptyExposureWindowSubmissionCount(numberOfExposureWindowsSent))
    }
}

package uk.nhs.nhsx.covid19.android.app.testordering

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource
import javax.inject.Inject

class SubmitFakeExposureWindows @Inject constructor(
    private val submitEmptyData: SubmitEmptyData,
    private val getEmptyExposureWindowSubmissionCount: GetEmptyExposureWindowSubmissionCount
) {

    operator fun invoke(emptySubmissionSource: EmptySubmissionSource, numberOfExposureWindowsSent: Int = 0) {
        val emptyExposureWindowSubmissionCount = getEmptyExposureWindowSubmissionCount(numberOfExposureWindowsSent)
        for (callIndex in 1..emptyExposureWindowSubmissionCount) {
            Timber.d("Fake exposure window submission [$callIndex]")
            submitEmptyData(emptySubmissionSource)
        }
    }
}

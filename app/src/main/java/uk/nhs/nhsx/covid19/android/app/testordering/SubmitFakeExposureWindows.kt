package uk.nhs.nhsx.covid19.android.app.testordering

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource
import java.security.SecureRandom
import javax.inject.Inject

class SubmitFakeExposureWindows(
    private val emptyApi: EmptyApi,
    private val random: SecureRandom,
    private val emptyEpidemiologyEventSubmissionScope: CoroutineScope,
    private val emptyEpidemiologyEventSubmissionDispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(
        emptyApi: EmptyApi
    ) : this(
        emptyApi,
        random = SecureRandom(),
        emptyEpidemiologyEventSubmissionScope = GlobalScope,
        emptyEpidemiologyEventSubmissionDispatcher = Dispatchers.IO
    )

    operator fun invoke(emptySubmissionSource: EmptySubmissionSource, exposureWindowCount: Int) {
        emptyEpidemiologyEventSubmissionScope.launch(emptyEpidemiologyEventSubmissionDispatcher) {
            runSafely {
                val totalCallCount = random.nextInt(MAX_CALLS - MIN_CALLS) + MIN_CALLS
                val fakeCallCount = totalCallCount - exposureWindowCount
                for (callIndex in 1..fakeCallCount) {
                    Timber.d("Fake temporary epidemiology event submission [$callIndex]")
                    emptyApi.submit(EmptySubmissionRequest(emptySubmissionSource))
                }
            }
        }
    }

    companion object {
        private const val MIN_CALLS = 2
        private const val MAX_CALLS = 16 // exclusive
    }
}

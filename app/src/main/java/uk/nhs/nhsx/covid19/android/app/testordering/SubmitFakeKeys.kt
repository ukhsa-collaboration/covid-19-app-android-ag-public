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
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.KEY_SUBMISSION
import javax.inject.Inject

class SubmitFakeKeys(
    private val emptyApi: EmptyApi,
    private val emptyKeysSubmissionScope: CoroutineScope,
    private val emptyKeysSubmissionDispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(emptyApi: EmptyApi) : this(
        emptyApi,
        emptyKeysSubmissionScope = GlobalScope,
        emptyKeysSubmissionDispatcher = Dispatchers.IO
    )

    operator fun invoke() {
        emptyKeysSubmissionScope.launch(emptyKeysSubmissionDispatcher) {
            runSafely {
                Timber.d("Fake temporary exposure key submission")
                emptyApi.submit(EmptySubmissionRequest(KEY_SUBMISSION))
            }
        }
    }
}

package uk.nhs.nhsx.covid19.android.app.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource
import javax.inject.Inject

class SubmitEmptyData(
    private val emptyApi: EmptyApi,
    private val emptySubmissionScope: CoroutineScope,
    private val emptySubmissionDispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(emptyApi: EmptyApi) : this(
        emptyApi,
        emptySubmissionScope = GlobalScope,
        emptySubmissionDispatcher = Dispatchers.IO
    )

    operator fun invoke(emptySubmissionSource: EmptySubmissionSource) {
        emptySubmissionScope.launch(emptySubmissionDispatcher) {
            runSafely {
                emptyApi.submit(EmptySubmissionRequest(emptySubmissionSource))
            }
        }
    }
}

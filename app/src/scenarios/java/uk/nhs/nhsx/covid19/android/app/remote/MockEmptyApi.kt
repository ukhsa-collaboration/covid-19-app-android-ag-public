package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest

class MockEmptyApi : EmptyApi {
    override suspend fun submit(emptySubmissionRequest: EmptySubmissionRequest) { }
}

package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload

class MockKeysSubmissionApi : KeysSubmissionApi {

    override suspend fun submitGeneratedKeys(temporaryExposureKeysPayload: TemporaryExposureKeysPayload) {
    }
}

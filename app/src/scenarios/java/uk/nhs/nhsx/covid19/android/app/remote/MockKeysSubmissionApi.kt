package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload
import java.lang.Exception

class MockKeysSubmissionApi : KeysSubmissionApi {

    var shouldSucceed = true

    override suspend fun submitGeneratedKeys(temporaryExposureKeysPayload: TemporaryExposureKeysPayload) {
        if (!shouldSucceed) {
            throw Exception("Mock error")
        }
    }
}

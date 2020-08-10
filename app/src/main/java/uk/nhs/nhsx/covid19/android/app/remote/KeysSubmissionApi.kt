package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload

interface KeysSubmissionApi {
    @POST("submission/diagnosis-keys")
    suspend fun submitGeneratedKeys(@Body temporaryExposureKeysPayload: TemporaryExposureKeysPayload)
}

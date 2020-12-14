package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest

interface EmptyApi {
    @POST("submission/empty-submission")
    suspend fun submit(@Body emptySubmissionRequest: EmptySubmissionRequest)
}

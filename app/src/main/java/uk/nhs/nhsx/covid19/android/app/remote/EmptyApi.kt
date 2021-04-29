package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET

interface EmptyApi {
    @GET("submission/empty-submission-v2")
    suspend fun submit()
}

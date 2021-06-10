package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse

interface LocalMessagesApi {
    @GET("distribution/local-messages")
    suspend fun fetchLocalMessages(): LocalMessagesResponse
}

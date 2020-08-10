package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.ActivationRequest

interface ActivationApi {
    @POST("activation/request")
    suspend fun activate(@Body activationRequest: ActivationRequest): Response<Unit>
}

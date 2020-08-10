package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse

interface IsolationConfigurationApi {

    @GET("distribution/self-isolation")
    suspend fun getIsolationConfiguration(): IsolationConfigurationResponse
}

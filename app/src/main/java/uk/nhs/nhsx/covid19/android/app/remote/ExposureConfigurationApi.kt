package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.GET
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse

interface ExposureConfigurationApi {
    @GET("distribution/exposure-configuration")
    suspend fun getExposureConfiguration(): ExposureConfigurationResponse
}

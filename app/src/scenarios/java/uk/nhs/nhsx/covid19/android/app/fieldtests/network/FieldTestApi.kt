package uk.nhs.nhsx.covid19.android.app.fieldtests.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FieldTestApi {
    @POST("team/{teamId}/experiment")
    suspend fun createExperiment(
        @Path("teamId") teamId: String,
        @Body experimentInfo: ExperimentInfo
    ): ExperimentInfo

    @GET("team/{teamId}/experiment/latest")
    suspend fun getLatestExperiment(@Path("teamId") teamId: String): ExperimentInfo

    @POST("team/{teamId}/experiment/{experimentId}/device")
    suspend fun joinExperiment(
        @Path("teamId") teamId: String,
        @Path("experimentId") experimentId: String,
        @Body deviceInfo: DeviceInfo
    ): Response<ExperimentInfo>

    @GET("team/{teamId}/experiment/{experimentId}")
    suspend fun getExperimentInfo(
        @Path("teamId") teamId: String,
        @Path("experimentId") experimentId: String
    ): ExperimentInfo

    @POST("team/{teamId}/experiment/{experimentId}/result/{deviceName}")
    suspend fun sendResult(
        @Path("teamId") teamId: String,
        @Path("experimentId") experimentId: String,
        @Path("deviceName") deviceName: String,
        @Body matchingResult: MatchingResult
    )

    @GET("team/{teamId}/experiment/{experimentId}/results")
    suspend fun getResults(
        @Path("teamId") teamId: String,
        @Path("experimentId") experimentId: String
    )
}

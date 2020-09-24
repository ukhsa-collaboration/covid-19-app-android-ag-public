package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class MockKeysDistributionApi : KeysDistributionApi {
    override suspend fun fetchDailyKeys(timestamp: String): ResponseBody {
        return Response.error<Unit>(
            400, "{}".toResponseBody("application/json".toMediaType())
        ).errorBody()!!
    }

    override suspend fun fetchHourlyKeys(timestamp: String): ResponseBody {
        return Response.error<Unit>(
            400, "{}".toResponseBody("application/json".toMediaType())
        ).errorBody()!!
    }
}

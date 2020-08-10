package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import uk.nhs.nhsx.covid19.android.app.remote.data.ActivationRequest

class MockActivationApi : ActivationApi {
    override suspend fun activate(activationRequest: ActivationRequest): Response<Unit> {
        if (activationRequest.activationCode == MOCKED_ACTIVATION_CODE) {
            return Response.success(null)
        }
        return Response.error(
            400, "{}".toResponseBody("application/jso".toMediaType())
        )
    }

    companion object {
        const val MOCKED_ACTIVATION_CODE = "11111111"
    }
}

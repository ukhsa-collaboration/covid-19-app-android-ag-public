package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerResponse

class MockExposureCircuitBreakerApi : ExposureCircuitBreakerApi {

    override suspend fun submitExposureInfo(exposureCircuitBreakerRequest: ExposureCircuitBreakerRequest) =
        ExposureCircuitBreakerResponse(
            approvalToken = "sample approval token",
            approval = "yes"
        )

    override suspend fun getExposureCircuitBreakerResolution(approvalToken: String): ExposureCircuitBreakerPollingResponse {
        return ExposureCircuitBreakerPollingResponse("yes")
    }
}

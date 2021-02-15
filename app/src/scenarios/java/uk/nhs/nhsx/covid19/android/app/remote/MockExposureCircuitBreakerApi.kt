package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerResponse

class MockExposureCircuitBreakerApi : ExposureCircuitBreakerApi {

    override suspend fun submitExposureInfo(exposureCircuitBreakerRequest: ExposureCircuitBreakerRequest) =
        MockApiModule.behaviour.invoke {
            ExposureCircuitBreakerResponse(
                approvalToken = "sample approval token",
                approval = YES
            )
        }

    override suspend fun getExposureCircuitBreakerResolution(approvalToken: String): ExposureCircuitBreakerPollingResponse =
        MockApiModule.behaviour.invoke {
            ExposureCircuitBreakerPollingResponse(YES)
        }
}

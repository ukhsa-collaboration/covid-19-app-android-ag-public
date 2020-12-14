package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerResponse

class MockRiskyVenueCircuitBreakerApi : RiskyVenuesCircuitBreakerApi {

    override suspend fun getApproval(emptyBodyObject: Any): RiskyVenuesCircuitBreakerResponse =
        RiskyVenuesCircuitBreakerResponse(
            approvalToken = "sample approval token",
            approval = YES
        )

    override suspend fun getRiskyVenuesBreakerResolution(
        approvalToken: String
    ): RiskyVenuesCircuitBreakerPollingResponse =
        RiskyVenuesCircuitBreakerPollingResponse(YES)
}

package uk.nhs.nhsx.covid19.android.app.remote

import kotlinx.coroutines.delay
import uk.nhs.nhsx.covid19.android.app.remote.data.PostDistrictsResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM

class MockRiskyPostDistrictsApi : RiskyPostDistrictsApi {
    override suspend fun fetchRiskyPostDistricts(): PostDistrictsResponse {
        delay(2_000)
        return PostDistrictsResponse(mapOf("A1" to HIGH, "CM1" to HIGH, "A2" to LOW, "CM2" to MEDIUM))
    }
}

package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload

interface AnalyticsApi {

    @POST(PATH)
    suspend fun submitAnalytics(@Body analyticsPayload: AnalyticsPayload)

    companion object {
        const val PATH = "submission/mobile-analytics"
    }
}

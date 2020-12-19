package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest

interface EpidemiologyDataApi {

    @POST(PATH)
    suspend fun submitEpidemiologyData(@Body epidemiologyRequest: EpidemiologyRequest)

    companion object {
        const val PATH = "submission/mobile-analytics-events"
    }
}

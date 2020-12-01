package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyData

interface EpidemiologyDataApi {

    @POST(PATH)
    suspend fun submitEpidemiologyData(@Body epidemiologyData: EpidemiologyData)

    companion object {
        const val PATH = "submission/mobile-analytics-events"
    }
}

package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest

class MockEpidemiologyDataApi : EpidemiologyDataApi {

    val requests = mutableListOf<EpidemiologyRequest>()

    override suspend fun submitEpidemiologyData(epidemiologyRequest: EpidemiologyRequest) =
        MockApiModule.behaviour.invoke {
            requests += epidemiologyRequest
        }

    fun clear() {
        requests.clear()
    }
}

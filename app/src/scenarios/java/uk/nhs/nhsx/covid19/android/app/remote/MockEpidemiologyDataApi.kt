package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest

class MockEpidemiologyDataApi : EpidemiologyDataApi {
    override suspend fun submitEpidemiologyData(epidemiologyRequest: EpidemiologyRequest) =
        MockApiModule.behaviour.invoke {}
}

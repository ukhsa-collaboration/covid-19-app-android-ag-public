package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyData

class MockEpidemiologyDataApi : EpidemiologyDataApi {
    override suspend fun submitEpidemiologyData(epidemiologyData: EpidemiologyData) {}
}

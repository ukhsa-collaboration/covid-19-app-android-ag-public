package uk.nhs.nhsx.covid19.android.app.remote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest

class MockEpidemiologyDataApi : EpidemiologyDataApi {
    private val lastRequestLiveData = MutableLiveData<EpidemiologyRequest>()
    fun lastRequest(): LiveData<EpidemiologyRequest> = lastRequestLiveData

    override suspend fun submitEpidemiologyData(epidemiologyRequest: EpidemiologyRequest) =
        MockApiModule.behaviour.invoke {
            lastRequestLiveData.postValue(epidemiologyRequest)
        }
}

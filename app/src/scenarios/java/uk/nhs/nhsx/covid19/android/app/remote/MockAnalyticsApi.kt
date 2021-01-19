package uk.nhs.nhsx.covid19.android.app.remote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload

class MockAnalyticsApi : AnalyticsApi {
    private val lastRequestLiveData = MutableLiveData<AnalyticsPayload>()
    fun lastRequest(): LiveData<AnalyticsPayload> = lastRequestLiveData

    override suspend fun submitAnalytics(analyticsPayload: AnalyticsPayload) {
        lastRequestLiveData.postValue(analyticsPayload)
    }
}

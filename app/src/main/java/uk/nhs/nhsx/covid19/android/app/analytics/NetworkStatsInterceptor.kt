package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import javax.inject.Inject

class NetworkStatsInterceptor @Inject constructor(
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : Interceptor {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        GlobalScope.launch(Dispatchers.IO) {
            analyticsEventProcessor.track(AnalyticsEvent.UpdateNetworkStats)
        }
        return response
    }
}

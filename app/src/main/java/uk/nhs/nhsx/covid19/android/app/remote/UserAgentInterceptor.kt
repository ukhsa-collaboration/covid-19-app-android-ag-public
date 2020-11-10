package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import uk.nhs.nhsx.covid19.android.app.common.AppInfo
import javax.inject.Inject

class UserAgentInterceptor @Inject constructor(
    appInfo: AppInfo
) : Interceptor {

    private val userAgent: String by lazy {
        "p=Android,o=${appInfo.osVersion},v=${appInfo.shortVersionName},b=${appInfo.buildNumber}"
    }

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader(HEADER_USER_AGENT, userAgent)
            .build()
        return chain.proceed(newRequest)
    }

    companion object {
        const val HEADER_USER_AGENT = "User-Agent"
    }
}

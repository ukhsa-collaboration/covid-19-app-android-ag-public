package uk.nhs.nhsx.covid19.android.app.analytics

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import javax.inject.Inject

/**
 * By default, OkHttp is very aggressive when it comes to caching and it will use an heuristics based on Last-Modified
 * header to skip some network calls (see https://github.com/square/okhttp/issues/4073).
 * The mission of the class is to align Android and iOS client, where Last-Modified header is not used for caching purposes.
 * The class must be added to OkHttpClient as a network interceptor.
 */
class StripOutLastModifiedHeaderInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        return response.newBuilder().removeHeader("Last-Modified").build()
    }
}

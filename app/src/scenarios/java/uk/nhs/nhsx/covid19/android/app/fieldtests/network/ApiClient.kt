package uk.nhs.nhsx.covid19.android.app.fieldtests.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import uk.nhs.covid19.config.FieldTestConfiguration

object ApiClient {

    val service: FieldTestApi by lazy {

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder().apply {
                    FieldTestConfiguration.distributedRemote.headers.forEach { (name, value) ->
                        addHeader(name, value)
                    }
                }
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(FieldTestConfiguration.distributedRemoteBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        retrofit.create(FieldTestApi::class.java)
    }
}

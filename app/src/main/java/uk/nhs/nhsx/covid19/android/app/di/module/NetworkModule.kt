package uk.nhs.nhsx.covid19.android.app.di.module

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.TlsVersion.TLS_1_2
import okhttp3.TlsVersion.TLS_1_3
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import uk.nhs.covid19.config.EnvironmentConfiguration
import uk.nhs.covid19.config.Remote
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.analytics.NetworkStatsInterceptor
import uk.nhs.nhsx.covid19.android.app.di.module.SignatureValidationInterceptor.Companion.HEADER_REQUEST_ID
import uk.nhs.nhsx.covid19.android.app.network.TrafficLengthObfuscationInterceptor
import uk.nhs.nhsx.covid19.android.app.remote.UserAgentInterceptor
import uk.nhs.nhsx.covid19.android.app.util.Base64Decoder
import uk.nhs.nhsx.covid19.android.app.util.adapters.ColorSchemeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.ContentBlockTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalMessageTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.PolicyIconAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.RiskyVenueMessageTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.TranslatableLocalMessageAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.TranslatableStringAdapter
import uk.nhs.riskscore.ObservationType
import uk.nhs.riskscore.ObservationType.gen
import java.util.UUID
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named
import javax.inject.Singleton

@Module
class NetworkModule(
    private val configuration: EnvironmentConfiguration,
    private val interceptors: List<Interceptor>
) {
    @Provides
    @Singleton
    @Named(DISTRIBUTION_REMOTE)
    fun provideDistributionRemoteRetrofit(
        @Named(DISTRIBUTION_REMOTE) okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(configuration.distributedRemoteBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @Named(API_REMOTE)
    fun provideApiRemoteRetrofit(
        @Named(API_REMOTE) okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(configuration.apiRemoteBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @Named(DISTRIBUTION_REMOTE)
    fun provideDistributionOkHttpClient(
        base64Decoder: Base64Decoder,
        networkStatsInterceptor: NetworkStatsInterceptor,
        applicationContext: Context
    ): OkHttpClient {
        val signatureValidationInterceptor = createSignatureValidationInterceptor(
            base64Decoder,
            configuration.distributedRemote,
            dynamicContent = false
        )
        return createOkHttpClient(
            configuration.distributedRemote,
            signatureValidationInterceptor,
            interceptors,
            networkStatsInterceptor,
            trafficLengthObfuscationInterceptor = null,
            applicationContext
        )
    }

    @Provides
    @Singleton
    @Named(API_REMOTE)
    fun provideApiOkHttpClient(
        base64Decoder: Base64Decoder,
        networkStatsInterceptor: NetworkStatsInterceptor,
        userAgentInterceptor: UserAgentInterceptor,
        trafficLengthObfuscationInterceptor: TrafficLengthObfuscationInterceptor,
        applicationContext: Context
    ): OkHttpClient {
        val signatureValidationInterceptor = createSignatureValidationInterceptor(
            base64Decoder,
            configuration.apiRemote,
            dynamicContent = true
        )
        return createOkHttpClient(
            configuration.apiRemote,
            signatureValidationInterceptor,
            listOf(userAgentInterceptor) + interceptors,
            networkStatsInterceptor,
            trafficLengthObfuscationInterceptor,
            applicationContext
        )
    }

    private fun createSignatureValidationInterceptor(
        base64Decoder: Base64Decoder,
        remote: Remote,
        dynamicContent: Boolean
    ): SignatureValidationInterceptor = SignatureValidationInterceptor(
        base64Decoder,
        remote.signatureKeys,
        dynamicContent
    )

    private fun createOkHttpClient(
        remote: Remote,
        signatureValidationInterceptor: SignatureValidationInterceptor,
        interceptors: List<Interceptor>,
        networkStatsInterceptor: NetworkStatsInterceptor,
        trafficLengthObfuscationInterceptor: TrafficLengthObfuscationInterceptor?,
        applicationContext: Context
    ): OkHttpClient {
        val certificatePinnerBuilder = CertificatePinner.Builder().apply {
            remote.certificates.forEach { certificate ->
                add(remote.host, "sha256/$certificate")
            }
        }.build()

        val connectionSpecs = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TLS_1_2, TLS_1_3)
            .build()

        val builder = OkHttpClient.Builder()
            .cache(Cache(applicationContext.cacheDir, CACHE_SIZE_BYTES))
            .certificatePinner(certificatePinnerBuilder)
            .connectionSpecs(listOf(connectionSpecs))
            .followRedirects(true)
            .followSslRedirects(false)
            .connectTimeout(1, MINUTES)
            .readTimeout(1, MINUTES)
            .writeTimeout(1, MINUTES)
            .callTimeout(2, MINUTES)
            .addInterceptor { chain ->
                val request = chain.request()
                val newRequest = request.newBuilder().apply {
                    remote.headers.forEach { (name, value) ->
                        addHeader(name, value)
                    }
                    addHeader(HEADER_REQUEST_ID, UUID.randomUUID().toString())
                }
                    .build()
                chain.proceed(newRequest)
            }
            .addInterceptor(signatureValidationInterceptor)
            .apply {
                trafficLengthObfuscationInterceptor?.let { addInterceptor(it) }
            }
            .addInterceptor(networkStatsInterceptor)
            .apply {
                interceptors.forEach {
                    addInterceptor(it)
                }
            }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(LocalDateAdapter())
            .add(InstantAdapter())
            .add(TranslatableStringAdapter())
            .add(TranslatableLocalMessageAdapter())
            .add(PolicyIconAdapter())
            .add(ColorSchemeAdapter())
            .add(RiskyVenueMessageTypeAdapter())
            .add(LocalMessageTypeAdapter())
            .add(ContentBlockTypeAdapter())
            .add(
                ObservationType::class.java,
                EnumJsonAdapter.create(ObservationType::class.java).withUnknownFallback(gen)
            )
            .add(AnalyticsLogStorage.analyticsLogItemAdapter)
            .build()
    }

    companion object {
        const val DISTRIBUTION_REMOTE = "DISTRIBUTION_REMOTE"
        const val API_REMOTE = "API_REMOTE"
        const val CACHE_SIZE_BYTES: Long = 1024 * 1024 * 2
    }
}

package uk.nhs.nhsx.covid19.android.app.di.module

import com.moczul.ok2curl.CurlInterceptor
import com.moczul.ok2curl.logger.Loggable
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion.TLS_1_2
import okhttp3.TlsVersion.TLS_1_3
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import uk.nhs.covid19.config.EnvironmentConfiguration
import uk.nhs.covid19.config.Remote
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.di.module.SignatureValidationInterceptor.Companion.HEADER_REQUEST_ID
import uk.nhs.nhsx.covid19.android.app.state.StateJson
import uk.nhs.nhsx.covid19.android.app.util.Base64Decoder
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.util.UUID
import javax.inject.Named
import javax.inject.Singleton

@Module
class NetworkModule(
    private val configuration: EnvironmentConfiguration
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
        httpLoggingInterceptor: HttpLoggingInterceptor,
        base64Decoder: Base64Decoder
    ): OkHttpClient {
        val signatureValidationInterceptor = createSignatureValidationInterceptor(
            base64Decoder,
            configuration.distributedRemote,
            dynamicContent = false
        )
        return createOkHttpClient(
            configuration.distributedRemote,
            httpLoggingInterceptor,
            signatureValidationInterceptor
        )
    }

    @Provides
    @Singleton
    @Named(API_REMOTE)
    fun provideApiOkHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor,
        base64Decoder: Base64Decoder
    ): OkHttpClient {
        val signatureValidationInterceptor = createSignatureValidationInterceptor(
            base64Decoder,
            configuration.apiRemote,
            dynamicContent = true
        )
        return createOkHttpClient(
            configuration.apiRemote,
            httpLoggingInterceptor,
            signatureValidationInterceptor
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
        httpLoggingInterceptor: HttpLoggingInterceptor,
        signatureValidationInterceptor: SignatureValidationInterceptor
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
            .certificatePinner(certificatePinnerBuilder)
            .connectionSpecs(listOf(connectionSpecs))
            .followRedirects(true)
            .followSslRedirects(false)
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder().apply {
                    remote.headers.forEach { (name, value) ->
                        addHeader(name, value)
                    }
                    addHeader(HEADER_REQUEST_ID, UUID.randomUUID().toString())
                }
                    .build()
                chain.proceed(newRequest)
            }
            .addInterceptor(signatureValidationInterceptor)

        if (BuildConfig.DEBUG) {
            builder
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(
                    CurlInterceptor(
                        Loggable { log ->
                            Timber.tag("Ok2Curl").v(log)
                        }
                    )
                )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun getHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return httpLoggingInterceptor
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(StateJson.stateMoshiAdapter)
            .add(LocalDateAdapter())
            .add(InstantAdapter())
            .build()
    }

    companion object {
        const val DISTRIBUTION_REMOTE = "DISTRIBUTION_REMOTE"
        const val API_REMOTE = "API_REMOTE"
    }
}

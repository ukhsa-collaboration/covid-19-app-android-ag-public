package uk.nhs.nhsx.covid19.android.app.di

import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule.Companion.DISTRIBUTION_REMOTE
import uk.nhs.nhsx.covid19.android.app.remote.ActivationApi
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.AppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysDistributionApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockActivationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockAnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockAppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.MockExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.MockExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockIsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockQuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenueCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.QuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import javax.inject.Named
import javax.inject.Singleton

@Module
class MockApiModule {
    @Provides
    @Singleton
    fun provideKeysSubmissionApi(@Named(DISTRIBUTION_REMOTE) retrofit: Retrofit): KeysSubmissionApi {
        return retrofit.create(KeysSubmissionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideKeysDistributionApi(@Named(DISTRIBUTION_REMOTE) retrofit: Retrofit): KeysDistributionApi {
        return retrofit.create(KeysDistributionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRiskyPostDistrictsApi(): RiskyPostDistrictsApi =
        MockRiskyPostDistrictsApi()

    @Provides
    @Singleton
    fun provideRiskyVenuesApi(): RiskyVenuesApi =
        MockRiskyVenuesApi()

    @Provides
    @Singleton
    fun provideQuestionnaireApi(): QuestionnaireApi =
        MockQuestionnaireApi()

    @Provides
    @Singleton
    fun provideVirologyTestingApi(): VirologyTestingApi =
        MockVirologyTestingApi()

    @Provides
    @Singleton
    fun provideExposureCircuitBreaker(): ExposureCircuitBreakerApi =
        MockExposureCircuitBreakerApi()

    @Provides
    @Singleton
    fun provideExposureConfiguration(): ExposureConfigurationApi =
        MockExposureConfigurationApi()

    @Provides
    @Singleton
    fun provideRiskyVenuesCircuitBreaker(): RiskyVenuesCircuitBreakerApi =
        MockRiskyVenueCircuitBreakerApi()

    @Provides
    @Singleton
    fun provideIsolationConfigurationApi(): IsolationConfigurationApi =
        MockIsolationConfigurationApi()

    @Provides
    @Singleton
    fun provideAppAvailabilityApi(): AppAvailabilityApi =
        MockAppAvailabilityApi()

    @Provides
    @Singleton
    fun provideActivationApi(): ActivationApi =
        MockActivationApi()

    @Provides
    @Singleton
    fun provideAnalyticsApi(): AnalyticsApi =
        MockAnalyticsApi()
}

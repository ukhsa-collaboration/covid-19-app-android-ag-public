package uk.nhs.nhsx.covid19.android.app.testhelpers

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.AppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysDistributionApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockAnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockAppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.MockExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.MockExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockIsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockKeysDistributionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockKeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockQuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenueCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.QuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import javax.inject.Singleton

@Module
class ManagedApiModule(
    private val virologyTestingApi: VirologyTestingApi,
    private val questionnaireApi: MockQuestionnaireApi
) {

    @Provides
    @Singleton
    fun provideKeysSubmissionApi(): KeysSubmissionApi =
        MockKeysSubmissionApi()

    @Provides
    @Singleton
    fun provideKeysDistributionApi(): KeysDistributionApi =
        MockKeysDistributionApi()

    @Provides
    @Singleton
    fun provideRiskyPostDistrictsApi(): RiskyPostDistrictsApi = MockRiskyPostDistrictsApi()

    @Provides
    @Singleton
    fun provideRiskyVenuesApi(): RiskyVenuesApi = MockRiskyVenuesApi()

    @Provides
    @Singleton
    fun provideQuestionnaireApi(): QuestionnaireApi = questionnaireApi

    @Provides
    @Singleton
    fun provideVirologyTestingApi(): VirologyTestingApi = virologyTestingApi

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
    fun provideAnalyticsApi(): AnalyticsApi =
        MockAnalyticsApi()
}

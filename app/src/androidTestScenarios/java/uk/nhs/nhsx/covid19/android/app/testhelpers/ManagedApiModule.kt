package uk.nhs.nhsx.covid19.android.app.testhelpers

import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.AppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.IsolationPaymentApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysDistributionApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.LocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.remote.LocalStatsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockAnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockAppAvailabilityApi
import uk.nhs.nhsx.covid19.android.app.remote.MockEmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.MockEpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.MockExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.MockExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockIsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.MockKeysDistributionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockKeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockLocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.remote.MockLocalStatsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockQuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRemoteServiceExceptionCrashReportSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenueCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.MockRiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.QuestionnaireApi
import uk.nhs.nhsx.covid19.android.app.remote.RemoteServiceExceptionCrashReportSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyPostDistrictsApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import javax.inject.Singleton

@Module
class ManagedApiModule(
    private val riskyVenuesApi: RiskyVenuesApi,
    private val virologyTestingApi: VirologyTestingApi,
    private val questionnaireApi: MockQuestionnaireApi,
    private val isolationPaymentApi: IsolationPaymentApi,
    private val keysSubmissionApi: MockKeysSubmissionApi,
    private val analyticsApi: MockAnalyticsApi,
    private val mockEpidemiologyDataApi: MockEpidemiologyDataApi,
    private val localMessagesApi: MockLocalMessagesApi,
    private val mockIsolationConfigurationApi: MockIsolationConfigurationApi
) {

    @Provides
    @Singleton
    fun provideKeysSubmissionApi(): KeysSubmissionApi =
        keysSubmissionApi

    @Provides
    @Singleton
    fun provideKeysDistributionApi(): KeysDistributionApi =
        MockKeysDistributionApi()

    @Provides
    @Singleton
    fun provideRiskyPostDistrictsApi(): RiskyPostDistrictsApi = MockRiskyPostDistrictsApi()

    @Provides
    @Singleton
    fun provideRiskyVenuesApi(): RiskyVenuesApi = riskyVenuesApi

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
    fun provideIsolationConfigurationApi(): IsolationConfigurationApi = mockIsolationConfigurationApi

    @Provides
    @Singleton
    fun provideAppAvailabilityApi(): AppAvailabilityApi =
        MockAppAvailabilityApi()

    @Provides
    @Singleton
    fun provideAnalyticsApi(): AnalyticsApi = analyticsApi

    @Provides
    @Singleton
    fun provideEpidemiologyDataApi(): EpidemiologyDataApi =
        mockEpidemiologyDataApi

    @Provides
    @Singleton
    fun provideEmptyApi(): EmptyApi =
        MockEmptyApi()

    @Provides
    @Singleton
    fun provideIsolationPaymentApi(): IsolationPaymentApi =
        isolationPaymentApi

    @Provides
    @Singleton
    fun provideRiskyVenueConfigurationApi(): RiskyVenueConfigurationApi =
        MockRiskyVenueConfigurationApi()

    @Provides
    @Singleton
    fun provideRemoteServiceExceptionCrashReportSubmissionApi(): RemoteServiceExceptionCrashReportSubmissionApi =
        MockRemoteServiceExceptionCrashReportSubmissionApi()

    @Provides
    @Singleton
    fun provideLocalMessagesApi(): LocalMessagesApi = localMessagesApi

    @Provides
    @Singleton
    fun provideLocalStatsApi(localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader): LocalStatsApi =
        MockLocalStatsApi(localAuthorityPostCodesLoader)
}

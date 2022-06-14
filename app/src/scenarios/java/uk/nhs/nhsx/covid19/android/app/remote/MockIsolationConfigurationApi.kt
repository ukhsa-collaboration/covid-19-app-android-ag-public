package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration

class MockIsolationConfigurationApi : IsolationConfigurationApi {
    private var configuration: IsolationConfiguration? = null

    override suspend fun getIsolationConfiguration(): IsolationConfigurationResponse = MockApiModule.behaviour.invoke {
        IsolationConfigurationResponse(
            englandConfiguration = configuration?.toCountrySpecificConfiguration() ?: CountrySpecificConfiguration(
                contactCase = 11,
                indexCaseSinceSelfDiagnosisOnset = 11,
                indexCaseSinceSelfDiagnosisUnknownOnset = 9,
                maxIsolation = 21,
                indexCaseSinceTestResultEndDate = 11,
                pendingTasksRetentionPeriod = 14,
                testResultPollingTokenRetentionPeriod = 28
            ),
            walesConfiguration = configuration?.toCountrySpecificConfiguration() ?: CountrySpecificConfiguration(
                contactCase = 11,
                indexCaseSinceSelfDiagnosisOnset = 6,
                indexCaseSinceSelfDiagnosisUnknownOnset = 6,
                maxIsolation = 16,
                indexCaseSinceTestResultEndDate = 6,
                pendingTasksRetentionPeriod = 14,
                testResultPollingTokenRetentionPeriod = 28
            ),
        )
    }

    fun setIsolationConfigurationForAnalytics(configuration: IsolationConfiguration) {
        this.configuration = configuration
    }

    private fun IsolationConfiguration.toCountrySpecificConfiguration(): CountrySpecificConfiguration {
        return CountrySpecificConfiguration(
            contactCase = contactCase,
            indexCaseSinceSelfDiagnosisOnset = indexCaseSinceSelfDiagnosisOnset,
            indexCaseSinceSelfDiagnosisUnknownOnset = indexCaseSinceSelfDiagnosisUnknownOnset,
            maxIsolation = maxIsolation,
            indexCaseSinceTestResultEndDate = indexCaseSinceTestResultEndDate,
            pendingTasksRetentionPeriod = pendingTasksRetentionPeriod,
            testResultPollingTokenRetentionPeriod = testResultPollingTokenRetentionPeriod
        )
    }
}

package uk.nhs.nhsx.covid19.android.app.common

import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse
import javax.inject.Inject

class ConvertIsolationConfigurationResponseToDurationDays @Inject constructor() {
    operator fun invoke(response: IsolationConfigurationResponse): DurationDays {
        with(response) {
            return DurationDays(
                england = CountrySpecificConfiguration(
                    contactCase = englandConfiguration.contactCase,
                    indexCaseSinceSelfDiagnosisOnset = englandConfiguration.indexCaseSinceSelfDiagnosisOnset,
                    indexCaseSinceSelfDiagnosisUnknownOnset = englandConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset,
                    indexCaseSinceTestResultEndDate = englandConfiguration.indexCaseSinceTestResultEndDate,
                    maxIsolation = englandConfiguration.maxIsolation,
                    pendingTasksRetentionPeriod = englandConfiguration.pendingTasksRetentionPeriod,
                    testResultPollingTokenRetentionPeriod = englandConfiguration.testResultPollingTokenRetentionPeriod
                ),
                wales = CountrySpecificConfiguration(
                    contactCase = walesConfiguration.contactCase,
                    indexCaseSinceSelfDiagnosisOnset = walesConfiguration.indexCaseSinceSelfDiagnosisOnset,
                    indexCaseSinceSelfDiagnosisUnknownOnset = walesConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset,
                    indexCaseSinceTestResultEndDate = walesConfiguration.indexCaseSinceTestResultEndDate,
                    maxIsolation = walesConfiguration.maxIsolation,
                    pendingTasksRetentionPeriod = walesConfiguration.pendingTasksRetentionPeriod,
                    testResultPollingTokenRetentionPeriod = walesConfiguration.testResultPollingTokenRetentionPeriod
                )
            )
        }
    }
}

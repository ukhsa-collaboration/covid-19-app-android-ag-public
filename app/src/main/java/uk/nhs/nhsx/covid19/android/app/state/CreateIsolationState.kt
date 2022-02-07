package uk.nhs.nhsx.covid19.android.app.state

import javax.inject.Inject

class CreateIsolationState @Inject constructor(
    private val isolationState: IsolationState,
    private val isolationConfigurationProvider: IsolationConfigurationProvider
) {
    operator fun invoke(isolationInfo: IsolationInfo): IsolationState {

        val remembersIsolation =
            isolationState.contact != null || isolationState.selfAssessment != null || isolationState.testResult != null

        val isolationConfiguration = if (remembersIsolation) {
            isolationState.isolationConfiguration
        } else {
            isolationConfigurationProvider.durationDays
        }

        return IsolationState(
            isolationConfiguration,
            isolationInfo.selfAssessment,
            isolationInfo.testResult,
            isolationInfo.contact,
            isolationInfo.hasAcknowledgedEndOfIsolation
        )
    }
}

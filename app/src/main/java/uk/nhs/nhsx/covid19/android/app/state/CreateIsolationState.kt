package uk.nhs.nhsx.covid19.android.app.state

import javax.inject.Inject

class CreateIsolationState @Inject constructor(
    private val stateStorage: StateStorage,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val createIsolationConfiguration: CreateIsolationConfiguration
) {
    operator fun invoke(isolationInfo: IsolationInfo): IsolationState {

        val isolationState = stateStorage.state
        val remembersIsolation =
            isolationState.contact != null || isolationState.selfAssessment != null || isolationState.testResult != null

        val isolationConfiguration = if (remembersIsolation) {
            isolationState.isolationConfiguration
        } else {
            createIsolationConfiguration(isolationConfigurationProvider.durationDays)
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

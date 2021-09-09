package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OptedOutForContactIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import javax.inject.Inject

class OptOutOfContactIsolation @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) {
    operator fun invoke() {
        isolationStateMachine.readState().contact?.exposureDate?.let {
            analyticsEventProcessor.track(OptedOutForContactIsolation)
            isolationStateMachine.optOutOfContactIsolation(it)
        }
    }
}

package uk.nhs.nhsx.covid19.android.app.exposure

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OptedOutForContactIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.NEW_ADVICE
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import javax.inject.Inject

class OptOutOfContactIsolation @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) {
    operator fun invoke(reason: OptOutReason) {
        isolationStateMachine.readState().contact?.exposureDate?.let {
            val event = when (reason) {
                QUESTIONNAIRE, NEW_ADVICE -> OptedOutForContactIsolation
            }
            analyticsEventProcessor.track(event)
            isolationStateMachine.optOutOfContactIsolation(it, reason)
        }
    }
}

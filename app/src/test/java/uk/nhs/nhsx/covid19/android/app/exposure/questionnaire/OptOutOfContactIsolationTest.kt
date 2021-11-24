package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OptedOutForContactIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.LocalDate

class OptOutOfContactIsolationTest {
    private val isolationStateMachine: IsolationStateMachine = mockk(relaxUnitFun = true)
    private val analyticsEventProcessor: AnalyticsEventProcessor = mockk(relaxUnitFun = true)

    private val optOutOfContactIsolation = OptOutOfContactIsolation(isolationStateMachine, analyticsEventProcessor)

    @Test
    fun `for questionnaire opt-out calls optOutOfContactIsolation on isolation state machine and tracks optedOutForContactIsolation analytics event`() {
        val expectedDate: LocalDate = mockk()
        every { isolationStateMachine.readState().contact?.exposureDate } returns expectedDate

        optOutOfContactIsolation(reason = QUESTIONNAIRE)

        verify {
            isolationStateMachine.optOutOfContactIsolation(expectedDate, reason = QUESTIONNAIRE)
            analyticsEventProcessor.track(OptedOutForContactIsolation)
        }
    }

    @Test
    fun `does not call optOutOfContactIsolation or track analytics event if there is no contact case`() {
        every { isolationStateMachine.readState().contact } returns null

        optOutOfContactIsolation(reason = QUESTIONNAIRE)

        verify(exactly = 0) { isolationStateMachine.optOutOfContactIsolation(any(), reason = QUESTIONNAIRE) }
        verify(exactly = 0) { analyticsEventProcessor.track(OptedOutForContactIsolation) }
    }
}

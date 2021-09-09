package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OptedOutForContactIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.LocalDate

class OptOutOfContactIsolationTest {
    private val isolationStateMachine: IsolationStateMachine = mockk(relaxUnitFun = true)
    private val analyticsEventProcessor: AnalyticsEventProcessor = mockk(relaxUnitFun = true)

    private val optOutOfContactIsolation = OptOutOfContactIsolation(isolationStateMachine, analyticsEventProcessor)

    @Test
    fun `calls optOutOfContactIsolation on isolation state machine and tracks optedOutForContactIsolation analytics event`() {
        val expectedDate: LocalDate = mockk()
        every { isolationStateMachine.readState().contact?.exposureDate } returns expectedDate

        optOutOfContactIsolation()

        verify {
            isolationStateMachine.optOutOfContactIsolation(expectedDate)
            analyticsEventProcessor.track(OptedOutForContactIsolation)
        }
    }

    @Test
    fun `does not call optOutOfContactIsolation or track analytics event if there is no contact case`() {
        every { isolationStateMachine.readState().contact } returns null

        optOutOfContactIsolation()

        verify(exactly = 0) { isolationStateMachine.optOutOfContactIsolation(any()) }
        verify(exactly = 0) { analyticsEventProcessor.track(OptedOutForContactIsolation) }
    }
}

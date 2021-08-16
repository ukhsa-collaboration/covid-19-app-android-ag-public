package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState

class IsolationHubReminderReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = IsolationHubReminderReceiver().apply {
        notificationProvider = mockk(relaxUnitFun = true)
        isolationHubReminderTimeProvider = mockk(relaxUnitFun = true)
        isolationStateMachine = mockk()
        clock = mockk()
    }

    private val intent = mockk<Intent>()

    @Test
    fun `when still in active isolation, triggers isolation hub reminder notification and resets reminder time`() =
        runBlocking {
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { testSubject.isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIsolation(testSubject.clock) } returns true

            testSubject.onReceive(context, intent)

            verify {
                testSubject.notificationProvider.showIsolationHubReminderNotification()
                testSubject.isolationHubReminderTimeProvider setProperty "value" value null
            }
        }

    @Test
    fun `when not in active isolation then reset reminder time without triggering isolation hub reminder notification`() =
        runBlocking {
            val isolationLogicalState = mockk<IsolationLogicalState>()
            every { testSubject.isolationStateMachine.readLogicalState() } returns isolationLogicalState
            every { isolationLogicalState.isActiveIsolation(testSubject.clock) } returns false

            testSubject.onReceive(context, intent)

            verify(exactly = 0) { testSubject.notificationProvider.showIsolationHubReminderNotification() }
            verify { testSubject.isolationHubReminderTimeProvider setProperty "value" value null }
        }
}

package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import java.time.Instant
import java.time.LocalDate

class AlarmRestarterTest : FieldInjectionUnitTest() {

    private val testSubject = AlarmRestarter().apply {
        isolationStateMachine = mockk()
        isolationExpirationAlarmController = mockk(relaxed = true)
        exposureNotificationReminderAlarmController = mockk(relaxed = true)
        resumeContactTracingNotificationTimeProvider = mockk()
    }

    private val intent = mockk<Intent>(relaxed = true)

    private val testIsolation = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(
            symptomsOnsetDate = LocalDate.now(),
            expiryDate = LocalDate.now(),
            selfAssessment = true
        )
    )

    @Test
    fun `intent action is not ACTION_BOOT_COMPLETED or ACTION_MY_PACKAGE_REPLACED has no side-effects`() {
        every { intent.action } returns Intent.ACTION_LOCKED_BOOT_COMPLETED

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.isolationStateMachine.readState() }
    }

    @Test
    fun `default state does not set alarm`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.isolationStateMachine.readState() } returns Default()

        testSubject.onReceive(context, intent)

        verify(exactly = 0) {
            testSubject.isolationExpirationAlarmController.setupExpirationCheck(
                any()
            )
        }
    }

    @Test
    fun `when in isolation state set up expiration check and no contact tracing paused`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.isolationStateMachine.readState() } returns testIsolation
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns null

        testSubject.onReceive(context, intent)

        verify {
            testSubject.isolationExpirationAlarmController.setupExpirationCheck(
                any()
            )
        }
        verify(exactly = 0) { testSubject.exposureNotificationReminderAlarmController.setup(any()) }
    }

    @Test
    fun `when in isolation state set up expiration check and contact tracing paused`() {
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
        every { testSubject.isolationStateMachine.readState() } returns testIsolation
        every { testSubject.resumeContactTracingNotificationTimeProvider.value } returns 1000L

        testSubject.onReceive(context, intent)

        verify {
            testSubject.isolationExpirationAlarmController.setupExpirationCheck(
                any()
            )
        }
        verify { testSubject.exposureNotificationReminderAlarmController.setup(any()) }
    }
}

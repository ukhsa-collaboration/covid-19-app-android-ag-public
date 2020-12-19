package uk.nhs.nhsx.covid19.android.app.state

import androidx.work.ListenableWorker.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class DisplayStateExpirationNotificationTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)

    @Test
    fun `on default state don't send notification`() = runBlocking {
        val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

        every { isolationStateMachine.readState() } returns Default()

        val testSubject = DisplayStateExpirationNotification(
            isolationStateMachine,
            notificationProvider,
            userInbox,
            fixedClock
        )

        val result = testSubject.doWork()

        verify(exactly = 0) { notificationProvider.showStateExpirationNotification() }

        assertEquals(Result.success(), result)
    }

    @Test
    fun `when not default state send notification when expiry date is tomorrow and time now is 9pm`() =
        runBlocking {

            val fixedClock = Clock.fixed(Instant.parse("2020-05-21T21:00:00Z"), ZoneOffset.UTC)

            val symptomsOnset = LocalDate.parse("2020-05-20")

            val startDate = Instant.now(fixedClock)
            val expiryDate = LocalDate.of(2020, 5, 22)
            every { isolationStateMachine.readState(validateExpiry = false) } returns Isolation(
                startDate,
                DurationDays(),
                indexCase = IndexCase(symptomsOnset, expiryDate, true)
            )

            val testSubject = DisplayStateExpirationNotification(
                isolationStateMachine,
                notificationProvider,
                userInbox,
                fixedClock
            )

            val result = testSubject.doWork()

            verify(exactly = 1) { notificationProvider.showStateExpirationNotification() }

            assertEquals(Result.success(), result)
        }

    @Test
    fun `when not default state don't send notification when expiry date is not tomorrow`() =
        runBlocking {

            val fixedClock = Clock.fixed(Instant.parse("2020-05-21T21:00:00Z"), ZoneOffset.UTC)

            val symptomsOnset = LocalDate.parse("2020-05-20")

            val startDate = Instant.now(fixedClock)
            val expiryDate = LocalDate.of(2020, 5, 23)
            every { isolationStateMachine.readState() } returns Isolation(
                startDate,
                DurationDays(),
                indexCase = IndexCase(symptomsOnset, expiryDate, true)
            )

            val testSubject = DisplayStateExpirationNotification(
                isolationStateMachine,
                notificationProvider,
                userInbox,
                fixedClock
            )

            val result = testSubject.doWork()

            verify(exactly = 0) { notificationProvider.showStateExpirationNotification() }

            assertEquals(Result.success(), result)
        }
}

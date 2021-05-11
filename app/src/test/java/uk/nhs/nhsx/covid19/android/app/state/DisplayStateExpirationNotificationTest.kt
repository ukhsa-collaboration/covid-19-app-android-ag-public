package uk.nhs.nhsx.covid19.android.app.state

import androidx.work.ListenableWorker.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.DoNotNotify
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.Notify
import java.time.LocalDate
import kotlin.test.assertEquals

class DisplayStateExpirationNotificationTest {

    private val shouldNotifyStateExpiration = mockk<ShouldNotifyStateExpiration>()
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val isolationExpirationAlarmProvider = mockk<IsolationExpirationAlarmProvider>(relaxUnitFun = true)

    private val testSubject = DisplayStateExpirationNotification(
        shouldNotifyStateExpiration,
        notificationProvider,
        isolationExpirationAlarmProvider
    )

    @Test
    fun `don't send notification if shouldNotifyStateExpiration returns DoNotNotify`() = runBlocking {
        every { shouldNotifyStateExpiration() } returns DoNotNotify

        val result = testSubject.doWork()

        verify { isolationExpirationAlarmProvider.value = null }

        verify(exactly = 0) { notificationProvider.showStateExpirationNotification() }

        assertEquals(Result.success(), result)
    }

    @Test
    fun `send notification if shouldNotifyStateExpiration returns Notify`() = runBlocking {
        every { shouldNotifyStateExpiration() } returns Notify(LocalDate.now())

        val result = testSubject.doWork()

        verify { isolationExpirationAlarmProvider.value = null }

        verify(exactly = 1) { notificationProvider.showStateExpirationNotification() }

        assertEquals(Result.success(), result)
    }
}

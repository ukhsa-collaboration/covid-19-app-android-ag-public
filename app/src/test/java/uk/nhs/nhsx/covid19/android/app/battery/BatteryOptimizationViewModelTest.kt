package uk.nhs.nhsx.covid19.android.app.battery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BatteryOptimizationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val batteryOptimizationAcknowledgementProvider = mockk<BatteryOptimizationAcknowledgementProvider>(relaxed = true)
    private val batteryOptimizationRequired = mockk<BatteryOptimizationRequired>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-01T10:00:00Z"), ZoneOffset.UTC)

    private val acknowledgementObserver = mockk<Observer<Void>>(relaxed = true)

    private val testSubject = BatteryOptimizationViewModel(
        batteryOptimizationAcknowledgementProvider,
        batteryOptimizationRequired,
        fixedClock
    )

    @Before
    fun setUp() {
        testSubject.onAcknowledge().observeForever(acknowledgementObserver)
    }

    @Test
    fun `when battery optimization is not required should set acknowledged timestamp and fire acknowledged event`() {
        every { batteryOptimizationRequired() } returns false

        testSubject.onCreate()

        verify {
            batteryOptimizationAcknowledgementProvider setProperty "value" value eq(Instant.now(fixedClock).toEpochMilli())
            acknowledgementObserver.onChanged(null)
        }
    }

    @Test
    fun `when battery optimization is required should neither set acknowledged timestamp nor fire acknowledged event`() {
        every { batteryOptimizationRequired() } returns true

        testSubject.onCreate()

        verify(exactly = 0) {
            batteryOptimizationAcknowledgementProvider setProperty "value" value any<Long>()
            acknowledgementObserver.onChanged(any())
        }
    }

    @Test
    fun `on acknowledgement should set acknowledged timestamp and fire acknowledged event`() {
        testSubject.onIgnoreBatteryOptimizationAcknowledged()

        verify {
            batteryOptimizationAcknowledgementProvider setProperty "value" value eq(Instant.now(fixedClock).toEpochMilli())
            acknowledgementObserver.onChanged(null)
        }
    }
}

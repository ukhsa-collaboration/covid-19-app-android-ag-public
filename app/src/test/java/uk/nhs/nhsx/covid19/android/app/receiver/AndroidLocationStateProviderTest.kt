package uk.nhs.nhsx.covid19.android.app.receiver

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED

class AndroidLocationStateProviderTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val context = mockk<Context>(relaxed = true)
    private val testSubject = AndroidLocationStateProvider(context)
    private val intent = mockk<Intent>(relaxUnitFun = true)
    private val locationManager = mockk<LocationManager>(relaxUnitFun = true)

    private val availabilityState = mockk<Observer<AvailabilityState>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        mockkStatic(LocationManagerCompat::class)
        every { context.getSystemService(any()) } returns locationManager
        testSubject.availabilityState.observeForever(availabilityState)
    }

    @Test
    fun `start broadcasts state and registers receiver`() = runBlocking {
        every { locationManager.isProviderEnabled(any()) } returns true

        testSubject.start(context)

        verify(exactly = 1) { availabilityState.onChanged(any()) }
        verify { context.registerReceiver(testSubject, any()) }
    }

    @Test
    fun `stop unregisters receiver with no side effects`() = runBlocking {
        testSubject.stop(context)

        verify(exactly = 0) { availabilityState.onChanged(any()) }
        verify { context.unregisterReceiver(testSubject) }
    }

    @Test
    fun `no value publishing if intent action is not PROVIDERS_CHANGED_ACTION`() = runBlocking {
        every { intent.action } returns LocationManager.MODE_CHANGED_ACTION

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { availabilityState.onChanged(any()) }
    }

    @Test
    fun `receiving broadcast triggers post value on observer with state on`() = runBlocking {
        every { intent.action } returns LocationManager.PROVIDERS_CHANGED_ACTION
        every { intent.getIntExtra(any(), any()) } returns BluetoothAdapter.STATE_ON
        every { LocationManagerCompat.isLocationEnabled(locationManager) } returns true

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { availabilityState.onChanged(ENABLED) }
    }

    @Test
    fun `receiving broadcast triggers post value on observer with state off`() = runBlocking {
        every { intent.action } returns LocationManager.PROVIDERS_CHANGED_ACTION
        every { intent.getIntExtra(any(), any()) } returns BluetoothAdapter.STATE_OFF
        every { LocationManagerCompat.isLocationEnabled(locationManager) } returns false

        testSubject.onReceive(context, intent)

        verify(exactly = 1) { availabilityState.onChanged(DISABLED) }
    }
}

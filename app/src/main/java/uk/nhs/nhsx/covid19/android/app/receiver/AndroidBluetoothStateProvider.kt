package uk.nhs.nhsx.covid19.android.app.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ERROR
import android.bluetooth.BluetoothAdapter.STATE_ON
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class AndroidBluetoothStateProvider : AvailabilityStateProvider,
    BroadcastReceiver() {
    private val bluetoothStateMutable = SingleLiveEvent<AvailabilityState>()

    private fun updateState() {
        val isEmulator = BuildConfig.DEBUG && BluetoothAdapter.getDefaultAdapter() == null
        val currentState =
            if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true || isEmulator) {
                ENABLED
            } else {
                DISABLED
            }
        bluetoothStateMutable.postValue(currentState)
    }

    override val availabilityState: LiveData<AvailabilityState> = distinctUntilChanged(bluetoothStateMutable)

    override fun start(context: Context) {
        updateState()
        context.registerReceiver(this, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun stop(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
            return
        }
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, ERROR)

        val bluetoothState = if (state == STATE_ON) ENABLED else DISABLED

        bluetoothStateMutable.postValue(bluetoothState)
    }
}

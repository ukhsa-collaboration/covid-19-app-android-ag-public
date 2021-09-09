package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Context
import androidx.lifecycle.LiveData
import uk.nhs.nhsx.covid19.android.app.BuildConfig

interface AvailabilityStateProvider {
    val availabilityState: LiveData<AvailabilityState>

    fun start(context: Context)
    fun stop(context: Context)
    fun getState(isDebug: Boolean = BuildConfig.DEBUG): AvailabilityState
}

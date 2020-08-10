package uk.nhs.nhsx.covid19.android.app.fieldtests.utils

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ExposureEvent

object ExposureListener {
    private val listeners: MutableList<((String, ExposureEvent) -> Unit)> = mutableListOf()

    fun addListener(listener: (String, ExposureEvent) -> Unit) {
        listeners.add(listener)
    }

    fun onExposureResult(token: String, exposureEvent: ExposureEvent) {
        Timber.d("onExposureResult: $token event = $exposureEvent")
        listeners.forEach { it.invoke(token, exposureEvent) }
    }
}

package uk.nhs.nhsx.covid19.android.app.status

class ScenariosDateChangeBroadcastReceiver : DateChangeBroadcastReceiver() {
    fun trigger() {
        callback?.invoke()
    }
}

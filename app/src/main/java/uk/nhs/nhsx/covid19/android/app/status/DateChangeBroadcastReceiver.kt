package uk.nhs.nhsx.covid19.android.app.status

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE

open class DateChangeBroadcastReceiver : BroadcastReceiver(), DateChangeReceiver {
    @VisibleForTesting(otherwise = PRIVATE)
    internal var callback: (() -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        callback?.invoke()
    }

    override fun registerReceiver(activity: Activity, callback: () -> Unit) {
        this.callback = callback
        activity.registerReceiver(this, IntentFilter(Intent.ACTION_DATE_CHANGED))
    }

    override fun unregisterReceiver(activity: Activity) {
        this.callback = null
        activity.unregisterReceiver(this)
    }
}

interface DateChangeReceiver {
    fun registerReceiver(activity: Activity, callback: () -> Unit)
    fun unregisterReceiver(activity: Activity)
}

package uk.nhs.nhsx.covid19.android.app.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * That class is needed for unit testing, since PendingIntent.getBroadcast returns null in unit tests
 */
@Singleton
class BroadcastProvider @Inject constructor() {
    fun getBroadcast(context: Context, requestCode: Int, clazz: Class<*>, flags: Int) =
        PendingIntent.getBroadcast(context, requestCode, Intent(context, clazz), flags)
}

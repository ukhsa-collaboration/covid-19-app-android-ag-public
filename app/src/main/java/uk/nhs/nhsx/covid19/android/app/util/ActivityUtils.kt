package uk.nhs.nhsx.covid19.android.app.util

import android.app.Activity
import android.content.Intent
import timber.log.Timber

fun Activity.startActivitySafely(
    intent: Intent,
    handler: (Exception) -> Unit = {
        Timber.d(it, "Starting activity failed")
    }
) {
    try {
        startActivity(intent)
    } catch (exception: Exception) {
        handler.invoke(exception)
    }
}

fun Activity.startActivityForResultSafely(
    intent: Intent,
    requestCode: Int,
    handler: (Exception) -> Unit = {
        Timber.d(it, "Starting activity failed")
    }
) {
    try {
        startActivityForResult(intent, requestCode)
    } catch (exception: Exception) {
        handler.invoke(exception)
    }
}

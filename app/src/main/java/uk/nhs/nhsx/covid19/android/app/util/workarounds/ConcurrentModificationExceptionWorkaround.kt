package uk.nhs.nhsx.covid19.android.app.util.workarounds

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.System
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object ConcurrentModificationExceptionWorkaround {

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    val shouldApplyWorkaround: Boolean by lazy {
        isRunningInFirebase()
    }

    private fun isRunningInFirebase(): Boolean {
        val context = context ?: return false
        val testLabSetting: String? = System.getString(context.contentResolver, "firebase.test.lab")
        val isRunningInFirebase = testLabSetting == "true"
        Timber.d("isRunningInFirebase == $isRunningInFirebase")
        return isRunningInFirebase
    }
}

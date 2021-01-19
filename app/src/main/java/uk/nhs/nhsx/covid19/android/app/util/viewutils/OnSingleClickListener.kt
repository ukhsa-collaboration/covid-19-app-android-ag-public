package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.view.View
import android.widget.TextView
import timber.log.Timber

/**
 * Listener that ignores multiple clicks that happen too close in time and reacts only to the first one
 */
abstract class OnSingleClickListener : View.OnClickListener {

    private var lastClickTime: Long = 0

    abstract fun onSingleClick(v: View?)

    override fun onClick(v: View?) {
        val currentClickTime: Long = System.currentTimeMillis()
        val elapsedTime = currentClickTime - lastClickTime
        lastClickTime = currentClickTime
        if (elapsedTime > MIN_CLICK_INTERVAL) {
            Timber.d("Clicking ${v?.javaClass?.simpleName} \"${(v as? TextView)?.text}\"") // TODO remove
            onSingleClick(v)
        } else {
            Timber.d("Omitting click on ${v?.javaClass?.simpleName} \"${(v as? TextView)?.text}\". Elapsed time $elapsedTime") // TODO remove
        }
    }

    companion object {
        private const val MIN_CLICK_INTERVAL: Long = 600
    }
}

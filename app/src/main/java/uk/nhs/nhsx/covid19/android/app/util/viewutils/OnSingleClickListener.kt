package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.view.View

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
            onSingleClick(v)
        }
    }

    companion object {
        private const val MIN_CLICK_INTERVAL: Long = 600
    }
}

package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.material.appbar.MaterialToolbar
import uk.nhs.nhsx.covid19.android.app.R

fun Context.smallestScreenWidth(): Int = resources.configuration.smallestScreenWidthDp

fun AppCompatActivity.setNavigateUpToolbar(
    toolbar: MaterialToolbar,
    @StringRes titleResId: Int,
    @DrawableRes homeIndicator: Int = R.drawable.ic_arrow_back_primary,
    listenerAction: () -> Unit = {}
) {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeAsUpIndicator(homeIndicator)
    supportActionBar?.setHomeActionContentDescription(R.string.go_back)
    supportActionBar?.title = getString(titleResId)
    toolbar.setNavigationOnClickListener {
        listenerAction()
        onBackPressed()
    }

    toolbar.getChildAt(0)?.let {
        if (it is TextView) {
            it.setUpAccessibilityHeading()
        }
    }
}

/**
 * Reads headings for devices with api 19+.
 * This is handling accessibility headings in a better way
 * than just using the xml attribute => (android:accessibilityHeading="true")
 * because this attribute only works for api 28+.
 */
fun View.setUpAccessibilityHeading() {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View?,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isHeading = true
            }
        }
    )
}

fun TextView.setUpOpensInBrowserWarning() {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.roleDescription =
                    context.getString(R.string.accessibility_announcement_link, text)
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        context.getString(R.string.open_in_browser_warning)
                    )
                )
            }
        }
    )
}

fun TextView.setUpAccessibilityButton() {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription =
                    context.getString(R.string.accessibility_announcement_button, text)
            }
        }
    )
}

fun Context.announce(textToAnnounce: String) {
    val accessibilityEvent = AccessibilityEvent.obtain().apply {
        eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
        text.add(textToAnnounce)
    }
    val accessibilityManager =
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    if (accessibilityManager?.isEnabled == true) {
        accessibilityManager.sendAccessibilityEvent(
            accessibilityEvent
        )
    }
}

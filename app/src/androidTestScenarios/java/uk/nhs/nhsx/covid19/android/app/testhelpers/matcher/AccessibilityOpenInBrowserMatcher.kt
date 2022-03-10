package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.R

class AccessibilityOpenInBrowserMatcher : TypeSafeMatcher<View?>(View::class.java) {
    var resourceName: String? = null

    override fun matchesSafely(target: View?): Boolean {
        require(target != null) { "Cannot match a null view" }

        val delegate = ViewCompat.getAccessibilityDelegate(target)
        val nodeInfo = AccessibilityNodeInfoCompat.obtain(target)

        if (delegate != null && nodeInfo != null) {
            return nodeInfo.actionList.any { action ->
                action.id == AccessibilityNodeInfoCompat.ACTION_CLICK && action.label == target.context.getString(R.string.open_in_browser_warning)
            }
        }
        return false
    }

    override fun describeTo(description: Description) {
        description.appendText("with accessibility delegate ")
        if (resourceName != null) {
            description.appendText("[")
            description.appendText(resourceName)
            description.appendText("]")
        }
    }
}

fun isAnnouncedAsOpenInBrowser(): Matcher<View?> {
    return AccessibilityHeadingMatcher()
}

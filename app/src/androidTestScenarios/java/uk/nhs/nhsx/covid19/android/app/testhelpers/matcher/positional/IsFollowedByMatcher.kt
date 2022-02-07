package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.positional

import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class IsFollowedByMatcher(
    private val nextViewId: Int
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("is followed by a widget with id = $nextViewId")
    }

    override fun matchesSafely(view: View): Boolean {
        if (view.parent !is ViewGroup) return false
        val parent = view.parent as ViewGroup
        val targetViewIndex = parent.indexOfChild(view)
        val nextView = parent.findViewById<View>(nextViewId)
        val nextViewIndex = targetViewIndex + 1

        return targetViewIndex >= 0 &&
                parent.childCount > nextViewIndex &&
                parent.getChildAt(nextViewIndex) == nextView
    }
}

fun isFollowedBy(nextViewId: Int): Matcher<View> {
    return IsFollowedByMatcher(nextViewId)
}

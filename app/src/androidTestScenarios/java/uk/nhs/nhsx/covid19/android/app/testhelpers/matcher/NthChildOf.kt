package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun NthChildOf(parentMatcher: Matcher<View?>, childPosition: Int): Matcher<View?> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("position $childPosition of parent ")
            parentMatcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            if (view.parent !is ViewGroup) return false
            val parent = view.parent as ViewGroup
            return (
                parentMatcher.matches(parent) &&
                    parent.childCount > childPosition && parent.getChildAt(childPosition) == view
                )
        }
    }
}

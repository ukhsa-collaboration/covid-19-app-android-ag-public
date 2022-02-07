package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.positional

import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class IsNthChildOfMatcher(private val parentMatcher: Matcher<View>, private val childPosition: Int) :
    TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("position $childPosition of parent ")
        parentMatcher.describeTo(description)
    }

    override fun matchesSafely(view: View): Boolean {
        if (view.parent !is ViewGroup) return false
        val parent = view.parent as ViewGroup
        return parentMatcher.matches(parent) &&
                parent.childCount > childPosition &&
                parent.getChildAt(childPosition) == view
    }
}

fun isNthChildOf(parentMatcher: Matcher<View>, childPosition: Int): Matcher<View> {
    return IsNthChildOfMatcher(parentMatcher, childPosition)
}

package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.positional

import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class IsPrecededByMatcher(
    private val previousViewId: Int
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("is preceded by a widget with id = $previousViewId")
    }

    override fun matchesSafely(view: View): Boolean {
        if (view.parent !is ViewGroup) return false
        val parent = view.parent as ViewGroup
        val targetViewIndex = parent.indexOfChild(view)
        val previousView = parent.findViewById<View>(previousViewId)
        val previousViewIndex = targetViewIndex - 1

        return previousViewIndex >= 0 &&
                parent.childCount > targetViewIndex &&
                parent.getChildAt(previousViewIndex) == previousView
    }
}

fun isPrecededBy(previousViewId: Int): Matcher<View> {
    return IsPrecededByMatcher(previousViewId)
}

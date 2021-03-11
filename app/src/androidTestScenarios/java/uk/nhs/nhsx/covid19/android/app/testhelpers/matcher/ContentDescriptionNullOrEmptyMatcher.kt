package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class ContentDescriptionNullOrEmptyMatcher() :
    TypeSafeMatcher<View?>(View::class.java) {
    var resourceName: String? = null

    protected override fun matchesSafely(target: View?): Boolean {
        require(target != null) { "Cannot match a null view" }

        return target.contentDescription.isNullOrEmpty()
    }

    override fun describeTo(description: Description) {
        description.appendText("with null or empty content description ")
        if (resourceName != null) {
            description.appendText("[")
            description.appendText(resourceName)
            description.appendText("]")
        }
    }
}

fun withNullOrEmptyContentDescription(): Matcher<View?> {
    return ContentDescriptionNullOrEmptyMatcher()
}

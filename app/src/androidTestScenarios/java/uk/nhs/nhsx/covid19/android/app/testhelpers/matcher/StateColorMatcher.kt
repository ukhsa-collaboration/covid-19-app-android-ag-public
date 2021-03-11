package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.IntegerRes
import androidx.core.content.ContextCompat
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.widgets.StateInfoView

class StateColorMatcher(@field:ColorRes @param:IntegerRes private val expectedId: Int) :
    TypeSafeMatcher<View?>(View::class.java) {
    var resourceName: String? = null

    protected override fun matchesSafely(target: View?): Boolean {
        require(target != null) { "Cannot match a null view" }

        if (target !is StateInfoView) {
            return false
        }

        return target.stateColor == ContextCompat.getColor(target.context, expectedId)
    }

    override fun describeTo(description: Description) {
        description.appendText("with stateColor from resource id: ")
        description.appendValue(expectedId)
        if (resourceName != null) {
            description.appendText("[")
            description.appendText(resourceName)
            description.appendText("]")
        }
    }
}

fun withStateColor(@ColorRes resourceId: Int): Matcher<View?> {
    return StateColorMatcher(resourceId)
}

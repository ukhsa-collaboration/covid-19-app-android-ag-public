package uk.nhs.nhsx.covid19.android.app.testhelpers.matcher

import android.view.View
import androidx.annotation.StringRes
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.widgets.StateInfoView

class StateTextMatcher(@field:StringRes @param:StringRes private val expectedId: Int) :
    TypeSafeMatcher<View?>(View::class.java) {
    var resourceName: String? = null

    protected override fun matchesSafely(target: View?): Boolean {
        require(target != null) { "Cannot match a null view" }

        return target is StateInfoView && target.stateText == target.context.resources.getString(expectedId)
    }

    override fun describeTo(description: Description) {
        description.appendText("with stateText from resource id: ")
        description.appendValue(expectedId)
        if (resourceName != null) {
            description.appendText("[")
            description.appendText(resourceName)
            description.appendText("]")
        }
    }
}

fun withStateStringResource(@StringRes resourceId: Int): Matcher<View?> {
    return StateTextMatcher(resourceId)
}

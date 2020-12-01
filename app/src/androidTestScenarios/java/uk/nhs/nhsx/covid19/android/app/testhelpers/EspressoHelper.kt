package uk.nhs.nhsx.covid19.android.app.testhelpers

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff.Mode
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation

fun getCurrentActivity(): Activity? {
    getInstrumentation().waitForIdleSync()
    var currentActivity: Activity? = null
    getInstrumentation().runOnMainSync {
        run {
            currentActivity =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                    Stage.RESUMED
                ).elementAtOrNull(0)
        }
    }
    return currentActivity
}

fun assertBrowserIsOpened(url: String, block: () -> Unit) {
    Intents.init()
    val expectedIntent = CoreMatchers.allOf(
        IntentMatchers.hasAction(Intent.ACTION_VIEW),
        IntentMatchers.hasData(url)
    )
    Intents.intending(expectedIntent).respondWith(Instrumentation.ActivityResult(0, null))

    block()
    try {
        Intents.intended(expectedIntent)
    } finally {
        Intents.release()
    }
}

class NestedScrollViewScrollToAction(
    private val original: ScrollToAction = ScrollToAction()
) : ViewAction by original {

    override fun getConstraints(): Matcher<View> = CoreMatchers.anyOf(
        CoreMatchers.allOf(
            ViewMatchers.withEffectiveVisibility(VISIBLE),
            ViewMatchers.isDescendantOfA(ViewMatchers.isAssignableFrom(NestedScrollView::class.java))
        ),
        original.constraints
    )
}

fun ViewInteraction.isDisplayed(): Boolean =
    runCatching {
        check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        true
    }.getOrElse { false }

fun withDrawable(@DrawableRes id: Int, @ColorRes tint: Int? = null, tintMode: Mode = Mode.SRC_IN) =
    object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("ImageView with drawable same as drawable with id $id")
            tint?.let { description.appendText(", tint color id: $tint, mode: $tintMode") }
        }

        override fun matchesSafely(view: View): Boolean {
            val context = view.context
            val tintColor = tint?.toColor(context)
            val expectedBitmap = context.getDrawable(id)?.tinted(tintColor, tintMode)?.toBitmap()

            return view is ImageView && view.drawable.toBitmap().sameAs(expectedBitmap)
        }
    }

private fun Int.toColor(context: Context) = ContextCompat.getColor(context, this)

private fun Drawable.tinted(
    @ColorInt tintColor: Int? = null,
    tintMode: Mode = Mode.SRC_IN
) =
    apply {
        setTintList(tintColor?.toColorStateList())
        setTintMode(tintMode)
    }

private fun Int.toColorStateList() = ColorStateList.valueOf(this)

fun withViewAtPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?>? {
    return object : BoundedMatcher<View?, RecyclerView?>(RecyclerView::class.java) {
        override fun describeTo(description: Description?) {
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(recyclerView: RecyclerView?): Boolean {
            val viewHolder: RecyclerView.ViewHolder? =
                recyclerView?.findViewHolderForAdapterPosition(position)
            return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
        }
    }
}

fun clickChildViewWithId(id: Int): ViewAction? {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View>? {
            return null
        }

        override fun getDescription(): String {
            return "Click on a child view with specified id."
        }

        override fun perform(uiController: UiController?, view: View) {
            val v = view.findViewById<View>(id)
            v.performClick()
        }
    }
}

fun setScreenOrientation(orientation: Orientation) {
    val device = UiDevice.getInstance(getInstrumentation())
    when (orientation) {
        Orientation.LANDSCAPE -> device.setOrientationLeft()
        Orientation.PORTRAIT -> device.setOrientationNatural()
    }
}

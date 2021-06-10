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
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.EspressoException
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
import org.awaitility.kotlin.await
import org.awaitility.kotlin.ignoreExceptionsMatching
import org.awaitility.kotlin.untilAsserted
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.report.isRunningScreenshotCapture
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals

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

fun assertBrowserIsOpened(url: String, action: () -> Unit) {
    runWithIntents {
        val expectedIntent = CoreMatchers.allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasData(url)
        )
        Intents.intending(expectedIntent).respondWith(Instrumentation.ActivityResult(0, null))

        action()
        Intents.intended(expectedIntent)
    }
}

fun runWithIntents(action: () -> Unit) {
    Intents.init()
    try {
        action()
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

fun compose(vararg actions: ViewAction): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> =
            allOf(actions.map { it.constraints })

        override fun getDescription(): String =
            actions.joinToString(separator = " and ")

        override fun perform(
            uiController: UiController,
            view: View
        ) {
            actions.forEach { it.perform(uiController, view) }
        }
    }
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

fun withViewAtPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
    return object : BoundedMatcher<View?, RecyclerView?>(RecyclerView::class.java) {
        override fun describeTo(description: Description?) {
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(recyclerView: RecyclerView?): Boolean {
            val viewHolder: ViewHolder? =
                recyclerView?.findViewHolderForAdapterPosition(position)
            return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
        }
    }
}

fun clickChildViewWithId(id: Int): ViewAction {
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
        LANDSCAPE -> device.setOrientationLeft()
        PORTRAIT -> device.setOrientationNatural()
    }
    waitForOrientationChangeCompleted(orientation, device)
}

private fun waitForOrientationChangeCompleted(
    orientation: Orientation,
    device: UiDevice
) {
    // Causing some problems with DoReTo tool. TODO: investigate the problem
    if (isRunningScreenshotCapture()) {
        return
    }

    await.atMost(
        10, SECONDS
    ) ignoreExceptionsMatching {
        it is AssertionError
    } untilAsserted {
        val shouldBeNaturalOrientation = orientation == PORTRAIT
        assertEquals(shouldBeNaturalOrientation, device.isNaturalOrientation)
    }
}

fun allRecyclerViewItemsMatch(matcher: Matcher<View>): Matcher<View> {
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {

        override fun describeTo(description: Description?) {
            description?.appendText("all items match: ")
            matcher.describeTo(description)
        }

        override fun matchesSafely(recyclerView: RecyclerView?): Boolean {
            if (recyclerView == null) {
                return true
            }
            val itemCount = recyclerView.adapter?.itemCount ?: 0
            for (itemPosition in 0 until itemCount) {
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(itemPosition)
                val matches = matcher.matches(viewHolder?.itemView)
                if (!matches) {
                    return false
                }
            }
            return true
        }
    }
}

/**
 * Returns a matcher that applies [itemMatcher] to each item a the recycler view and:
 * <ul>
 *     <li>If [itemMatcher] returns true, applies [matcherIfTrue] to the item</li>
 *     <li>If [itemMatcher] returns false, applies [matcherIfFalse] to the item</li>
 * </ul>
 */
fun recyclerViewItemDiscriminatorMatcher(
    itemMatcher: Matcher<View>,
    matcherIfTrue: Matcher<View>,
    matcherIfFalse: Matcher<View>
): Matcher<View> {
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {

        override fun describeTo(description: Description?) {
            description?.let { desc ->
                desc.appendText("items matching: ")
                itemMatcher.describeTo(desc)
                desc.appendText(" match: ")
                matcherIfTrue.describeTo(desc)
                desc.appendText(" and all others match: ")
                matcherIfFalse.describeTo(desc)
            }
        }

        override fun matchesSafely(recyclerView: RecyclerView?): Boolean {
            if (recyclerView == null) {
                return false
            }
            val itemCount = recyclerView.adapter?.itemCount ?: 0
            for (itemPosition in 0 until itemCount) {
                recyclerView.findViewHolderForAdapterPosition(itemPosition)?.let { viewHolder ->
                    val matcher = if (itemMatcher.matches(viewHolder.itemView)) matcherIfTrue else matcherIfFalse
                    if (!matcher.matches(viewHolder.itemView)) {
                        return false
                    }
                }
            }
            return true
        }
    }
}

fun waitFor(idleTime: Long = AWAIT_AT_MOST_SECONDS, assertion: () -> Unit) {
    await.atMost(
        idleTime, SECONDS
    ) ignoreExceptionsMatching {
        it is EspressoException
    } untilAsserted assertion
}

package uk.nhs.nhsx.covid19.android.app.settings.animations

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AnimationsRobot
import kotlin.test.assertTrue

class AnimationsActivityTest : EspressoTest() {

    private val animationsRobot = AnimationsRobot()

    @Test
    fun whenSystemAnimationDisabled_enableAnimations_shouldNotTurnOnAndShowDialog() {
        testAppContext.setAnimations(isEnabled = false)

        startTestActivity<AnimationsActivity>()

        animationsRobot.checkActivityIsDisplayed()

        animationsRobot.clickToggle()

        waitFor { animationsRobot.checkAnimationDialogIsDisplayed() }

        animationsRobot.clickDialogOkay()

        animationsRobot.checkAnimationsAreDisabled()
    }

    @Test
    fun whenBackPressed_shouldGoToSettingsActivity() {
        val activity = startTestActivity<AnimationsActivity>()

        animationsRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}

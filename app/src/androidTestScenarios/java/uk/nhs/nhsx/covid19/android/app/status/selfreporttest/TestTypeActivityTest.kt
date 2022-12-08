package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class TestTypeActivityTest : EspressoTest() {

    private val testTypeRobot = TestTypeRobot()

    @Test
    fun showErrorStateWhenNoTestTypeIsSelectedAndContinueIsClicked() {
        startTestActivity<TestTypeActivity>()

        testTypeRobot.checkActivityIsDisplayed()

        testTypeRobot.checkNothingIsSelected()
        testTypeRobot.checkErrorIsVisible(false)

        testTypeRobot.clickContinueButton()

        testTypeRobot.checkErrorIsVisible(true)
    }

    @Test
    fun testTypeSelected_choiceSurvivesRotation() {
        startTestActivity<TestTypeActivity>()

        testTypeRobot.checkActivityIsDisplayed()

        testTypeRobot.checkNothingIsSelected()
        testTypeRobot.clickPositiveButton()

        waitFor { testTypeRobot.checkPositiveIsSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { testTypeRobot.checkPositiveIsSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { testTypeRobot.checkPositiveIsSelected() }
    }

    @Test
    fun testTypeSelected_canChangeSelection() {
        startTestActivity<TestTypeActivity>()

        testTypeRobot.checkActivityIsDisplayed()

        testTypeRobot.checkNothingIsSelected()
        testTypeRobot.clickVoidButton()
        waitFor { testTypeRobot.checkVoidIsSelected() }

        testTypeRobot.clickNegativeButton()
        waitFor { testTypeRobot.checkNegativeIsSelected() }

        testTypeRobot.clickPositiveButton()
        waitFor { testTypeRobot.checkPositiveIsSelected() }
    }
}

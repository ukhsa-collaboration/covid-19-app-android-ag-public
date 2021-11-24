package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import kotlin.test.assertTrue

class ExposureNotificationActivityTest : EspressoTest(), IsolationSetupHelper {
    private val exposureNotificationRobot = ExposureNotificationRobot()
    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenHasContactAndIndexIsolation_displayEncounterDateAndHideTestingAndIsolationAdvice() {
        givenSelfAssessmentAndContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
        checkDateIsDisplayed()
        exposureNotificationRobot.checkTestingAndIsolationAdviceIsDisplayed(displayed = false)
    }

    @Test
    fun whenHasContactIsolation_andNotInActiveIndexCaseIsolation_displayEncounterDateAndShowTestingAndIsolationAdvice() {
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
        checkDateIsDisplayed()
        exposureNotificationRobot.checkTestingAndIsolationAdviceIsDisplayed(displayed = true)
    }

    @Test
    fun whenDoesNotHaveContactIsolation_ActivityIsFinished() {
        val activity = startTestActivity<ExposureNotificationActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    private fun checkDateIsDisplayed() {
        val encounterDate = testAppContext.getIsolationStateMachine().readState().contact?.exposureDate
        val encounterDateText = encounterDate?.uiLongFormat(testAppContext.app) ?: ""
        waitFor { exposureNotificationRobot.checkEncounterDateIsDisplayed(encounterDateText) }
    }
}

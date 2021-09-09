package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import java.time.LocalDate
import kotlin.test.assertTrue

class ExposureNotificationAgeLimitActivityTest : EspressoTest(), IsolationSetupHelper, LocalAuthoritySetupHelper {

    private val ageLimitRobot = ExposureNotificationAgeLimitRobot()
    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenLocalAuthorityIsInEnglandAndUserClicksContinue_errorIsDisplayed() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        checkDate()

        ageLimitRobot.checkErrorVisible(false)

        ageLimitRobot.checkNothingSelected()
        ageLimitRobot.clickContinueButton()

        waitFor { ageLimitRobot.checkErrorVisible(true) }
    }

    @Test
    fun whenLocalAuthorityIsInEnglandAndUserHasMadeSelection_canChange_survivesRotation() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        checkDate()

        ageLimitRobot.checkNothingSelected()

        ageLimitRobot.clickYesButton()
        waitFor { ageLimitRobot.checkYesSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { ageLimitRobot.checkYesSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { ageLimitRobot.checkYesSelected() }
    }

    @Test
    fun whenLocalAuthorityIsInWales_ageLimitBoundaryIsEncounterDate() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()
        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        checkDate(isEngland = false)
    }

    @Test
    fun whenUserDoesNotHaveContactIsolation_activityIsFinished() {
        val activity = startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun whenContactIsolationOnly_subtitleIsDisplayed() {
        givenContactIsolation()
        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.checkSubtitleDisplayed(displayed = true)
    }

    @Test
    fun whenSelfAssessmentAndContactIsolation_subtitleIsNotDisplayed() {
        givenSelfAssessmentAndContactIsolation()
        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.checkSubtitleDisplayed(displayed = false)
    }

    private fun checkDate(isEngland: Boolean = true) {
        // givenContactIsolation() has a 2 day offset
        val contactCaseOffSet = 2
        val encounterDate = LocalDate.now(testAppContext.clock).minusDays(contactCaseOffSet.toLong())

        val expectedDate = if (isEngland) {
            encounterDate.minusDays(183L)
        } else {
            encounterDate
        }
        val expectedDateString = expectedDate.uiLongFormat(testAppContext.app)
        waitFor { ageLimitRobot.checkDateLabel(expectedDateString) }
    }
}

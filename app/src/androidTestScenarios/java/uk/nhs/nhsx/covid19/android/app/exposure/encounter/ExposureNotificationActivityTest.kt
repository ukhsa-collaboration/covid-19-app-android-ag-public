package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import kotlin.test.assertTrue

class ExposureNotificationActivityTest : EspressoTest(), IsolationSetupHelper, LocalAuthoritySetupHelper {
    private val exposureNotificationRobot = ExposureNotificationRobot()
    private val exposureNotificationAgeLimitRobot = ExposureNotificationAgeLimitRobot()
    private val riskyContactIsolationOptOutRobot = RiskyContactIsolationOptOutRobot()
    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenInWalesAndHasContactAndIndexIsolation_displayEncounterDateAndShowTestingAndIsolationAdvice() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()
            givenSelfAssessmentAndContactIsolation()

            startTestActivity<ExposureNotificationActivity>()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
            checkDateIsDisplayed()
            exposureNotificationRobot.checkIsolationAdviceIsDisplayed(displayed = true, WALES)
            exposureNotificationRobot.checkTestingAdviceIsDisplayed(displayed = false)
        }
    }

    @Test
    fun whenInWalesAndHasContactIsolation_andNotInActiveIndexCaseIsolation_displayEncounterDateAndShowTestingAndIsolationAdvice() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation()

            startTestActivity<ExposureNotificationActivity>()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
            checkDateIsDisplayed()
            exposureNotificationRobot.checkIsolationAdviceIsDisplayed(displayed = true, WALES)
            exposureNotificationRobot.checkTestingAdviceIsDisplayed(displayed = false)
        }
    }

    @Test
    fun whenInEnglandAndHasContactIsolation_andNotInActiveIndexCaseIsolation_displayEncounterDateAndHideTestingAdvice() {
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()

            startTestActivity<ExposureNotificationActivity>()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
            checkDateIsDisplayed()
            exposureNotificationRobot.checkIsolationAdviceIsDisplayed(displayed = true, ENGLAND)
            exposureNotificationRobot.checkTestingAdviceIsDisplayed(displayed = false)
        }
    }

    @Test
    fun whenInWalesAndHasContactIsolation_displayWalesStringIds() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation()

            startTestActivity<ExposureNotificationActivity>()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
            exposureNotificationRobot.checkWalesStringAreDisplayed()
        }
    }

    @Test
    fun whenContinueButtonIsClicked_showExposureNotificationAgeLimitActivityForWales() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation()

            startTestActivity<ExposureNotificationActivity>()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }

            exposureNotificationRobot.clickContinueButton()

            waitFor { riskyContactIsolationOptOutRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun whenContinueButtonIsClicked_showRiskyContactIsolationOptOutActivityForEngland() {
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()

            startTestActivity<ExposureNotificationActivity>()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }

            exposureNotificationRobot.clickContinueButton()

            waitFor { riskyContactIsolationOptOutRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun whenDoesNotHaveContactIsolation_ActivityIsFinished() {
        givenLocalAuthorityIsInWales()
        val activity = startTestActivity<ExposureNotificationActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    private fun checkDateIsDisplayed() {
        val encounterDate = testAppContext.getIsolationStateMachine().readState().contact?.exposureDate
        val encounterDateText = encounterDate?.uiLongFormat(testAppContext.app) ?: ""
        waitFor { exposureNotificationRobot.checkEncounterDateIsDisplayed(encounterDateText) }
    }

    @Test
    fun whenInWalesAndHasContactIsolation_andNotInActiveIndexCaseIsolation_whileNewContactJourneyFeatureDisabled_showExposureNotificationAgeLimitActivity() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation()

            startTestActivity<ExposureNotificationActivity>()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }

            exposureNotificationRobot.clickContinueButton()

            waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }
        }
    }
}

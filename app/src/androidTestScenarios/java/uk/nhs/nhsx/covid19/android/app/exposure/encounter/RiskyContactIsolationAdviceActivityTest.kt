package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.Companion.OPT_OUT_OF_CONTACT_ISOLATION_EXTRA
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MINOR
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.NONE
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskyContactIsolationAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper

class RiskyContactIsolationAdviceActivityTest : EspressoTest(), IsolationSetupHelper {

    val robot = RiskyContactIsolationAdviceRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun startRiskyContactIsolationAdviceAsMinor() {
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MINOR)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsMinorViewState()
    }

    @Test
    fun startRiskyContactIsolationAdviceAsFullyVaccinated() {
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, FULLY_VACCINATED)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsFullyVaccinatedViewState()
    }

    @Test
    fun startRiskyContactIsolationAdviceAsMedicallyExempt() {
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MEDICALLY_EXEMPT)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsMedicallyExemptViewState()
    }

    @Test
    fun whenUserWasAlreadyInActiveIndexCaseIsolation_thenDisplayActivityWithAlreadyIsolatingViewState() {
        givenSelfAssessmentAndContactIsolation()

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInAlreadyIsolatingViewState(remainingDaysInIsolation = 9)
    }

    @Test
    fun whenUserIsNotInActiveIndexCaseIsolation_thenDisplayActivityWithNewlyIsolatingViewState() {
        givenContactIsolation()

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNewlyIsolatingViewState(remainingDaysInIsolation = 9)
    }
}

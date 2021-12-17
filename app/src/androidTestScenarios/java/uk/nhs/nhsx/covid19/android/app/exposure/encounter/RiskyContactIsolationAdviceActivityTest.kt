package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.Companion.OPT_OUT_OF_CONTACT_ISOLATION_EXTRA
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MINOR
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.NONE
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskyContactIsolationAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import java.time.LocalDate

class RiskyContactIsolationAdviceActivityTest : EspressoTest(), IsolationSetupHelper, LocalAuthoritySetupHelper {

    private val robot = RiskyContactIsolationAdviceRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    // region Minor
    @Test
    fun startRiskyContactIsolationAdviceAsMinorAsEnglishUser_thenDisplayActivityWithMinorViewState() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MINOR)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsMinorViewState(country = ENGLAND, testingAdviceToShow = Default)
    }

    @Test
    fun startRiskyContactIsolationAdviceAsMinor_asWelshUser_6DaysAfterExposure_thenDisplayMinor_withDefaultTestAdvice() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation(exposureDaysAgo = 6)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MINOR)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsMinorViewState(country = WALES, testingAdviceToShow = Default)
    }

    @Test
    fun startRiskyContactIsolationAdviceAsMinor_asWelshUser_5DaysAfterExposure_thenDisplayMinor_withTestAdviceWithDate() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation(exposureDaysAgo = 5)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MINOR)
        }

        val pcrAdviceDate = LocalDate.now(testAppContext.clock).plusDays(3)

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsMinorViewState(country = WALES, testingAdviceToShow = WalesWithinAdviceWindow(date = pcrAdviceDate))
    }
    // endregion

    // region FullyVaccinated
    @Test
    fun startRiskyContactIsolationAdviceAsFullyVaccinatedAsEnglishUser_thenDisplayActivityWithFullyVaccinatedViewState() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, FULLY_VACCINATED)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsFullyVaccinatedViewState(country = ENGLAND, testingAdviceToShow = Default)
    }

    @Test
    fun startRiskyContactIsolationAdviceAsFullyVaccinated_asWelshUser_6DaysAfterExposure_thenDisplayFullyVaccinated_withDefaultTestAdvice() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation(exposureDaysAgo = 6)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, FULLY_VACCINATED)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsFullyVaccinatedViewState(country = WALES, testingAdviceToShow = Default)
    }

    @Test
    fun startRiskyContactIsolationAdviceAsFullyVaccinated_asWelshUser_5DaysAfterExposure_thenDisplayFullyVaccinated_withTestAdviceWithDate() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation(exposureDaysAgo = 5)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, FULLY_VACCINATED)
        }

        val pcrAdviceDate = LocalDate.now(testAppContext.clock).plusDays(3)

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsFullyVaccinatedViewState(country = WALES, testingAdviceToShow = WalesWithinAdviceWindow(date = pcrAdviceDate))
    }
    // endregion

    // region MedicallyExempt
    @Test
    fun startRiskyContactIsolationAdviceAsMedicallyExempt() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MEDICALLY_EXEMPT)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsMedicallyExemptViewState()
    }
    // endregion

    // region AlreadyIsolating
    @Test
    fun whenUserWasAlreadyInActiveIndexCaseIsolationAsEnglishUser_thenDisplayActivityWithAlreadyIsolatingViewState() {
        givenLocalAuthorityIsInEngland()
        givenSelfAssessmentAndContactIsolation()

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInAlreadyIsolatingViewState(remainingDaysInIsolation = 9, testingAdviceToShow = Default)
    }

    @Test
    fun whenUserWasAlreadyInActiveIndexCaseIsolation_asWelshUser_6DaysAfterExposure_thenDisplayAlreadyIsolating_withDefaultTestAdvice() {
        givenLocalAuthorityIsInWales()
        givenSelfAssessmentAndContactIsolation(exposureDaysAgo = 6)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInAlreadyIsolatingViewState(
            remainingDaysInIsolation = 7,
            testingAdviceToShow = Default
        )
    }

    @Test
    fun whenUserWasAlreadyInActiveIndexCaseIsolation_asWelshUser_5DaysAfterExposure_thenDisplayAlreadyIsolating_withTestAdviceWithDate() {
        givenLocalAuthorityIsInWales()
        givenSelfAssessmentAndContactIsolation(exposureDaysAgo = 5)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        val pcrAdviceDate = LocalDate.now(testAppContext.clock).plusDays(3)

        robot.checkActivityIsDisplayed()
        robot.checkIsInAlreadyIsolatingViewState(
            remainingDaysInIsolation = 7,
            testingAdviceToShow = WalesWithinAdviceWindow(date = pcrAdviceDate)
        )
    }
    // endregion

    // region NewlyIsolating
    @Test
    fun whenUserIsNotInActiveIndexCaseIsolationAsEnglishUser_thenDisplayActivityWithNewlyIsolatingViewState() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNewlyIsolatingViewState(remainingDaysInIsolation = 9, testingAdviceToShow = Default)
    }

    @Test
    fun whenUserIsNotInActiveIndexCaseIsolation_asWelshUser_6DaysAfterExposure_thenDisplayNewlyIsolating_withDefaultTestAdvice() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation(exposureDaysAgo = 6)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNewlyIsolatingViewState(
            remainingDaysInIsolation = 5,
            testingAdviceToShow = Default
        )
    }

    @Test
    fun whenUserIsNotInActiveIndexCaseIsolation_asWelshUser_5DaysAfterExposure_thenDisplayNewlyIsolating_withTestAdviceWithDate() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation(exposureDaysAgo = 5)

        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, NONE)
        }

        val pcrAdviceDate = LocalDate.now(testAppContext.clock).plusDays(3)

        robot.checkActivityIsDisplayed()
        robot.checkIsInNewlyIsolatingViewState(
            remainingDaysInIsolation = 6,
            testingAdviceToShow = WalesWithinAdviceWindow(date = pcrAdviceDate)
        )
    }
    // endregion

    @Test
    fun toolbarNavigationIconIsCloseIcon() {
        givenContactIsolation()
        startTestActivity<RiskyContactIsolationAdviceActivity>()
        robot.verifyCloseButton()
    }
}

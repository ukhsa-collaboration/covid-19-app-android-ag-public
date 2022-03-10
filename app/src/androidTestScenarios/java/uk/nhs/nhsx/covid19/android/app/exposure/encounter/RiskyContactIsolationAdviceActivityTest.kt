package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.Companion.OPT_OUT_OF_CONTACT_ISOLATION_EXTRA
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.MINOR
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationAdviceActivity.OptOutOfContactIsolationExtra.NONE
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskyContactIsolationAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import java.time.LocalDate

@RunWith(Parameterized::class)
class RiskyContactIsolationAdviceActivityTest(override val configuration: TestConfiguration) : EspressoTest(),
    IsolationSetupHelper, LocalAuthoritySetupHelper {

    private val robot = RiskyContactIsolationAdviceRobot()
    private val statusRobot = StatusRobot()
    private val testOrderingRobot = TestOrderingRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    private val isolationConfiguration = IsolationConfiguration(
        contactCase = 11,
        indexCaseSinceSelfDiagnosisOnset = 11,
        indexCaseSinceSelfDiagnosisUnknownOnset = 9,
        maxIsolation = 21,
        indexCaseSinceTestResultEndDate = 11,
        pendingTasksRetentionPeriod = 14,
        testResultPollingTokenRetentionPeriod = 28
    )

    @Before
    fun setupConfiguration() {
        testAppContext.mockIsolationConfigurationApi.setIsolationConfigurationForAnalytics(isolationConfiguration)
    }

    // region Minor
    @Test
    @Reported
    fun startRiskyContactIsolationAdviceAsMinorAsEnglishUser_thenDisplayActivityWithMinorViewState() = reporter(
        scenario = "Risky contact isolation advice",
        title = "Risky contact isolation advice for minor - England",
        description = "Risky contact isolation advice for minor - England",
        kind = Reporter.Kind.SCREEN
    ) {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MINOR)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsNotIsolatingAsMinorViewState(country = ENGLAND, testingAdviceToShow = Default)
        step("Minor in England", "Shows isolation advice for minor in England")
    }

    @Test
    @Reported
    fun startRiskyContactIsolationAdviceAsMinor_asWelshUser_6DaysAfterExposure_thenDisplayMinor_withDefaultTestAdvice() =
        reporter(
            scenario = "Risky contact isolation advice",
            title = "Risky contact isolation advice for minor - Wales",
            description = "Risky contact isolation advice for minor after 6 days exposure - Wales",
            kind = Reporter.Kind.SCREEN
        ) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation(exposureDaysAgo = 6)

            startTestActivity<RiskyContactIsolationAdviceActivity> {
                putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MINOR)
            }

            robot.checkActivityIsDisplayed()
            robot.checkIsNotIsolatingAsMinorViewState(country = WALES, testingAdviceToShow = Default)
            step("Minor in Wales", "Shows isolation advice for minor in Wales")
        }

    @Test
    @Reported
    fun startRiskyContactIsolationAdviceAsMinor_asWelshUser_5DaysAfterExposure_thenDisplayMinor_withTestAdviceWithDate() =
        reporter(
            scenario = "Risky contact isolation advice",
            title = "Risky contact isolation advice for minor - Wales",
            description = "Risky contact isolation advice for minor after 5 days exposure - Wales",
            kind = Reporter.Kind.SCREEN
        ) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation(exposureDaysAgo = 5)

            startTestActivity<RiskyContactIsolationAdviceActivity> {
                putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MINOR)
            }

            val pcrAdviceDate = LocalDate.now(testAppContext.clock).plusDays(3)

            robot.checkActivityIsDisplayed()
            robot.checkIsNotIsolatingAsMinorViewState(
                country = WALES,
                testingAdviceToShow = WalesWithinAdviceWindow(date = pcrAdviceDate)
            )
            step("Minor in Wales", "Shows isolation advice for minor in Wales")
        }
    // endregion

    // region FullyVaccinated
    @Test
    @Reported
    fun startRiskyContactIsolationAdviceAsFullyVaccinatedAsEnglishUser_thenDisplayActivityWithFullyVaccinatedViewState() =
        reporter(
            scenario = "Risky contact isolation advice",
            title = "Risky contact isolation advice for fully vaccinated - England",
            description = "Risky contact isolation advice for fully vaccinated - England",
            kind = Reporter.Kind.SCREEN
        ) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()
            startTestActivity<RiskyContactIsolationAdviceActivity> {
                putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, FULLY_VACCINATED)
            }

            robot.checkActivityIsDisplayed()
            robot.checkIsInNotIsolatingAsFullyVaccinatedViewState(country = ENGLAND, testingAdviceToShow = Default)
            step("Fully vaccinated in England", "Shows isolation advice for fully vaccinated in England")
        }

    @Test
    @Reported
    fun startRiskyContactIsolationAdviceAsFullyVaccinated_asWelshUser_6DaysAfterExposure_thenDisplayFullyVaccinated_withDefaultTestAdvice() =
        reporter(
            scenario = "Risky contact isolation advice",
            title = "Risky contact isolation advice for fully vaccinated - Wales",
            description = "Risky contact isolation advice for fully vaccinated after 6 days exposure - Wales",
            kind = Reporter.Kind.SCREEN
        ) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation(exposureDaysAgo = 6)

            startTestActivity<RiskyContactIsolationAdviceActivity> {
                putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, FULLY_VACCINATED)
            }

            robot.checkActivityIsDisplayed()
            robot.checkIsInNotIsolatingAsFullyVaccinatedViewState(country = WALES, testingAdviceToShow = Default)
            step("Fully vaccinated in Wales", "Shows isolation advice for fully vaccinated in Wales")
        }

    @Test
    @Reported
    fun startRiskyContactIsolationAdviceAsFullyVaccinated_asWelshUser_5DaysAfterExposure_thenDisplayFullyVaccinated_withTestAdviceWithDate() =
        reporter(
            scenario = "Risky contact isolation advice",
            title = "Risky contact isolation advice for fully vaccinated - Wales",
            description = "Risky contact isolation advice for fully vaccinated after 5 days exposure - Wales",
            kind = Reporter.Kind.SCREEN
        ) {
            givenLocalAuthorityIsInWales()
            givenContactIsolation(exposureDaysAgo = 5)

            startTestActivity<RiskyContactIsolationAdviceActivity> {
                putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, FULLY_VACCINATED)
            }

            val pcrAdviceDate = LocalDate.now(testAppContext.clock).plusDays(3)

            robot.checkActivityIsDisplayed()
            robot.checkIsInNotIsolatingAsFullyVaccinatedViewState(
                country = WALES,
                testingAdviceToShow = WalesWithinAdviceWindow(date = pcrAdviceDate)
            )
            step("Fully vaccinated in Wales", "Shows isolation advice for fully vaccinated in Wales")
        }
    // endregion

    // region MedicallyExempt
    @Test
    @Reported
    fun startRiskyContactIsolationAdviceAsMedicallyExempt() = reporter(
        scenario = "Risky contact isolation advice",
        title = "Risky contact isolation advice for medically exempt - England",
        description = "Risky contact isolation advice for medically exempt - England",
        kind = Reporter.Kind.SCREEN
    ) {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<RiskyContactIsolationAdviceActivity> {
            putExtra(OPT_OUT_OF_CONTACT_ISOLATION_EXTRA, MEDICALLY_EXEMPT)
        }

        robot.checkActivityIsDisplayed()
        robot.checkIsInNotIsolatingAsMedicallyExemptViewStateForEngland()
        step("Medically exempt in England", "Shows isolation advice for medically exempt in England")
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
        robot.checkIsInNewlyIsolatingViewState(ENGLAND, remainingDaysInIsolation = 9, testingAdviceToShow = Default)

        robot.clickPrimaryButton()
        testOrderingRobot.checkActivityIsDisplayed()
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
            WALES,
            remainingDaysInIsolation = 5,
            testingAdviceToShow = Default
        )

        assertBrowserIsOpened("https://gov.wales/get-rapid-lateral-flow-covid-19-tests-if-you-do-not-have-symptoms") {
            robot.clickPrimaryButton()
        }
        statusRobot.checkActivityIsDisplayed()
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
            WALES,
            remainingDaysInIsolation = 6,
            testingAdviceToShow = WalesWithinAdviceWindow(date = pcrAdviceDate)
        )

        assertBrowserIsOpened("https://gov.wales/get-rapid-lateral-flow-covid-19-tests-if-you-do-not-have-symptoms") {
            robot.clickPrimaryButton()
        }
        statusRobot.checkActivityIsDisplayed()
    }
    // endregion

    @Test
    fun toolbarNavigationIconIsCloseIcon() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<RiskyContactIsolationAdviceActivity>()
        robot.verifyCloseButton()
    }
}

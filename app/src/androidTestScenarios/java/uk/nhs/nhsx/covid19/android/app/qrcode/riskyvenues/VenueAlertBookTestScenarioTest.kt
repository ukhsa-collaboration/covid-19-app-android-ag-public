package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.questionnaire.NewGuidanceForSymptomaticCasesEnglandRobot
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.OrderLfdTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertBookTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import java.time.Instant

class VenueAlertBookTestScenarioTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val venueAlertBookTestRobot = VenueAlertBookTestRobot()
    private val symptomsAfterRiskyVenueVisitRobot = SymptomsAfterRiskyVenueRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()
    private val orderLfdTestRobot = OrderLfdTestRobot()
    private val browserRobot = BrowserRobot()
    private val guidanceForSymptomaticCasesEnglandRobot = NewGuidanceForSymptomaticCasesEnglandRobot()

    private val statusRobot = StatusRobot()
    private val orderTest = OrderTest(this)

    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Before
    fun setUp() {
        runBlocking {
            testAppContext.getVisitedVenuesStorage().setVisits(visits)
        }
    }

    @Test
    fun whenNotInActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_clickCancel_clickLeave_shouldShowStatusActivity() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }

        symptomsAfterRiskyVenueVisitRobot.clickCancelButton()
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }

        setScreenOrientation(LANDSCAPE)
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }
        setScreenOrientation(PORTRAIT)
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }

        symptomsAfterRiskyVenueVisitRobot.clickDialogStayButton()
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsNotDisplayed() }

        testAppContext.device.pressBack()
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }

        setScreenOrientation(LANDSCAPE)
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }
        setScreenOrientation(PORTRAIT)

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickDialogStayButton()
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsNotDisplayed() }

        setScreenOrientation(LANDSCAPE)

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsNotDisplayed() }

        symptomsAfterRiskyVenueVisitRobot.clickCancelButton()
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickDialogLeaveButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenInActiveIndexCaseIsolation_clickBookATest_shouldShowTestOrderingActivity() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { testOrderingRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenNotInActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_withScreenRotation_pressBack_canOpenCancelDialog() {
        setScreenOrientation(PORTRAIT)
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }

        setScreenOrientation(LANDSCAPE)
        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkCancelDialogIsDisplayed() }
    }

    @Test
    fun symptomaticSelfIsolationEnabled_notActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_hasSymptoms_shouldShowNewAdviceActivity_forWales() {
        givenLocalAuthorityIsInWales()
        testAppContext.questionnaireApi.isSymptomaticSelfIsolationForWalesEnabled = true
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()

        waitFor { questionnaireRobot.checkActivityIsDisplayed() }
        questionnaireRobot.selectSymptomsAtPositions(2)
        questionnaireRobot.reviewSymptoms()

        waitFor { reviewSymptomsRobot.checkActivityIsDisplayed() }
        reviewSymptomsRobot.selectCannotRememberDate()
        reviewSymptomsRobot.confirmSelection()

        waitFor { symptomsAdviceIsolateRobot.checkActivityIsDisplayed() }
        waitFor { symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenIsolationDueToSelfAssessment(testAppContext.getRemainingDaysInIsolation())
        ) }
        symptomsAdviceIsolateRobot.clickBottomActionButton()
    }

    @Test
    fun symptomaticSelfIsolationDisabled_notActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_hasSymptoms_shouldShowContinueWithIsolation_forWales() {
        givenLocalAuthorityIsInWales()
        testAppContext.questionnaireApi.isSymptomaticSelfIsolationForWalesEnabled = false
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()

        waitFor { questionnaireRobot.checkActivityIsDisplayed() }
        questionnaireRobot.selectSymptomsAtPositions(2)
        questionnaireRobot.reviewSymptoms()

        waitFor { reviewSymptomsRobot.checkActivityIsDisplayed() }
        reviewSymptomsRobot.confirmSelection()

        waitFor { symptomsAdviceIsolateRobot.checkActivityIsDisplayed() }
        waitFor { symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(testAppContext.getRemainingDaysInIsolation())
        ) }
        symptomsAdviceIsolateRobot.clickBottomActionButton()
    }

    @Test
    fun notActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_hasSymptoms_completeQuestionnaire_shouldShowNewAdvice_showStatusActivity_forEngland() {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()

        waitFor { questionnaireRobot.checkActivityIsDisplayed() }
        questionnaireRobot.selectSymptomsAtPositions(2)
        questionnaireRobot.reviewSymptoms()

        waitFor { reviewSymptomsRobot.checkActivityIsDisplayed() }
        reviewSymptomsRobot.selectCannotRememberDate()
        reviewSymptomsRobot.confirmSelection()

        waitFor { symptomsAdviceIsolateRobot.checkActivityIsDisplayed() }

        symptomsAdviceIsolateRobot.clickBottomActionButton()

        waitFor { guidanceForSymptomaticCasesEnglandRobot.checkActivityIsDisplayed() }

        guidanceForSymptomaticCasesEnglandRobot.clickPrimaryActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenNotInActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_clickHasSymptoms_completeQuestionnaire_shouldShowNewAdviceActivity_forEngland() {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()

        waitFor { questionnaireRobot.checkActivityIsDisplayed() }
        questionnaireRobot.selectSymptomsAtPositions(2)
        questionnaireRobot.reviewSymptoms()

        waitFor { reviewSymptomsRobot.checkActivityIsDisplayed() }
        reviewSymptomsRobot.selectCannotRememberDate()
        reviewSymptomsRobot.confirmSelection()

        waitFor { symptomsAdviceIsolateRobot.checkActivityIsDisplayed() }

        symptomsAdviceIsolateRobot.clickBottomActionButton()
    }

    @Test
    fun whenNotInActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_clickHasSymptoms_reportNoSymptomsInQuestionnaire_shouldShowStatusActivity() {
        testAppContext.setState(isolationHelper.contact().asIsolation())
        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()

        waitFor { questionnaireRobot.checkActivityIsDisplayed() }
        questionnaireRobot.clickNoSymptoms()

        waitFor { questionnaireRobot.discardSymptomsDialogIsDisplayed() }
        waitFor { questionnaireRobot.continueOnDiscardSymptomsDialog() }

        waitFor { symptomsAdviceIsolateRobot.checkActivityIsDisplayed() }

        symptomsAdviceIsolateRobot.clickBottomActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenNotInActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_clickNoSymptoms_orderLfdTest_shouldShowStatusActivity() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {

            testAppContext.setState(isolationHelper.neverInIsolation())

            startVenueAlertBookTestActivity()

            waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
            venueAlertBookTestRobot.clickBookTestButton()

            waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
            symptomsAfterRiskyVenueVisitRobot.clickNoSymptomsButton()

            waitFor { orderLfdTestRobot.checkActivityIsDisplayed() }
            orderLfdTestRobot.clickOrderTestButton()

            waitFor { browserRobot.checkActivityIsDisplayed() }

            testAppContext.device.pressBack()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun whenNotInActiveIndexCaseIsolation_navigateToSymptomsAfterRiskyVenue_clickNoSymptoms_clickAlreadyHaveLfdTestKit_shouldShowStatusActivity() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startVenueAlertBookTestActivity()

        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        venueAlertBookTestRobot.clickBookTestButton()

        waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
        symptomsAfterRiskyVenueVisitRobot.clickNoSymptomsButton()

        waitFor { orderLfdTestRobot.checkActivityIsDisplayed() }

        orderLfdTestRobot.clickIAlreadyHaveKitButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    private fun startVenueAlertBookTestActivity() =
        startTestActivity<VenueAlertBookTestActivity> {
            putExtra(VenueAlertBookTestActivity.EXTRA_VENUE_ID, RISKY_VENUE_ID)
        }

    private val visits = listOf(
        VenueVisit(
            venue = Venue(RISKY_VENUE_ID, "Venue1"),
            from = Instant.parse("2020-07-25T10:00:00Z"),
            to = Instant.parse("2020-07-25T12:00:00Z")
        )
    )

    companion object {
        private const val RISKY_VENUE_ID = "1"
    }
}

package uk.nhs.nhsx.covid19.android.app.status.testinghub

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultOnsetDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import java.time.LocalDate

class TestingHubScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val testingHubRobot = TestingHubRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val orderTest = OrderTest(this)
    private val questionnaireRobot = QuestionnaireRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestResultSymptomsRobot = LinkTestResultSymptomsRobot()
    private val linkTestResultOnsetDateRobot = LinkTestResultOnsetDateRobot()
    private val testResultRobot = TestResultRobot(InstrumentationRegistry.getInstrumentation().targetContext)
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private val symptomsAfterRiskyVenueRobot = SymptomsAfterRiskyVenueRobot()

    @Test
    fun activeIsolation_thenNavigateToBookATest_tapBack_shouldShowTestingHub() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        testOrderingRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        testingHubRobot.checkActivityIsDisplayed()
    }

    @Test
    fun activeIsolationFromSelfAssessment_thenNavigateToBookATest_bookATest_shouldShowStatusActivity() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        testOrderingRobot.checkActivityIsDisplayed()

        orderTest()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun activeIsolationAsContactCase_withRiskyVenueNotification_thenNavigateToSymptomsScreen_thenShouldShowBookATest_shouldShowStatusActivity() {
        testAppContext.setState(isolationHelper.contact().asIsolation())
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue =
            LastVisitedBookTestTypeVenueDate(
                latestDate = LocalDate.now(testAppContext.clock),
                riskyVenueConfigurationDurationDays = RiskyVenueConfigurationDurationDays()
            )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        symptomsAfterRiskyVenueRobot.checkActivityIsDisplayed()
        symptomsAfterRiskyVenueRobot.clickHasSymptomsButton()

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)
        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()
        reviewSymptomsRobot.selectCannotRememberDate()
        reviewSymptomsRobot.confirmSelection()

        symptomsAdviceIsolateRobot.clickBottomActionButton()

        orderTest()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun activeIsolationAsContactCase_withNoRiskyVenueNotification_thenNavigateToBookATest_shouldShowStatusActivity() {
        testAppContext.setState(isolationHelper.contact().asIsolation())
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        orderTest()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun navigateToEnterTestResultViaTestingHub_enterValidToken_receivedApiResult_shouldShowStatusActivity() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickEnterTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()
        // LFD token because we want to skip the symptom onset flow
        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_LFD_TOKEN_INDICATIVE_NO_KEY_SUBMISSION)
        linkTestResultRobot.clickContinue()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest()
        }
        testResultRobot.clickCloseButton()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun navigateToEnterTestResultViaTestingHub_requiresSymptomOnsetDate_completeFlow_shouldShowStatusActivity() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickEnterTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()
        // PCR token because we want to enter the symptom onset flow
        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN_NO_KEY_SUBMISSION)
        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }
        linkTestResultSymptomsRobot.clickYes()

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()
        linkTestResultOnsetDateRobot.clickSelectDate()
        val onsetDate = LocalDate.now(testAppContext.clock).minusDays(3)
        linkTestResultOnsetDateRobot.selectDayOfMonth(onsetDate.dayOfMonth)
        linkTestResultOnsetDateRobot.clickContinueButton()

        waitFor {
            testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation()
        }
        testResultRobot.clickIsolationActionButton()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun navigateToEnterTestResultViaTestingHub_receiveErrorFromApi_shouldShowError_tapBack_shouldShowTestingHub() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickEnterTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        // Force failure response and tap return to verify appropriate navigation behaviour
        testAppContext.virologyTestingApi.testResultForCtaTokenStatusCode = 500

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkValidationErrorUnexpectedIsDisplayed() }

        testAppContext.device.pressBack()

        testingHubRobot.checkActivityIsDisplayed()
    }

    @Test
    fun activeIsolationAsContactCase_withRiskyVenueNotification_thenNavigateToSymptomsScreen_thenClicksCancel_shouldShowTestingHub() {
        testAppContext.setState(isolationHelper.contact().asIsolation())
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue =
            LastVisitedBookTestTypeVenueDate(
                latestDate = LocalDate.now(testAppContext.clock),
                riskyVenueConfigurationDurationDays = RiskyVenueConfigurationDurationDays()
            )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        symptomsAfterRiskyVenueRobot.checkActivityIsDisplayed()
        symptomsAfterRiskyVenueRobot.clickCancelButton()

        testingHubRobot.checkActivityIsDisplayed()
    }
}

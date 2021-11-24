package uk.nhs.nhsx.covid19.android.app.status.testinghub

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.BookTestTypeVenueVisitSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class TestingHubActivityTest : EspressoTest(), IsolationSetupHelper, BookTestTypeVenueVisitSetupHelper,
    LocalAuthoritySetupHelper {
    private val testingHubRobot = TestingHubRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val symptomsAfterRiskyVenueRobot = SymptomsAfterRiskyVenueRobot()
    private val browserRobot = BrowserRobot()
    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenInContactIsolation_andNoBookTestTypeVenueVisitStored_clickOrderPcrTestButton_shouldNavigateToBookAPcrTest() {
        givenContactIsolation()
        givenNoBookTestTypeVenueVisitStored()

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookPcrTestIsDisplayed()

        testingHubRobot.clickBookTest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenInContactIsolation_andBookTestTypeVenueVisitStored_clickOrderPcrTestButton_shouldNavigateToSymptomsCheck() {
        givenContactIsolation()
        givenBookTestTypeVenueVisitStored()

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookPcrTestIsDisplayed()

        testingHubRobot.clickBookTest()

        symptomsAfterRiskyVenueRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenNotIsolating_andBookTestTypeVenueVisitStored_clickOrderPcrTestButton_shouldNavigateToSymptomsCheck() {
        givenNeverInIsolation()
        givenBookTestTypeVenueVisitStored()

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookPcrTestIsDisplayed()

        testingHubRobot.clickBookTest()

        symptomsAfterRiskyVenueRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenInSelfAssessmentIsolation_andNoBookTestTypeVenueVisitStored_clickOrderPcrTestButton_shouldNavigateToBookAPcrTest() {
        givenSelfAssessmentIsolation()
        givenNoBookTestTypeVenueVisitStored()

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookPcrTestIsDisplayed()

        testingHubRobot.clickBookTest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenInContactIsolation_andNoBookTestTypeVenueVisitStored_clickBookPcrTestButton_shouldNavigateToBookAPcrTest() {
        givenContactIsolation()
        givenNoBookTestTypeVenueVisitStored()

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookPcrTestIsDisplayed()

        testingHubRobot.clickBookTest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenNotIsolating_andNoBookTestTypeVenueVisitStored_clickBookLfdTestButton_shouldOpenWebsiteInExternalBrowser() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            givenNeverInIsolation()
            givenNoBookTestTypeVenueVisitStored()

            startTestActivity<TestingHubActivity>()

            testingHubRobot.checkActivityIsDisplayed()
            testingHubRobot.checkBookLfdTestIsDisplayed()

            testingHubRobot.clickBookTest()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }
}

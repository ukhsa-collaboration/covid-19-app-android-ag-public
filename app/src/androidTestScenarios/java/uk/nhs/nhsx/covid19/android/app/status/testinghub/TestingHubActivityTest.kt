package uk.nhs.nhsx.covid19.android.app.status.testinghub

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import java.time.LocalDate

class TestingHubActivityTest : EspressoTest() {

    private val testingHubRobot = TestingHubRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val symptomsAfterRiskyVenueRobot = SymptomsAfterRiskyVenueRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenInActiveIsolation_showBookTestButton_doNotShowFindOutAboutTesting() {
        testAppContext.setState(isolationHelper.contactCase().asIsolation())

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsDisplayed()
        testingHubRobot.checkFindOutAboutTestingIsNotDisplayed()
    }

    @Test
    fun whenNotIsolating_showFindOutAboutTesting_doNotShowBookTestButton() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsNotDisplayed()
        testingHubRobot.checkFindOutAboutTestingIsDisplayed()
    }

    @Test
    fun whenLastVisitedBookTestTypeVenueAtRisk_withNoActiveIsolation_orderTestButtonShouldBeDisplayed() {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue =
            LastVisitedBookTestTypeVenueDate(
                LocalDate.now(),
                RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
            )

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsDisplayed()
        testingHubRobot.checkFindOutAboutTestingIsDisplayed()
    }

    @Test
    fun whenLastVisitedBookTestTypeVenueNotAtRisk_withNoActiveIsolation_orderTestButtonShouldBeDisplayed() {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsNotDisplayed()
        testingHubRobot.checkFindOutAboutTestingIsDisplayed()
    }

    @Test
    fun whenLastVisitedBookTestTypeVenueAtRisk_withNoActiveIsolation_clickOrderTestButton_shouldNavigateToSymptomsCheck() {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue =
            LastVisitedBookTestTypeVenueDate(
                LocalDate.now(),
                RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
            )

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsDisplayed()

        testingHubRobot.clickBookTest()

        symptomsAfterRiskyVenueRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenLastVisitedVenueNotAtRisk_whenInActiveIsolationFromContactCase_clickOrderTestButton_shouldNavigateToBookTest() {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null
        testAppContext.setState(isolationHelper.contactCase().asIsolation())

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsDisplayed()

        testingHubRobot.clickBookTest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenInActiveIsolationFromSelfAssessment_clickOrderTestButton_shouldNavigateToBookTest() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsDisplayed()

        testingHubRobot.clickBookTest()

        testOrderingRobot.checkActivityIsDisplayed()
    }
}

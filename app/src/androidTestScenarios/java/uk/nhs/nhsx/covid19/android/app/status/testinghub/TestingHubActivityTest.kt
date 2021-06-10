package uk.nhs.nhsx.covid19.android.app.status.testinghub

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import java.time.LocalDate

class TestingHubActivityTest : EspressoTest() {

    private val testingHubRobot = TestingHubRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenInActiveIsolation_showBookTestButton_doNotShowFindOutAboutTesting() = notReported {
        testAppContext.setState(isolationHelper.contactCase().asIsolation())

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsDisplayed()
        testingHubRobot.checkFindOutAboutTestingIsNotDisplayed()
    }

    @Test
    fun whenNotIsolating_showFindOutAboutTesting_doNotShowBookTestButton() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsNotDisplayed()
        testingHubRobot.checkFindOutAboutTestingIsDisplayed()
    }

    @Test
    fun whenLastVisitedBookTestTypeVenueAtRisk_withNoActiveIsolation_orderTestButtonShouldBeDisplayed() = notReported {
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
    fun whenLastVisitedBookTestTypeVenueNotAtRisk_withNoActiveIsolation_orderTestButtonShouldBeDisplayed() = notReported {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null

        startTestActivity<TestingHubActivity>()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.checkBookTestIsNotDisplayed()
        testingHubRobot.checkFindOutAboutTestingIsDisplayed()
    }
}

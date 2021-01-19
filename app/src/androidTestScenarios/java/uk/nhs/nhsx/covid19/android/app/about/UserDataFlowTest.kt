package uk.nhs.nhsx.covid19.android.app.about

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UserDataRobot
import java.time.Instant
import java.time.ZoneOffset

class UserDataFlowTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val userDataRobot = UserDataRobot()

    @Test
    fun startContactCase_checkLastDayOfIsolationInStatusScreen_confirmLastDayOfIsolationInUserDataScreen() =
        notReported {
            testAppContext.clock.currentInstant = startDate
            testAppContext.setState(contactCaseIsolation)

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            waitFor { statusRobot.checkIsolationViewIsDisplayed() }

            val expectedExpiryDate = "24 Dec 2020"

            statusRobot.checkIsolationSubtitleIsDisplayedWithText(testAppContext.app, expectedExpiryDate)

            statusRobot.clickMoreAboutApp()

            moreAboutAppRobot.checkActivityIsDisplayed()

            waitFor { moreAboutAppRobot.clickSeeData() }

            userDataRobot.checkActivityIsDisplayed()

            waitFor { userDataRobot.checkLastDayOfIsolationIsDisplayed() }

            waitFor { userDataRobot.checkLastDayOfIsolationDisplaysText(expectedExpiryDate) }
        }

    companion object {
        private val startDate = Instant.parse("2020-12-11T13:00:00Z")
        private val contactCaseIsolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = null,
                expiryDate = startDate.atOffset(ZoneOffset.UTC).toLocalDate().plusDays(14)
            )
        )
    }
}

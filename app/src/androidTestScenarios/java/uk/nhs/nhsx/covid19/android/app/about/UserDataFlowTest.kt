package uk.nhs.nhsx.covid19.android.app.about

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyDataRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.ZoneOffset

class UserDataFlowTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val userDataRobot = MyDataRobot()
    private val settingsRobot = SettingsRobot()

    @Test
    fun startContactCase_checkLastDayOfIsolationInStatusScreen_confirmLastDayOfIsolationInUserDataScreen() =
        notReported {
            testAppContext.clock.currentInstant = startInstant
            testAppContext.setState(contactCaseIsolation)

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            waitFor { statusRobot.checkIsolationViewIsDisplayed() }

            val expectedExpiryDate = "24 Dec 2020"

            statusRobot.checkIsolationSubtitleIsDisplayedWithText(testAppContext.app, expectedExpiryDate)

            statusRobot.clickSettings()

            settingsRobot.clickMyDataSetting()

            userDataRobot.checkActivityIsDisplayed()

            waitFor { userDataRobot.checkLastDayOfIsolationIsDisplayed() }

            waitFor { userDataRobot.checkLastDayOfIsolationDisplaysText(expectedExpiryDate) }
        }

    companion object {
        private val startInstant = Instant.parse("2020-12-11T13:00:00Z")
        private val startDate = startInstant.toLocalDate(ZoneOffset.UTC)
        private val contactCaseIsolation = IsolationState(
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                exposureDate = startDate,
                notificationDate = startDate,
                expiryDate = startDate.plusDays(14)
            )
        )
    }
}

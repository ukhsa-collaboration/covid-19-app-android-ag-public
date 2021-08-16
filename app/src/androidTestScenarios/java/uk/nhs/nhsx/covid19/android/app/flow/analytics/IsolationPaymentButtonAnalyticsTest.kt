package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

class IsolationPaymentButtonAnalyticsTest : AnalyticsTest() {

    private val statusRobot = StatusRobot()
    private val isolationHubRobot = IsolationHubRobot()

    @Test
    fun increasesSelectedIsolationPaymentsButtonOnButtonClick() {
        testAppContext.getIsolationPaymentTokenStateProvider().tokenState = Token("token")
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    exposureDate = LocalDate.now().minus(10, DAYS),
                    notificationDate = LocalDate.now().minus(2, DAYS),
                    expiryDate = LocalDate.now().plusDays(30)
                )
            )
        )

        startTestActivity<StatusActivity>()

        assertOnFields {
            assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
        }

        statusRobot.clickIsolationHub()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.clickItemIsolationPayment()

        assertOnFields {
            assertEquals(1, Metrics::selectedIsolationPaymentsButton)
            assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
        }
    }
}

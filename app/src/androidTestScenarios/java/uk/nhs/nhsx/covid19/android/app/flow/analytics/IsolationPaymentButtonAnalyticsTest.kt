package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

class IsolationPaymentButtonAnalyticsTest : AnalyticsTest() {

    private val statusRobot = StatusRobot()
    private val isolationHubRobot = IsolationHubRobot()

    @Test
    fun increasesSelectedIsolationPaymentsButtonOnButtonClick() =
        runWithFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES) {
        testAppContext.getIsolationPaymentTokenStateProvider().tokenState = Token("token")
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                contact = Contact(
                    exposureDate = LocalDate.now(testAppContext.clock).minus(3, DAYS),
                    notificationDate = LocalDate.now(testAppContext.clock).minus(2, DAYS)
                )
            )
        )

        startTestActivity<StatusActivity>()

        assertOnFields(implicitlyAssertNotPresent = false) {
            assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
        }

        statusRobot.clickIsolationHub()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.clickItemIsolationPayment()

        assertOnFields(implicitlyAssertNotPresent = false) {
            assertEquals(1, Metrics::selectedIsolationPaymentsButton)
            assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
        }
    }
}

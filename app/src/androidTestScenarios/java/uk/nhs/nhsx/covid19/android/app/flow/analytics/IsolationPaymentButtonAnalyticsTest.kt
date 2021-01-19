package uk.nhs.nhsx.covid19.android.app.flow.analytics

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class IsolationPaymentButtonAnalyticsTest : AnalyticsTest() {

    private val statusRobot = StatusRobot()

    @Test
    fun increasesSelectedIsolationPaymentsButtonOnButtonClick() {
        testAppContext.getIsolationPaymentTokenStateProvider().tokenState = Token("token")
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now().minus(20, DAYS),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = Instant.now().minus(10, DAYS),
                    notificationDate = Instant.now().minus(2, DAYS),
                    expiryDate = LocalDate.now().plusDays(30)
                )
            )
        )

        startTestActivity<StatusActivity>()

        assertOnFields {
            assertEquals(0, Metrics::selectedIsolationPaymentsButton)
        }

        statusRobot.clickFinancialSupport()

        assertOnFields {
            assertEquals(1, Metrics::selectedIsolationPaymentsButton)
        }
    }
}

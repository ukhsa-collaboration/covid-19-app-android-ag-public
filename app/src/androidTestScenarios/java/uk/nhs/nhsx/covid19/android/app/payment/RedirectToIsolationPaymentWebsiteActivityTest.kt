package uk.nhs.nhsx.covid19.android.app.payment

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RedirectToIsolationPaymentWebsiteActivityTest : EspressoTest() {

    private val isolationPaymentProgressRobot = ProgressRobot()

    @Before
    fun setUp() {
        testAppContext.setIsolationPaymentToken("abc")
        testAppContext.setState(
            State.Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = State.Isolation.ContactCase(
                    startDate = Instant.now().minus(3, ChronoUnit.DAYS),
                    notificationDate = Instant.now().minus(2, ChronoUnit.DAYS),
                    expiryDate = LocalDate.now().plus(1, ChronoUnit.DAYS)
                )
            )
        )
    }

    @Test
    fun opensBrowser() = notReported {
        testAppContext.isolationPaymentApi.shouldPass = true

        assertBrowserIsOpened("about:blank") {
            startTestActivity<RedirectToIsolationPaymentWebsiteActivity>()
        }
    }

    @Test
    fun clickTryAgainButtonOnResponseFailure() = notReported {
        testAppContext.isolationPaymentApi.shouldPass = false

        startTestActivity<RedirectToIsolationPaymentWebsiteActivity>()

        isolationPaymentProgressRobot.checkActivityIsDisplayed()

        isolationPaymentProgressRobot.checkErrorIsDisplayed()

        testAppContext.isolationPaymentApi.shouldPass = true

        assertBrowserIsOpened("about:blank") {
            isolationPaymentProgressRobot.clickTryAgainButton()
        }
    }
}

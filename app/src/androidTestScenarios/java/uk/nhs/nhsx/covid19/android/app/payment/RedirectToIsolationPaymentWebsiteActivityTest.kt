package uk.nhs.nhsx.covid19.android.app.payment

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

class RedirectToIsolationPaymentWebsiteActivityTest : EspressoTest() {

    private val isolationPaymentProgressRobot = ProgressRobot()

    @Before
    fun setUp() {
        testAppContext.setIsolationPaymentToken("abc")
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    exposureDate = LocalDate.now().minus(3, DAYS),
                    notificationDate = LocalDate.now().minus(2, DAYS),
                    expiryDate = LocalDate.now().plus(1, DAYS)
                )
            )
        )
    }

    @Test
    fun opensBrowser() {
        assertBrowserIsOpened("about:blank") {
            startTestActivity<RedirectToIsolationPaymentWebsiteActivity>()
        }
    }

    @Test
    fun clickTryAgainButtonOnResponseFailure() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL

        startTestActivity<RedirectToIsolationPaymentWebsiteActivity>()

        isolationPaymentProgressRobot.checkActivityIsDisplayed()

        isolationPaymentProgressRobot.checkErrorIsDisplayed()

        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

        assertBrowserIsOpened("about:blank") {
            isolationPaymentProgressRobot.clickTryAgainButton()
        }
    }
}

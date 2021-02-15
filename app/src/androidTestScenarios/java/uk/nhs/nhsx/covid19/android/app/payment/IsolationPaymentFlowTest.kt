package uk.nhs.nhsx.covid19.android.app.payment

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.FAIL_SUCCEED_LOOP
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationPaymentRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class IsolationPaymentFlowTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val isolationPaymentRobot = IsolationPaymentRobot()
    private val isolationPaymentProgressRobot = ProgressRobot()
    private val browserRobot = BrowserRobot()

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @RetryFlakyTest
    @Test
    fun whenUserInContactCaseIsolation_performIsolationPaymentFlow_retryOnce_endInStatusActivity() = notReported {
        MockApiModule.behaviour.responseType = FAIL_SUCCEED_LOOP

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

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickFinancialSupport()

        isolationPaymentRobot.checkActivityIsDisplayed()

        isolationPaymentRobot.clickEligibilityButton()

        isolationPaymentProgressRobot.checkErrorIsDisplayed()

        isolationPaymentProgressRobot.clickTryAgainButton()

        browserRobot.checkActivityIsDisplayed()

        browserRobot.clickCloseButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}

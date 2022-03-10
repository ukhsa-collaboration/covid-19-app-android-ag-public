package uk.nhs.nhsx.covid19.android.app.payment

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationPaymentRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

class IsolationPaymentFlowTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val statusRobot = StatusRobot()
    private val isolationPaymentRobot = IsolationPaymentRobot()
    private val isolationPaymentProgressRobot = ProgressRobot()
    private val isolationHubRobot = IsolationHubRobot()
    private val browserRobot = BrowserRobot()

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun whenUserInContactCaseIsolation_performIsolationPaymentFlow_retryOnce_endInStatusActivity() {
        givenLocalAuthorityIsInEngland()
        MockApiModule.behaviour.responseType = ALWAYS_FAIL

        testAppContext.setIsolationPaymentToken("abc")
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                contact = Contact(
                    exposureDate = LocalDate.now().minus(3, DAYS),
                    notificationDate = LocalDate.now().minus(2, DAYS)
                )
            )
        )

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        statusRobot.clickIsolationHub()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.clickItemIsolationPayment()

        isolationPaymentRobot.checkActivityIsDisplayed()
        isolationPaymentRobot.clickEligibilityButton()

        isolationPaymentProgressRobot.checkErrorIsDisplayed()

        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

        isolationPaymentProgressRobot.clickTryAgainButton()

        browserRobot.checkActivityIsDisplayed()

        browserRobot.clickCloseButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}

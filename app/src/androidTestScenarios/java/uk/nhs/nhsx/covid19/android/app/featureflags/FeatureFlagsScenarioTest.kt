package uk.nhs.nhsx.covid19.android.app.featureflags

import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_VENUES
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_DIAGNOSIS
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class FeatureFlagsScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun navigateToStatusScreen_selfDiagnosisFeatureToggledOff() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(SELF_DIAGNOSIS)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkReportSymptomsIsNotDisplayed()
    }

    @Test
    fun navigateToStatusScreen_highRiskVenuesFeatureToggledOff() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(HIGH_RISK_VENUES)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkScanQrCodeOptionIsNotDisplayed()
    }
}

package uk.nhs.nhsx.covid19.android.app.featureflags

import com.jeroenmols.featureflag.framework.FeatureFlag.HIGH_RISK_VENUES
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_DIAGNOSIS
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class FeatureFlagsScenarioTest : EspressoTest() {

    val statusRobot = StatusRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun navigateToStatusScreen_selfDiagnosisFeatureToggledOff() = reporter(
        "Feature flag",
        "Self-diagnosis disabled",
        "Self-diagnosis feature is disabled.",
        Reporter.Kind.FLOW
    ) {
        FeatureFlagTestHelper.disableFeatureFlag(SELF_DIAGNOSIS)

        startTestActivity<StatusActivity>()

        step(
            "Start",
            "The self-diagnosis feature is toggled off via feature flags." +
                " This should prevent the button to start self-diagnosis from being shown."
        )

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkReportSymptomsIsNotDisplayed()
    }

    @Test
    fun navigateToStatusScreen_highRiskVenuesFeatureToggledOff() = reporter(
        "Feature flag",
        "High-risk venues disabled",
        "High-risk venues feature is disabled.",
        Reporter.Kind.FLOW
    ) {
        FeatureFlagTestHelper.disableFeatureFlag(HIGH_RISK_VENUES)

        startTestActivity<StatusActivity>()

        step(
            "Start",
            "The high-risk venues feature is toggled off via feature flags." +
                " This should prevent the option to scan a QR code from being shown."
        )

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkScanQrCodeOptionIsNotDisplayed()
    }
}

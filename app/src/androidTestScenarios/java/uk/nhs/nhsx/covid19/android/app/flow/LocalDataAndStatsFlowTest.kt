package uk.nhs.nhsx.covid19.android.app.flow

import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_COVID_STATS
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.localdata.LocalDataAndStatisticsRobot
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

@RunWith(Parameterized::class)
class LocalDataAndStatsFlowTest(override val configuration: TestConfiguration) : EspressoTest(), LocalAuthoritySetupHelper {

    private val statusRobot = StatusRobot()

    private val localDataAndStatsRobot = LocalDataAndStatisticsRobot()

    @Test
    @Reported
    fun whenLocalDataButtonClicked_showLocalDataAndStats() = reporter(
        scenario = "Local Covid Statistics",
        title = "Happy path - show local covid data statistics screen",
        description = "Users taps on Local Covid-19 data button on the Home screen users sees the Local Covid data screen",
        kind = FLOW
    ) {
        runWithFeatureEnabled(LOCAL_COVID_STATS) {
            givenLocalAuthorityIsInEngland()
            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()
            step(
                stepName = "Show home screen with local data and stats button enabled",
                stepDescription = "User clicks on local data and stats button"
            )

            statusRobot.clickLocalData()

            localDataAndStatsRobot.checkActivityIsDisplayed()

            step(
                stepName = "Local Statistics Screen",
                stepDescription = "The user is presented a screen with information about Local Covid-19 statistics."
            )
        }
    }
}

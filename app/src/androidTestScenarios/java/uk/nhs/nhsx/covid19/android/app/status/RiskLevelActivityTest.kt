package uk.nhs.nhsx.covid19.android.app.status

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity.Companion.EXTRA_RISK_LEVEL
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskLevelRobot

class RiskLevelActivityTest : EspressoTest() {

    private val riskLevelRobot = RiskLevelRobot()
    private val postCode = "ZE3"

    @Test
    fun testRiskLevelLow() = notReported {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    R.string.status_area_risk_level,
                    R.string.status_area_risk_level_low,
                    LOW
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleForLowRiskDisplayed(postCode)

        riskLevelRobot.checkTextForLowRiskDisplayed()

        riskLevelRobot.checkImageForLowRiskDisplayed()
    }

    @Test
    fun testRiskLevelMedium() = notReported {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    R.string.status_area_risk_level,
                    R.string.status_area_risk_level_medium,
                    MEDIUM
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleForMediumRiskDisplayed(postCode)

        riskLevelRobot.checkTextForMediumRiskDisplayed()

        riskLevelRobot.checkImageForMediumRiskDisplayed()
    }

    @Test
    fun testRiskLevelHigh() = notReported {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    R.string.status_area_risk_level,
                    R.string.status_area_risk_level_high,
                    HIGH
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleForHighRiskDisplayed(postCode)

        riskLevelRobot.checkTextForHighRiskDisplayed()

        riskLevelRobot.checkImageForHighRiskDisplayed()
    }
}

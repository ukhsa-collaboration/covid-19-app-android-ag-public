package uk.nhs.nhsx.covid19.android.app.status

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity.Companion.EXTRA_RISK_LEVEL
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.HighRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.LowRisk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.MediumRisk
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskLevelRobot

class RiskLevelActivityTest : EspressoTest() {
    private val riskLevelRobot = RiskLevelRobot()
    private val postCode = "ZE3"

    @Test
    fun testRiskLevelLow() {
        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, LowRisk(postCode))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleForLowRiskDisplayed(postCode)

        riskLevelRobot.checkTextForLowRiskDisplayed()

        riskLevelRobot.checkImageForLowRiskDisplayed()
    }

    @Test
    fun testRiskLevelMedium() {
        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, MediumRisk(postCode))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleForMediumRiskDisplayed(postCode)

        riskLevelRobot.checkTextForMediumRiskDisplayed()

        riskLevelRobot.checkImageForMediumRiskDisplayed()
    }

    @Test
    fun testRiskLevelHigh() {
        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, HighRisk(postCode))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleForHighRiskDisplayed(postCode)

        riskLevelRobot.checkTextForHighRiskDisplayed()

        riskLevelRobot.checkImageForHighRiskDisplayed()
    }
}

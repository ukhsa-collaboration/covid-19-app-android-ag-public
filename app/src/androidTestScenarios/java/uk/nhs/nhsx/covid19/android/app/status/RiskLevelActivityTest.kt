package uk.nhs.nhsx.covid19.android.app.status

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
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
                    RiskIndicator(
                        colorScheme = ColorScheme.GREEN,
                        name = Translatable(mapOf("en" to "$postCode is in Local Alert Level 1")),
                        heading = Translatable(mapOf("en" to "Heading low")),
                        content = Translatable(
                            mapOf(
                                "en" to "Content low"
                            )
                        ),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c"))
                    )
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in Local Alert Level 1")

        riskLevelRobot.checkContentForLowRiskDisplayed()

        riskLevelRobot.checkImageForLowRiskDisplayed()
    }

    @Test
    fun testRiskLevelMedium() = notReported {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.YELLOW,
                        name = Translatable(mapOf("en" to "$postCode is in Local Alert Level 2")),
                        heading = Translatable(mapOf("en" to "Heading medium")),
                        content = Translatable(
                            mapOf(
                                "en" to "Content medium"
                            )
                        ),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c"))
                    )
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in Local Alert Level 2")

        riskLevelRobot.checkContentForMediumRiskDisplayed()

        riskLevelRobot.checkImageForMediumRiskDisplayed()
    }

    @Test
    fun testRiskLevelHigh() = notReported {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.RED,
                        name = Translatable(mapOf("en" to "$postCode is in Local Alert Level 3")),
                        heading = Translatable(mapOf("en" to "Heading high")),
                        content = Translatable(
                            mapOf(
                                "en" to "Content high"
                            )
                        ),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c"))
                    )
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in Local Alert Level 3")

        riskLevelRobot.checkContentForHighRiskDisplayed()

        riskLevelRobot.checkImageForHighRiskDisplayed()
    }
}

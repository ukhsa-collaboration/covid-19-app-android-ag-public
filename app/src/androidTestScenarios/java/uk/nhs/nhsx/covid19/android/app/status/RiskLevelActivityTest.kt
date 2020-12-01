package uk.nhs.nhsx.covid19.android.app.status

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity.Companion.EXTRA_RISK_LEVEL
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskLevelRobot

class RiskLevelActivityTest : EspressoTest() {

    private val riskLevelRobot = RiskLevelRobot()
    private val postCode = "ZE3"

    private val meetingPolicy = Policy(
        policyIcon = PolicyIcon.MEETING_PEOPLE,
        policyHeading = Translatable(mapOf("en" to "Meeting people")),
        policyContent = Translatable(mapOf("en" to "Rule of six indoors and outdoors, in all settings."))
    )

    @After
    fun tearDown() {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)
    }

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
                        content = Translatable(mapOf("en" to "Content low")),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
                        policyData = null
                    ),
                    riskLevelFromLocalAuthority = false
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in Local Alert Level 1")

        riskLevelRobot.checkContentFromPostDistrictIsDisplayed("Content low")

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
                        content = Translatable(mapOf("en" to "Content medium")),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
                        policyData = null
                    ),
                    riskLevelFromLocalAuthority = false
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in Local Alert Level 2")

        riskLevelRobot.checkContentFromPostDistrictIsDisplayed("Content medium")

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
                        content = Translatable(mapOf("en" to "Content high")),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
                        policyData = null
                    ),
                    riskLevelFromLocalAuthority = false
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in Local Alert Level 3")

        riskLevelRobot.checkContentFromPostDistrictIsDisplayed("Content high")

        riskLevelRobot.checkImageForHighRiskDisplayed()
    }

    @Test
    fun testRiskLevelHighWithLocalAuthority() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.RED,
                        name = Translatable(mapOf("en" to "$postCode is in Local Alert Level 3")),
                        heading = Translatable(mapOf("en" to "Heading high")),
                        content = Translatable(mapOf("en" to "Content high")),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
                        policyData = PolicyData(
                            heading = Translatable(mapOf("en" to "Coronavirus cases are very high in your area")),
                            content = Translatable(mapOf("en" to "Local Authority content high")),
                            footer = Translatable(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                            policies = listOf(meetingPolicy, meetingPolicy),
                            localAuthorityRiskTitle = Translatable(mapOf("en" to "$postCode is in local COVID alert level: high"))
                        )
                    ),
                    riskLevelFromLocalAuthority = true
                )
            )
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in local COVID alert level: high")

        riskLevelRobot.checkContentFromLocalAuthorityIsDisplayed("Local Authority content high")

        riskLevelRobot.checkImageForHighRiskDisplayed()

        riskLevelRobot.checkForFooter()
    }
}

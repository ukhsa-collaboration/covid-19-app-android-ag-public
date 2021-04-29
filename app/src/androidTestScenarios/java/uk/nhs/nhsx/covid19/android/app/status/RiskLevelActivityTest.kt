package uk.nhs.nhsx.covid19.android.app.status

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
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskLevelRobot
import kotlin.test.assertTrue

class RiskLevelActivityTest : EspressoTest() {

    private val riskLevelRobot = RiskLevelRobot()
    private val postCode = "ZE3"

    private val meetingPolicy = Policy(
        policyIcon = PolicyIcon.MEETING_PEOPLE,
        policyHeading = Translatable(mapOf("en" to "Meeting people")),
        policyContent = Translatable(mapOf("en" to "Rule of six indoors and outdoors, in all settings."))
    )

    private val socialDistancingPolicy = Policy(
        policyIcon = PolicyIcon.SOCIAL_DISTANCING,
        policyHeading = Translatable(mapOf("en" to "Social distancing")),
        policyContent = Translatable(mapOf("en" to "Please keep a safe distance of at least 2 meters people not living in your household."))
    )

    private val workPolicy = Policy(
        policyIcon = PolicyIcon.WORK,
        policyHeading = Translatable(mapOf("en" to "Work")),
        policyContent = Translatable(mapOf("en" to "If working from home is possible, it is advised to do so."))
    )

    @Test
    fun testRiskLevelLow_notFromLocalAuthority() = notReported {
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
    fun testRiskLevelMedium_notFromLocalAuthority() = notReported {
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
    fun testRiskLevelHigh_notFromLocalAuthority() = notReported {
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

    @Test
    fun testRiskLevelUnknown() = notReported {
        val activity = startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Unknown
            )
        }

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun testRiskLevelTierFour() = notReported {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.RED,
                        colorSchemeV2 = ColorScheme.MAROON,
                        name = Translatable(mapOf("en" to "$postCode is in Local Alert Level 4")),
                        heading = Translatable(mapOf("en" to "Heading high")),
                        content = Translatable(mapOf("en" to "Content high")),
                        linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
                        linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
                        policyData = PolicyData(
                            heading = Translatable(mapOf("en" to "Coronavirus cases are very high in your area")),
                            content = Translatable(mapOf("en" to "Local Authority content high")),
                            footer = Translatable(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                            policies = listOf(meetingPolicy, socialDistancingPolicy, workPolicy),
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

        riskLevelRobot.checkImageForTierFourRiskDisplayed()

        riskLevelRobot.checkForFooter()
    }

    @Test
    fun testRiskLevelFive() = notReported {
        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, getTierFiveRisk(postCode))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in local COVID alert level: very high")

        riskLevelRobot.checkContentFromLocalAuthorityIsDisplayed("Local Authority content very high")

        riskLevelRobot.checkImageForTierFiveRiskDisplayed()

        riskLevelRobot.checkForFooter()
    }

    @Test
    fun testPostDistrictInEngland_riskIndicatorContainsPolicyInfo_showMassTestingInformation() = notReported {
        testAppContext.setPostCode("BN10")
        testAppContext.setLocalAuthority("E07000063")

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, getTierFiveRisk("BN10"))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkMassTestingIsDisplayed()
    }

    @Test
    fun testPostDistrictIsNotInEngland_doNotShowMassTestingInformation() = notReported {
        testAppContext.setPostCode("NP10")
        testAppContext.setLocalAuthority("W06000018")

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, getTierFiveRisk("NP10"))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkMassTestingIsNotDisplayed()
    }

    @Test
    fun testPostDistrictInEngland_riskIndicatorDoesNotContainPolicyInfo_showMassTestingInformation() =
        notReported {
            testAppContext.setPostCode("BN10")
            testAppContext.setLocalAuthority("E07000063")

            val risk = getTierFiveRisk("BN10")

            startTestActivity<RiskLevelActivity> {
                putExtra(EXTRA_RISK_LEVEL, risk.copy(riskIndicator = risk.riskIndicator.copy(policyData = null)))
            }

            riskLevelRobot.checkActivityIsDisplayed()

            riskLevelRobot.checkMassTestingIsDisplayed()
        }

    private fun getTierFiveRisk(postCode: String) = Risk(
        postCode,
        RiskIndicator(
            colorScheme = ColorScheme.RED,
            colorSchemeV2 = ColorScheme.BLACK,
            name = Translatable(mapOf("en" to "$postCode is in Local Alert Level 5")),
            heading = Translatable(mapOf("en" to "Heading very high")),
            content = Translatable(mapOf("en" to "Content very high")),
            linkTitle = Translatable(mapOf("en" to "Restrictions in your area")),
            linkUrl = Translatable(mapOf("en" to "https://a.b.c")),
            policyData = PolicyData(
                heading = Translatable(mapOf("en" to "Coronavirus cases are very high in your area")),
                content = Translatable(mapOf("en" to "Local Authority content very high")),
                footer = Translatable(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                policies = listOf(meetingPolicy, socialDistancingPolicy),
                localAuthorityRiskTitle = Translatable(mapOf("en" to "$postCode is in local COVID alert level: very high"))
            )
        ),
        riskLevelFromLocalAuthority = true
    )
}

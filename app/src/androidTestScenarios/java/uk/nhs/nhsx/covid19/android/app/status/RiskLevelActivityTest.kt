package uk.nhs.nhsx.covid19.android.app.status

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlData
import uk.nhs.nhsx.covid19.android.app.remote.data.ExternalUrlsWrapper
import uk.nhs.nhsx.covid19.android.app.remote.data.Policy
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyData
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.status.RiskLevelActivity.Companion.EXTRA_RISK_LEVEL
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Risk
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.RiskyPostCodeViewState.Unknown
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskLevelRobot
import kotlin.test.assertTrue

class RiskLevelActivityTest : EspressoTest() {

    private val riskLevelRobot = RiskLevelRobot()
    private val postCode = "ZE3"
    private val urlTitle1 = "NHS link"
    private val urlTitle2 = "NHS link 2"
    private val urlTitle3 = "NHS link 3"
    private val externalUrlHeader = "Helpful Links"

    private val meetingPolicy = Policy(
        policyIcon = PolicyIcon.MEETING_PEOPLE,
        policyHeading = TranslatableString(mapOf("en" to "Meeting people")),
        policyContent = TranslatableString(mapOf("en" to "Rule of six indoors and outdoors, in all settings."))
    )

    private val socialDistancingPolicy = Policy(
        policyIcon = PolicyIcon.SOCIAL_DISTANCING,
        policyHeading = TranslatableString(mapOf("en" to "Social distancing")),
        policyContent = TranslatableString(mapOf("en" to "Please keep a safe distance of at least 2 meters people not living in your household."))
    )

    private val workPolicy = Policy(
        policyIcon = PolicyIcon.WORK,
        policyHeading = TranslatableString(mapOf("en" to "Work")),
        policyContent = TranslatableString(mapOf("en" to "If working from home is possible, it is advised to do so."))
    )

    private val externalUrlData = ExternalUrlData(
        title = TranslatableString(mapOf("en" to urlTitle1)),
        url = TranslatableString(mapOf("en" to "https://a.b.c"))
    )

    private val severalExternalUrls = ExternalUrlsWrapper(
        title = TranslatableString(mapOf("en" to externalUrlHeader)),
        urls = listOf(
            externalUrlData,
            externalUrlData.copy(title = TranslatableString(mapOf("en" to urlTitle2))),
            externalUrlData.copy(title = TranslatableString(mapOf("en" to urlTitle3)))
        )
    )

    private val oneExternalUrl = ExternalUrlsWrapper(
        title = TranslatableString(mapOf("en" to externalUrlHeader)),
        urls = listOf(externalUrlData)
    )

    private val oneExternalUrlNoHeader = ExternalUrlsWrapper(
        title = null,
        urls = listOf(externalUrlData)
    )

    private val emptyExternalUrlList = ExternalUrlsWrapper(
        title = null,
        urls = listOf()
    )

    @Test
    fun testRiskLevelLow_notFromLocalAuthority() {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.GREEN,
                        name = TranslatableString(mapOf("en" to "$postCode is in Local Alert Level 1")),
                        heading = TranslatableString(mapOf("en" to "Heading low")),
                        content = TranslatableString(mapOf("en" to "Content low")),
                        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
                        policyData = null,
                        externalUrls = null
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
    fun testRiskLevelMedium_notFromLocalAuthority() {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.YELLOW,
                        name = TranslatableString(mapOf("en" to "$postCode is in Local Alert Level 2")),
                        heading = TranslatableString(mapOf("en" to "Heading medium")),
                        content = TranslatableString(mapOf("en" to "Content medium")),
                        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
                        policyData = null,
                        externalUrls = null
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
    fun testRiskLevelHigh_notFromLocalAuthority() {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.RED,
                        name = TranslatableString(mapOf("en" to "$postCode is in Local Alert Level 3")),
                        heading = TranslatableString(mapOf("en" to "Heading high")),
                        content = TranslatableString(mapOf("en" to "Content high")),
                        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
                        policyData = null,
                        externalUrls = null
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
    fun testRiskLevelHigh() {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.RED,
                        name = TranslatableString(mapOf("en" to "$postCode is in Local Alert Level 3")),
                        heading = TranslatableString(mapOf("en" to "Heading high")),
                        content = TranslatableString(mapOf("en" to "Content high")),
                        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
                        policyData = PolicyData(
                            heading = TranslatableString(mapOf("en" to "Coronavirus cases are very high in your area")),
                            content = TranslatableString(mapOf("en" to "Local Authority content high")),
                            footer = TranslatableString(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                            policies = listOf(meetingPolicy, meetingPolicy),
                            localAuthorityRiskTitle = TranslatableString(mapOf("en" to "$postCode is in local COVID alert level: high"))
                        ),
                        externalUrls = null
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
    fun testRiskLevelUnknown() {
        val activity = startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Unknown
            )
        }

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun testRiskLevelTierFour() {
        startTestActivity<RiskLevelActivity> {
            putExtra(
                EXTRA_RISK_LEVEL,
                Risk(
                    postCode,
                    RiskIndicator(
                        colorScheme = ColorScheme.RED,
                        colorSchemeV2 = ColorScheme.MAROON,
                        name = TranslatableString(mapOf("en" to "$postCode is in Local Alert Level 4")),
                        heading = TranslatableString(mapOf("en" to "Heading high")),
                        content = TranslatableString(mapOf("en" to "Content high")),
                        linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
                        linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
                        policyData = PolicyData(
                            heading = TranslatableString(mapOf("en" to "Coronavirus cases are very high in your area")),
                            content = TranslatableString(mapOf("en" to "Local Authority content high")),
                            footer = TranslatableString(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                            policies = listOf(meetingPolicy, socialDistancingPolicy, workPolicy),
                            localAuthorityRiskTitle = TranslatableString(mapOf("en" to "$postCode is in local COVID alert level: high"))
                        ),
                        externalUrls = null
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
    fun testRiskLevelFive() {
        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, getTierFiveRisk(postCode))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkTitleIsDisplayed("$postCode is in local COVID alert level: very high")

        riskLevelRobot.checkContentFromLocalAuthorityIsDisplayed("Local Authority content very high")

        riskLevelRobot.checkImageForTierFiveRiskDisplayed()

        riskLevelRobot.checkForFooter()

        riskLevelRobot.checkExternalUrlSectionIsHidden()
    }

    @Test
    fun testPostDistrictInEngland_riskIndicatorContainsPolicyInfo_showMassTestingInformation() {
        testAppContext.setPostCode("BN10")
        testAppContext.setLocalAuthority("E07000063")

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, getTierFiveRisk("BN10"))
        }

        riskLevelRobot.checkActivityIsDisplayed()
    }

    @Test
    fun testPostDistrictIsNotInEngland_doNotShowMassTestingInformation() {
        testAppContext.setPostCode("NP10")
        testAppContext.setLocalAuthority("W06000018")

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, getTierFiveRisk("NP10"))
        }

        riskLevelRobot.checkActivityIsDisplayed()
    }

    @Test
    fun testPostDistrictInEngland_riskIndicatorDoesNotContainPolicyInfo_showMassTestingInformation() {
        testAppContext.setPostCode("BN10")
        testAppContext.setLocalAuthority("E07000063")

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, getTierFiveRisk("BN10"))
        }

        riskLevelRobot.checkActivityIsDisplayed()
    }

    @Test
    fun testRiskLevelWithOneExternalUrl() {
        val risk = getTierFiveRisk(postCode)

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, risk.copy(riskIndicator = risk.riskIndicator.copy(externalUrls = oneExternalUrl)))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkExternalUrlSectionIsDisplayed()

        riskLevelRobot.checkExternalUrlHeaderIsDisplayed(externalUrlHeader)
        riskLevelRobot.checkUrlIsDisplayed(urlTitle1)
        riskLevelRobot.clickExternalUrlSectionLink_opensInExternalBrowser(urlTitle1)
    }

    @Test
    fun testRiskLevelWithOneExternalUrlAndNoExternalUrlHeader_headerIsNotDisplayed() {
        val risk = getTierFiveRisk(postCode)

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, risk.copy(riskIndicator = risk.riskIndicator.copy(externalUrls = oneExternalUrlNoHeader)))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkExternalUrlSectionIsDisplayed()

        riskLevelRobot.checkUrlIsDisplayed(urlTitle1)
        riskLevelRobot.checkExternalUrlSectionHasCorrectNumberOfChildElements(1)
    }

    @Test
    fun testRiskLevelWithSeveralExternalUrls() {
        val risk = getTierFiveRisk(postCode)

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, risk.copy(riskIndicator = risk.riskIndicator.copy(externalUrls = severalExternalUrls)))
        }

        riskLevelRobot.checkActivityIsDisplayed()

        riskLevelRobot.checkExternalUrlSectionIsDisplayed()

        riskLevelRobot.checkExternalUrlHeaderIsDisplayed(externalUrlHeader)
        riskLevelRobot.checkUrlIsDisplayed(urlTitle1)
        riskLevelRobot.checkUrlIsDisplayed(urlTitle2)
        riskLevelRobot.checkUrlIsDisplayed(urlTitle3)
    }

    @Test
    fun testRiskLevelWithEmptyExternalUrlList_externalUrlSectionIsHidden() {
        val risk = getTierFiveRisk(postCode)

        startTestActivity<RiskLevelActivity> {
            putExtra(EXTRA_RISK_LEVEL, risk.copy(riskIndicator = risk.riskIndicator.copy(externalUrls = emptyExternalUrlList)))
        }

        riskLevelRobot.checkActivityIsDisplayed()
        riskLevelRobot.checkExternalUrlSectionIsHidden()
    }

    private fun getTierFiveRisk(postCode: String) = Risk(
        postCode,
        RiskIndicator(
            colorScheme = ColorScheme.RED,
            colorSchemeV2 = ColorScheme.BLACK,
            name = TranslatableString(mapOf("en" to "$postCode is in Local Alert Level 5")),
            heading = TranslatableString(mapOf("en" to "Heading very high")),
            content = TranslatableString(mapOf("en" to "Content very high")),
            linkTitle = TranslatableString(mapOf("en" to "Restrictions in your area")),
            linkUrl = TranslatableString(mapOf("en" to "https://a.b.c")),
            policyData = PolicyData(
                heading = TranslatableString(mapOf("en" to "Coronavirus cases are very high in your area")),
                content = TranslatableString(mapOf("en" to "Local Authority content very high")),
                footer = TranslatableString(mapOf("en" to "Find out what rules apply in your area to help reduce the spread of coronavirus.")),
                policies = listOf(meetingPolicy, socialDistancingPolicy),
                localAuthorityRiskTitle = TranslatableString(mapOf("en" to "$postCode is in local COVID alert level: very high"))
            ),
            externalUrls = null
        ),
        riskLevelFromLocalAuthority = true
    )
}

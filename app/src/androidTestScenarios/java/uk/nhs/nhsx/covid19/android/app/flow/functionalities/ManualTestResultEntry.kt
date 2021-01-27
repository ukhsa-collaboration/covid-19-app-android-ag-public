package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_RAPID_SELF_REPORTED_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class ManualTestResultEntry(private val testAppContext: TestApplicationContext) {
    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val testResultRobot = TestResultRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    fun enterPositive(virologyTestKitType: VirologyTestKitType) {
        val token = when (virologyTestKitType) {
            LAB_RESULT -> POSITIVE_PCR_TOKEN
            RAPID_RESULT -> POSITIVE_LFD_TOKEN
            RAPID_SELF_REPORTED -> POSITIVE_RAPID_SELF_REPORTED_TOKEN
        }

        manuallyEnterTestResult(token)

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        testResultRobot.clickIsolationActionButton()

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

        shareKeysInformationRobot.clickIUnderstandButton()
    }

    fun enterNegative() {
        manuallyEnterTestResult(NEGATIVE_PCR_TOKEN)
    }

    fun enterVoid() {
        manuallyEnterTestResult(VOID_PCR_TOKEN)
    }

    private fun manuallyEnterTestResult(token: String) {
        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.clickLinkTestResult()

        waitFor { linkTestResultRobot.checkActivityIsDisplayed() }

        testAppContext.virologyTestingApi.testEndDate = testAppContext.clock.instant()

        linkTestResultRobot.enterCtaToken(token)

        linkTestResultRobot.clickContinue()
    }
}

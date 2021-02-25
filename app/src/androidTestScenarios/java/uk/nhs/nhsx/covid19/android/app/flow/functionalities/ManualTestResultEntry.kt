package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN_INDICATIVE_NO_KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_RAPID_SELF_REPORTED_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultOnsetDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import java.time.Instant
import java.time.ZoneOffset

class ManualTestResultEntry(private val testAppContext: TestApplicationContext) {
    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestSymptomsRobot = LinkTestResultSymptomsRobot()
    private val linkTestResultOnsetDateRobot = LinkTestResultOnsetDateRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    fun enterPositive(
        virologyTestKitType: VirologyTestKitType,
        symptomsAndOnsetFlowConfiguration: SymptomsAndOnsetFlowConfiguration? = null,
        requiresConfirmatoryTest: Boolean = false,
        testEndDate: Instant = testAppContext.clock.instant()
    ) {
        val inIsolation = testAppContext.getCurrentState() is Isolation

        val token = when (virologyTestKitType) {
            LAB_RESULT -> POSITIVE_PCR_TOKEN
            RAPID_RESULT ->
                if (requiresConfirmatoryTest) POSITIVE_LFD_TOKEN_INDICATIVE_NO_KEY_SUBMISSION else POSITIVE_LFD_TOKEN
            RAPID_SELF_REPORTED -> POSITIVE_RAPID_SELF_REPORTED_TOKEN
        }

        manuallyEnterTestResult(token, testEndDate)

        if (symptomsAndOnsetFlowConfiguration != null) {
            waitFor { linkTestSymptomsRobot.checkActivityIsDisplayed() }
            if (symptomsAndOnsetFlowConfiguration.didHaveSymptoms) {
                linkTestSymptomsRobot.clickYes()
                waitFor { linkTestResultOnsetDateRobot.checkActivityIsDisplayed() }
                if (symptomsAndOnsetFlowConfiguration.didRememberOnsetSymptomsDate) {
                    linkTestResultOnsetDateRobot.clickSelectDate()
                    waitFor {
                        linkTestResultOnsetDateRobot.selectDayOfMonth(
                            testEndDate.atOffset(ZoneOffset.UTC).toLocalDate().dayOfMonth
                        )
                    }
                } else {
                    linkTestResultOnsetDateRobot.selectCannotRememberDate()
                }
                linkTestResultOnsetDateRobot.clickContinueButton()
            } else {
                linkTestSymptomsRobot.clickNo()
            }
        }

        if (requiresConfirmatoryTest) {
            waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() }

            testResultRobot.clickCloseButton()
        } else {
            if (inIsolation) {
                waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }
            } else {
                waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
            }

            testResultRobot.clickIsolationActionButton()

            waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

            shareKeysInformationRobot.clickIUnderstandButton()
        }
    }

    fun enterNegative(testEndDate: Instant = testAppContext.clock.instant()) {
        manuallyEnterTestResult(NEGATIVE_PCR_TOKEN, testEndDate)
    }

    fun enterVoid(testEndDate: Instant = testAppContext.clock.instant()) {
        manuallyEnterTestResult(VOID_PCR_TOKEN, testEndDate)
    }

    private fun manuallyEnterTestResult(token: String, testEndDate: Instant) {
        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.clickLinkTestResult()

        waitFor { linkTestResultRobot.checkActivityIsDisplayed() }

        testAppContext.virologyTestingApi.testEndDate = testEndDate

        linkTestResultRobot.enterCtaToken(token)

        linkTestResultRobot.clickContinue()
    }

    data class SymptomsAndOnsetFlowConfiguration(
        val didHaveSymptoms: Boolean = false,
        val didRememberOnsetSymptomsDate: Boolean = false
    )
}

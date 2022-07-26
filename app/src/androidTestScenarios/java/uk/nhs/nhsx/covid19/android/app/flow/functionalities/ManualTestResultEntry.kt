package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.exposure.executeWithTheUserDecliningExposureKeySharing
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN_INDICATIVE
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_RAPID_SELF_REPORTED_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_RAPID_SELF_REPORTED_TOKEN_INDICATIVE
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BookFollowUpTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultOnsetDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.ZoneOffset

class ManualTestResultEntry(private val testAppContext: TestApplicationContext) {
    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestSymptomsRobot = LinkTestResultSymptomsRobot()
    private val linkTestResultOnsetDateRobot = LinkTestResultOnsetDateRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val bookFollowUpTestRobot = BookFollowUpTestRobot()
    private val shareKeys = ShareKeys()
    private val shareKeysAndBookTest = ShareKeysAndBookTest(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    fun enterPositive(
        virologyTestKitType: VirologyTestKitType,
        symptomsAndOnsetFlowConfiguration: SymptomsAndOnsetFlowConfiguration? = null,
        expectedScreenState: ExpectedScreenAfterPositiveTestResult,
        requiresConfirmatoryTest: Boolean = false,
        testEndDate: Instant = testAppContext.clock.instant(),
        country: PostCodeDistrict = WALES
    ) {
        val token = when (virologyTestKitType) {
            LAB_RESULT -> POSITIVE_PCR_TOKEN
            RAPID_RESULT ->
                if (requiresConfirmatoryTest) POSITIVE_LFD_TOKEN_INDICATIVE else POSITIVE_LFD_TOKEN
            RAPID_SELF_REPORTED ->
                if (requiresConfirmatoryTest) POSITIVE_RAPID_SELF_REPORTED_TOKEN_INDICATIVE else POSITIVE_RAPID_SELF_REPORTED_TOKEN
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
                            testEndDate.toLocalDate(ZoneOffset.UTC).dayOfMonth
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

        when (expectedScreenState) {
            is PositiveWillBeInIsolationAndOrderTest -> {
                waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest(country) }
                testResultRobot.clickCloseButton()
            }
            is PositiveContinueIsolation -> {
                waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(country) }
                testResultRobot.clickIsolationActionButton()
                shareKeys()
            }
            is PositiveWillBeInIsolation -> {
                waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country) }
                if (requiresConfirmatoryTest) {
                    if (expectedScreenState.includeBookATestFlow) {
                        shareKeysAndBookTest()
                        testAppContext.device.pressBack()
                        bookFollowUpTestRobot.clickCloseButton()
                    } else {
                        testResultRobot.clickIsolationActionButton()
                        shareKeys()
                    }
                } else {
                    testResultRobot.clickIsolationActionButton()
                    shareKeys()
                }
            }
            is PositiveWontBeInIsolation -> {
                waitFor { testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation(WALES) }
                testResultRobot.clickGoodNewsActionButton()
            }
        }
    }

    fun enterNegative(testEndDate: Instant = testAppContext.clock.instant()) {
        manuallyEnterTestResult(NEGATIVE_PCR_TOKEN, testEndDate)
    }

    fun enterVoid(testEndDate: Instant = testAppContext.clock.instant()) {
        manuallyEnterTestResult(VOID_PCR_TOKEN, testEndDate)
    }

    fun manuallyEnterTestResult(token: String, testEndDate: Instant) {
        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.clickLinkTestResult()

        waitFor { linkTestResultRobot.checkActivityIsDisplayed() }

        testAppContext.virologyTestingApi.testEndDate = testEndDate

        linkTestResultRobot.enterCtaToken(token)

        linkTestResultRobot.clickContinue()
    }

    fun enterPositivePCRTestResultAndDeclineExposureKeySharing(backGroundTask: () -> Unit) {
        manuallyEnterTestResult(POSITIVE_PCR_TOKEN, testAppContext.clock.instant())
        testResultRobot.clickIsolationActionButton()
        backGroundTask()
        testAppContext.executeWithTheUserDecliningExposureKeySharing {
            shareKeysInformationRobot.clickContinueButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
    }

    data class SymptomsAndOnsetFlowConfiguration(
        val didHaveSymptoms: Boolean = false,
        val didRememberOnsetSymptomsDate: Boolean = false
    )

    sealed class ExpectedScreenAfterPositiveTestResult {
        object PositiveWillBeInIsolationAndOrderTest : ExpectedScreenAfterPositiveTestResult()
        object PositiveContinueIsolation : ExpectedScreenAfterPositiveTestResult()
        data class PositiveWillBeInIsolation(val includeBookATestFlow: Boolean = true) : ExpectedScreenAfterPositiveTestResult()
        object PositiveWontBeInIsolation : ExpectedScreenAfterPositiveTestResult()
    }
}

package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.exposure.executeWithTheUserDecliningExposureKeySharing
import uk.nhs.nhsx.covid19.android.app.remote.TestResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import java.time.Instant

class PollingTestResult(val testAppContext: TestApplicationContext) {

    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysProgressRobot = ProgressRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val statusRobot = StatusRobot()

    private fun receiveAndAcknowledgeResult(
        result: VirologyTestResult,
        testKitType: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean,
        runBackgroundTasks: () -> Unit,
        acknowledge: () -> Unit,
        testEndDate: Instant = testAppContext.clock.instant()
    ) {
        with(testAppContext.virologyTestingApi) {
            pollingTestResultHttpStatusCode = 200
            this.testEndDate = testEndDate
            testResponseForPollingToken[pollingToken] =
                TestResponse(result, testKitType, requiresConfirmatoryTest = requiresConfirmatoryTest)
        }

        runBackgroundTasks()
        acknowledge()
    }

    fun receiveAndAcknowledgePositiveTestResult(
        testKitType: VirologyTestKitType,
        runBackgroundTasks: () -> Unit,
        keySharingSucceeds: Boolean = true
    ) {
        receiveAndAcknowledgeResult(
            result = POSITIVE,
            testKitType = testKitType,
            requiresConfirmatoryTest = false,
            runBackgroundTasks = runBackgroundTasks,
            acknowledge = {
                testResultRobot.clickIsolationActionButton()
                if (keySharingSucceeds) {
                    shareKeysInformationRobot.clickContinueButton()
                    waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }
                    shareKeysResultRobot.clickActionButton()
                } else {
                    testAppContext.executeWhileOffline {
                        shareKeysInformationRobot.clickContinueButton()
                        waitFor { shareKeysProgressRobot.checkActivityIsDisplayed() }
                        shareKeysProgressRobot.checkErrorIsDisplayed()
                    }
                    shareKeysProgressRobot.clickCancelButton()
                    waitFor { statusRobot.checkActivityIsDisplayed() }
                }
            },
        )
    }

    fun receiveAndAcknowledgePositiveTestResultAndDeclineKeySharing(
        testKitType: VirologyTestKitType,
        runBackgroundTasks: () -> Unit
    ) {
        receiveAndAcknowledgeResult(
            result = POSITIVE,
            testKitType = testKitType, requiresConfirmatoryTest = false,
            runBackgroundTasks = runBackgroundTasks,
            acknowledge = {
                testResultRobot.clickIsolationActionButton()
                testAppContext.executeWithTheUserDecliningExposureKeySharing {
                    shareKeysInformationRobot.clickContinueButton()
                    waitFor { statusRobot.checkActivityIsDisplayed() }
                }
            }
        )
    }

    fun receiveAndAcknowledgeNegativeTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            result = NEGATIVE,
            testKitType = testKitType, requiresConfirmatoryTest = false,
            runBackgroundTasks = runBackgroundTasks,
            acknowledge = {
                testResultRobot.clickGoodNewsActionButton()
            },
        )
    }

    fun receiveAndAcknowledgeVoidTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            result = VOID,
            testKitType = testKitType, requiresConfirmatoryTest = false,
            runBackgroundTasks = runBackgroundTasks,
            acknowledge = {
                testResultRobot.clickIsolationActionButton()
            },
        )
    }
}

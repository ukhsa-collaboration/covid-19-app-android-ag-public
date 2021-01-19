package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot

class PollingTestResult(
    private val espressoTest: EspressoTest
) {

    private val testResultRobot = TestResultRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    private fun receiveAndAcknowledgeResult(
        result: VirologyTestResult,
        runBackgroundTasks: () -> Unit,
        acknowledge: () -> Unit
    ) {
        with(espressoTest.testAppContext.virologyTestingApi) {
            pollingTestResultHttpStatusCode = 200
            testEndDate = espressoTest.testAppContext.clock.instant()
            testResultForPollingToken[pollingToken] = result
        }

        runBackgroundTasks()
        acknowledge()
    }

    fun receiveAndAcknowledgePositiveTestResult(runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(VirologyTestResult.POSITIVE, runBackgroundTasks) {
            testResultRobot.clickIsolationActionButton()
            shareKeysInformationRobot.clickIUnderstandButton()
        }
    }

    fun receiveAndAcknowledgeNegativeTestResult(runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(VirologyTestResult.NEGATIVE, runBackgroundTasks) {
            testResultRobot.clickGoodNewsActionButton()
        }
    }

    fun receiveAndAcknowledgeVoidTestResult(runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(VirologyTestResult.VOID, runBackgroundTasks) {
            testResultRobot.clickIsolationActionButton()
        }
    }
}

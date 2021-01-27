package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.remote.TestResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
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
        testKitType: VirologyTestKitType,
        runBackgroundTasks: () -> Unit,
        acknowledge: () -> Unit
    ) {
        with(espressoTest.testAppContext.virologyTestingApi) {
            pollingTestResultHttpStatusCode = 200
            testEndDate = espressoTest.testAppContext.clock.instant()
            testResponseForPollingToken[pollingToken] = TestResponse(result, testKitType)
        }

        runBackgroundTasks()
        acknowledge()
    }

    fun receiveAndAcknowledgePositiveTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            POSITIVE, testKitType, runBackgroundTasks,
            {
                testResultRobot.clickIsolationActionButton()
                shareKeysInformationRobot.clickIUnderstandButton()
            },
        )
    }

    fun receiveAndAcknowledgeNegativeTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            NEGATIVE, testKitType, runBackgroundTasks,
            {
                testResultRobot.clickGoodNewsActionButton()
            },
        )
    }

    fun receiveAndAcknowledgeVoidTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            VOID, testKitType, runBackgroundTasks,
            {
                testResultRobot.clickIsolationActionButton()
            },
        )
    }
}

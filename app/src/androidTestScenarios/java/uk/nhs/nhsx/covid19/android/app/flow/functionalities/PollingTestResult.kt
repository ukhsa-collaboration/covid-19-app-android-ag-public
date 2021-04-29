package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.remote.TestResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import java.time.Instant

class PollingTestResult(private val espressoTest: EspressoTest) {

    private val testResultRobot = TestResultRobot(espressoTest.testAppContext.app)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()

    internal fun receiveAndAcknowledgeResult(
        result: VirologyTestResult,
        testKitType: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean,
        runBackgroundTasks: () -> Unit,
        acknowledge: () -> Unit,
        testEndDate: Instant = espressoTest.testAppContext.clock.instant()
    ) {
        with(espressoTest.testAppContext.virologyTestingApi) {
            pollingTestResultHttpStatusCode = 200
            this.testEndDate = testEndDate
            testResponseForPollingToken[pollingToken] =
                TestResponse(result, testKitType, requiresConfirmatoryTest = requiresConfirmatoryTest)
        }

        runBackgroundTasks()
        acknowledge()
    }

    fun receiveAndAcknowledgePositiveTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            POSITIVE, testKitType, requiresConfirmatoryTest = false, runBackgroundTasks,
            {
                testResultRobot.clickIsolationActionButton()
                shareKeysInformationRobot.clickContinueButton()
                waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }
                shareKeysResultRobot.clickActionButton()
            },
        )
    }

    fun receiveAndAcknowledgeNegativeTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            NEGATIVE, testKitType, requiresConfirmatoryTest = false, runBackgroundTasks,
            {
                testResultRobot.clickGoodNewsActionButton()
            },
        )
    }

    fun receiveAndAcknowledgeVoidTestResult(testKitType: VirologyTestKitType, runBackgroundTasks: () -> Unit) {
        receiveAndAcknowledgeResult(
            VOID, testKitType, requiresConfirmatoryTest = false, runBackgroundTasks,
            {
                testResultRobot.clickIsolationActionButton()
            },
        )
    }
}

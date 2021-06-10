package uk.nhs.nhsx.covid19.android.app.about

import androidx.test.platform.app.InstrumentationRegistry
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.about.mydata.MyDataActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyDataRobot
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.LocalDate

class MyDataActivityTest : EspressoTest() {
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val myDataRobot = MyDataRobot()

    private val latestRiskyVenueVisitDate = LocalDate.parse("2020-07-25")
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Before
    fun setUp() = runBlocking {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
            latestRiskyVenueVisitDate,
            RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
        )
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun myDataScreenShows() = notReported {
        startTestActivity<MoreAboutAppActivity>()

        moreAboutAppRobot.checkActivityIsDisplayed()
    }

    @RetryFlakyTest
    @Test
    fun displayEmptyViewWhenNoData() = notReported {
        testAppContext.apply {
            setState(isolationHelper.neverInIsolation())
            getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null
        }
        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        myDataRobot.checkEmptyViewIsShown()
    }

    @Test
    fun doNotDisplayTestResultSectionIfNoTestResults() = notReported {
        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        myDataRobot.checkLastTestResultIsNotDisplayed()
    }

    @Test
    fun displaysLastVisitedBookTestTypeVenueDateIsDisplayed() = notReported {
        testAppContext.getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
            latestRiskyVenueVisitDate,
            RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
        )

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        myDataRobot.checkLastVisitedBookTestTypeVenueDateIsDisplayed(latestRiskyVenueVisitDate.uiFormat(testAppContext.app))
    }

    @Test
    fun displayLastPositivePcrAcknowledgedTestResult() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            LAB_RESULT
        )
    }

    @Test
    fun displayLastPositiveAssistedLfdAcknowledgedTestResult() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_RESULT
        )
    }

    @Test
    fun displayLastPositiveUnassistedLfdAcknowledgedTestResult() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_SELF_REPORTED
        )
    }

    @Test
    fun displayLastNegativePcrAcknowledgedTestResult() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            LAB_RESULT
        )
    }

    @Test
    fun displayLastNegativeAssistedLfdAcknowledgedTestResult() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                RAPID_RESULT
            )
        }

    @Test
    fun displayLastNegativeUnassistedLfdAcknowledgedTestResult() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                RAPID_SELF_REPORTED
            )
        }

    @Test
    fun requiresConfirmatoryTestNotReceivedFollowUpTestShouldBePending() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_SELF_REPORTED,
            requiresConfirmatoryTest = true
        )
    }

    @Test
    fun requiresConfirmatoryTestReceivedFollowUpTestShouldBeComplete() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_SELF_REPORTED,
            requiresConfirmatoryTest = true,
            completedDate = LocalDate.parse("2020-07-18")
        )
    }

    @Test
    fun requiresConfirmatoryTestReceivedFollowUpAfterDayLimitTestShouldBeComplete() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_RESULT,
            requiresConfirmatoryTest = true,
            completedDate = LocalDate.parse("2020-07-18"),
            confirmatoryTestCompletionStatus = COMPLETED
        )
    }

    @Test
    fun displayLastPositiveAcknowledgedTestResultOfUnknownType() =
        notReported {
            displayLastAcknowledgedTestResult(
                POSITIVE,
                testKitType = null // UNKNOWN
            )
        }

    @Test
    fun displayLastNegativeAcknowledgedTestResultOfUnknownType() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                testKitType = null // UNKNOWN
            )
        }

    private fun displayLastAcknowledgedTestResult(
        testResult: VirologyTestResult,
        testKitType: VirologyTestKitType?,
        requiresConfirmatoryTest: Boolean = false,
        completedDate: LocalDate? = null,
        confirmatoryTestCompletionStatus: ConfirmatoryTestCompletionStatus? = completedDate?.let { COMPLETED_AND_CONFIRMED }
    ) {
        when (testResult) {
            POSITIVE -> testAppContext.setState(
                isolationHelper.positiveTest(
                    AcknowledgedTestResult(
                        testEndDate = LocalDate.now(),
                        testResult = RelevantVirologyTestResult.POSITIVE,
                        testKitType = testKitType,
                        requiresConfirmatoryTest = requiresConfirmatoryTest,
                        acknowledgedDate = LocalDate.now(),
                        confirmedDate = completedDate,
                        confirmatoryTestCompletionStatus = confirmatoryTestCompletionStatus
                    )
                ).asIsolation()
            )

            NEGATIVE -> {
                if (requiresConfirmatoryTest || completedDate != null) {
                    throw IllegalArgumentException("Negative test results should not need or accept a follow-up test")
                }

                testAppContext.setState(
                    isolationHelper.negativeTest(
                        AcknowledgedTestResult(
                            testEndDate = LocalDate.now(),
                            testResult = RelevantVirologyTestResult.NEGATIVE,
                            testKitType = testKitType,
                            requiresConfirmatoryTest = requiresConfirmatoryTest,
                            acknowledgedDate = LocalDate.now()
                        )
                    ).asIsolation()
                )
            }

            VOID -> throw IllegalArgumentException("$testResult is not supported since it is never stored after being acknowledged")
        }

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        val shouldKitTypeBeVisible = testKitType != null

        val date: String? = completedDate
            ?.uiFormat(InstrumentationRegistry.getInstrumentation().targetContext)

        waitFor {
            myDataRobot.checkLastTestResultIsDisplayed(
                shouldKitTypeBeVisible,
                requiresConfirmatoryTest,
                date
            )
        }
    }

    @Test
    fun displayEncounterInIsolationWithNotificationDate() = notReported {
        testAppContext.setState(isolationHelper.contactCase().asIsolation())

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkEncounterIsDisplayed() }
        myDataRobot.checkExposureNotificationIsDisplayed()
        myDataRobot.checkExposureNotificationDateIsDisplayed()
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun displaySymptomsInIsolation() = notReported {
        testAppContext.setState(
            isolationHelper.selfAssessment(
                expired = false,
                onsetDate = LocalDate.now(testAppContext.clock).minusDays(2)
            ).asIsolation()
        )

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkSymptomsAreDisplayed() }
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun contactCaseOnly_notOptedInToDailyContactTesting_displayLastDayOfIsolationInIsolation() = notReported {
        testAppContext.setState(isolationHelper.contactCase().asIsolation())

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkLastDayOfIsolationIsDisplayed() }
        waitFor { myDataRobot.checkExposureNotificationDateIsDisplayed() }
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun doNotDisplayLastDayOfIsolationWhenIsolationIsExpired() = notReported {
        testAppContext.setState(isolationHelper.contactCase(expired = true).asIsolation())

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkLastDayOfIsolationIsNotDisplayed() }
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun doNotDisplayLastDayOfIsolationWhenNeverInIsolation() = notReported {
        testAppContext.setState(isolationHelper.neverInIsolation())

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkLastDayOfIsolationIsNotDisplayed() }
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun previouslyIsolatedAsContactCaseOnly_optedInToDailyContactTesting_showDailyContactTestingOptInDate() =
        notReported {
            FeatureFlagTestHelper.enableFeatureFlag(DAILY_CONTACT_TESTING)

            testAppContext.setState(
                isolationHelper.contactCaseWithDct(
                    dailyContactTestingOptInDate = LocalDate.now(testAppContext.clock)
                ).asIsolation()
            )

            startTestActivity<MyDataActivity>()

            myDataRobot.checkActivityIsDisplayed()

            waitFor { myDataRobot.checkLastDayOfIsolationIsNotDisplayed() }
            waitFor { myDataRobot.checkEncounterIsDisplayed() }
            waitFor { myDataRobot.checkExposureNotificationIsDisplayed() }
            waitFor { myDataRobot.checkExposureNotificationDateIsDisplayed() }
            waitFor { myDataRobot.checkDailyContactTestingOptInDateIsDisplayed() }
        }
}

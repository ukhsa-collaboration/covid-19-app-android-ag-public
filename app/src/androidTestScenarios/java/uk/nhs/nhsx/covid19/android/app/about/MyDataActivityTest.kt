package uk.nhs.nhsx.covid19.android.app.about

import androidx.test.platform.app.InstrumentationRegistry
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyDataRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Confirm
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MyDataActivityTest : EspressoTest() {
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val myDataRobot = MyDataRobot()

    private val latestRiskyVenueVisitDate = LocalDate.parse("2020-07-25")

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
            setState(Default())
            getLastVisitedBookTestTypeVenueDateProvider().lastVisitedVenue = null
            getRelevantTestResultProvider().clear()
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
    fun displayLastPositivePcrAcknowledgedTestResultWithKeySubmissionSupported() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true
        )
    }

    @Test
    fun displayLastPositivePcrAcknowledgedTestResultWithKeySubmissionNotSupported() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = false
        )
    }

    @Test
    fun displayLastPositiveAssistedLfdAcknowledgedTestResultWithKeySubmissionSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                POSITIVE,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        }

    @Test
    fun displayLastPositiveAssistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                POSITIVE,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = false
            )
        }

    @Test
    fun displayLastPositiveUnassistedLfdAcknowledgedTestResultWithKeySubmissionSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = true
            )
        }

    @Test
    fun displayLastPositiveUnassistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = false
            )
        }

    @Test
    fun displayLastNegativePcrAcknowledgedTestResultWithKeySubmissionSupported() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true
        )
    }

    @Test
    fun displayLastNegativePcrAcknowledgedTestResultWithKeySubmissionNotSupported() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = false
        )
    }

    @Test
    fun displayLastNegativeAssistedLfdAcknowledgedTestResultWithKeySubmissionSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        }

    @Test
    fun displayLastNegativeAssistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = false
            )
        }

    @Test
    fun displayLastNegativeUnassistedLfdAcknowledgedTestResultWithKeySubmissionSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = true
            )
        }

    @Test
    fun displayLastNegativeUnassistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = false
            )
        }

    @Test
    fun requiresConfirmatoryTestNotReceivedFollowUpTestShouldBePending() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            RAPID_SELF_REPORTED,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = true
        )
    }

    @Test
    fun requiresConfirmatoryTestReceivedFollowUpTestShouldBeComplete() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            RAPID_SELF_REPORTED,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = true,
            receivedFollowUpTest = Instant.parse("2020-07-18T00:05:00.00Z")
        )
    }

    @Test
    fun displayLastPositiveAcknowledgedTestResultOfUnknownTypeWithKeySubmissionSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                POSITIVE,
                testKitType = null, // UNKNOWN
                diagnosisKeySubmissionSupported = true
            )
        }

    @Test
    fun displayLastNegativeAcknowledgedTestResultOfUnknownTypeWithKeySubmissionSupported() =
        notReported {
            displayLastAcknowledgedTestResult(
                NEGATIVE,
                testKitType = null, // UNKNOWN
                diagnosisKeySubmissionSupported = true
            )
        }

    private fun displayLastAcknowledgedTestResult(
        testResult: VirologyTestResult,
        testKitType: VirologyTestKitType?,
        diagnosisKeySubmissionSupported: Boolean,
        requiresConfirmatoryTest: Boolean = false,
        receivedFollowUpTest: Instant? = null
    ) {
        val initialTestResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "a",
            testEndDate = Instant.now(),
            testResult = testResult,
            testKitType = testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported
        )

        testAppContext.getRelevantTestResultProvider().onTestResultAcknowledged(initialTestResult, Overwrite)

        if (receivedFollowUpTest != null) {
            val followupTest = ReceivedTestResult(
                diagnosisKeySubmissionToken = "b",
                testEndDate = receivedFollowUpTest,
                testResult = POSITIVE,
                testKitType = testKitType,
                requiresConfirmatoryTest = false,
                diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported
            )
            testAppContext.getRelevantTestResultProvider()
                .onTestResultAcknowledged(followupTest, Confirm(confirmedDate = receivedFollowUpTest))
        }

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        val shouldKitTypeBeVisible = testKitType != null

        val date: String? = receivedFollowUpTest?.atZone(ZoneId.systemDefault())?.toLocalDate()
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
    fun displayEncounterInIsolationWithoutNotificationDate() = notReported {
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    Instant.parse("2020-05-19T12:00:00Z"),
                    null,
                    LocalDate.now().plusDays(5),
                    dailyContactTestingOptInDate = null
                )
            )
        )

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkEncounterIsDisplayed() }
        myDataRobot.checkExposureNotificationIsDisplayed()
        myDataRobot.checkExposureNotificationDateIsNotDisplayed()
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun displayEncounterInIsolationWithNotificationDate() = notReported {
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    Instant.parse("2020-05-19T12:00:00Z"),
                    Instant.parse("2020-05-20T12:00:00Z"),
                    LocalDate.now().plusDays(5),
                    dailyContactTestingOptInDate = null
                )
            )
        )

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
            Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.now(),
                    expiryDate = LocalDate.now().plusDays(5),
                    selfAssessment = false
                )
            )
        )

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkSymptomsAreDisplayed() }
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun contactCaseOnly_notOptedInToDailyContactTesting_displayLastDayOfIsolationInIsolation() = notReported {
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    Instant.parse("2020-05-19T12:00:00Z"),
                    Instant.parse("2020-05-20T12:00:00Z"),
                    LocalDate.now().plusDays(5),
                    dailyContactTestingOptInDate = null
                )
            )
        )

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkLastDayOfIsolationIsDisplayed() }
        waitFor { myDataRobot.checkExposureNotificationDateIsDisplayed() }
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun doNotDisplayLastDayOfIsolationWhenNotInIsolation() = notReported {
        testAppContext.setState(Default())

        startTestActivity<MyDataActivity>()

        myDataRobot.checkActivityIsDisplayed()

        waitFor { myDataRobot.checkLastDayOfIsolationIsNotDisplayed() }
        waitFor { myDataRobot.checkDailyContactTestingOptInDateIsNotDisplayed() }
    }

    @Test
    fun previouslyIsolatedAsContactCaseOnly_optedInToDailyContactTesting_showDailyContactTestingOptInDate() =
        notReported {
            FeatureFlagTestHelper.enableFeatureFlag(DAILY_CONTACT_TESTING)

            testAppContext.setState(defaultWithPreviousIsolationContactCaseOnly)

            startTestActivity<MyDataActivity>()

            myDataRobot.checkActivityIsDisplayed()

            waitFor { myDataRobot.checkLastDayOfIsolationIsNotDisplayed() }
            waitFor { myDataRobot.checkExposureNotificationDateIsNotDisplayed() }
            waitFor { myDataRobot.checkDailyContactTestingOptInDateIsDisplayed() }
        }

    private val defaultWithPreviousIsolationContactCaseOnly = Default(
        previousIsolation = Isolation(
            isolationStart = Instant.now(),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                Instant.parse("2020-05-19T12:00:00Z"),
                null,
                LocalDate.now().plusDays(5),
                dailyContactTestingOptInDate = LocalDate.now().plusDays(5)
            )
        )
    )
}

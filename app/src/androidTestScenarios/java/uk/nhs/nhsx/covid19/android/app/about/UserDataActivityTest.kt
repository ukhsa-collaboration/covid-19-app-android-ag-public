package uk.nhs.nhsx.covid19.android.app.about

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DataAndPrivacyRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UserDataRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.WelcomeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult

class UserDataActivityTest : EspressoTest() {
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val userDataRobot = UserDataRobot()
    private val welcomeRobot = WelcomeRobot()
    private val dataAndPrivacyRobot = DataAndPrivacyRobot()
    private val postCodeRobot = PostCodeRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val statusRobot = StatusRobot()
    private val permissionRobot = PermissionRobot()

    private val visits = listOf(
        VenueVisit(
            venue = Venue("1", "Venue1"),
            from = Instant.parse("2020-07-25T10:00:00Z"),
            to = Instant.parse("2020-07-25T12:00:00Z")
        ),
        VenueVisit(
            venue = Venue("2", "Venue2"),
            from = Instant.parse("2020-07-25T10:00:00Z"),
            to = Instant.parse("2020-07-25T12:00:00Z")
        )
    )

    @Before
    fun setUp() = runBlocking {
        testAppContext.getVisitedVenuesStorage().setVisits(visits)
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

    @Test
    fun clickOnSetDataOpensMyDataScreen() = notReported {
        startTestActivity<MoreAboutAppActivity>()

        moreAboutAppRobot.checkActivityIsDisplayed()

        moreAboutAppRobot.clickSeeData()

        userDataRobot.checkActivityIsDisplayed()
    }

    @RetryFlakyTest
    @Test
    fun clickOnDeleteUserDataWithLocalAuthorityFeatureFlagEnabled_opensWelcomeScreenAndShowsPermissionScreenWithoutDialog() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        testAppContext.setPostCode(null)

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        userDataRobot.userClicksOnDeleteAllDataButton()

        userDataRobot.userClicksDeleteDataOnDialog()

        waitFor { welcomeRobot.isActivityDisplayed() }

        welcomeRobot.checkActivityIsDisplayed()

        welcomeRobot.clickConfirmOnboarding()

        welcomeRobot.checkAgeConfirmationDialogIsDisplayed()

        welcomeRobot.clickConfirmAgePositive()

        dataAndPrivacyRobot.checkActivityIsDisplayed()

        dataAndPrivacyRobot.clickConfirmOnboarding()

        postCodeRobot.checkActivityIsDisplayed()

        postCodeRobot.enterPostCode("N12")

        postCodeRobot.clickContinue()

        waitFor { localAuthorityRobot.checkActivityIsDisplayed() }

        localAuthorityRobot.clickConfirm()

        waitFor { permissionRobot.checkActivityIsDisplayed() }

        permissionRobot.clickEnablePermissions()

        statusRobot.checkActivityIsDisplayed()
    }

    @RetryFlakyTest
    @Test
    fun clickOnDeleteUserDataWithLocalAuthorityFeatureFlagDisabled_opensWelcomeScreenAndShowsPermissionScreenWithoutDialog() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        testAppContext.setPostCode(null)

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        userDataRobot.userClicksOnDeleteAllDataButton()

        waitFor { userDataRobot.checkDeleteDataConfirmationDialogIsDisplayed() }

        setScreenOrientation(LANDSCAPE)

        waitFor { userDataRobot.checkDeleteDataConfirmationDialogIsDisplayed() }

        setScreenOrientation(PORTRAIT)

        waitFor { userDataRobot.checkDeleteDataConfirmationDialogIsDisplayed() }

        waitFor { userDataRobot.userClicksDeleteDataOnDialog() }

        waitFor { welcomeRobot.isActivityDisplayed() }

        welcomeRobot.checkActivityIsDisplayed()

        welcomeRobot.clickConfirmOnboarding()

        welcomeRobot.checkAgeConfirmationDialogIsDisplayed()

        welcomeRobot.clickConfirmAgePositive()

        dataAndPrivacyRobot.checkActivityIsDisplayed()

        dataAndPrivacyRobot.clickConfirmOnboarding()

        postCodeRobot.checkActivityIsDisplayed()

        postCodeRobot.enterPostCode("SE1")

        postCodeRobot.clickContinue()

        waitFor { permissionRobot.checkActivityIsDisplayed() }

        permissionRobot.clickEnablePermissions()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun deleteSingleVenueVisit() = notReported {
        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        waitFor { userDataRobot.editVenueVisitsIsDisplayed() }

        userDataRobot.userClicksEditVenueVisits()

        userDataRobot.checkDeleteIconForFirstVenueVisitIsDisplayed()

        userDataRobot.clickDeleteVenueVisitOnFirstPosition()

        waitFor { userDataRobot.confirmDialogIsDisplayed() }

        setScreenOrientation(LANDSCAPE)

        waitFor { userDataRobot.confirmDialogIsDisplayed() }

        setScreenOrientation(PORTRAIT)

        waitFor { userDataRobot.confirmDialogIsDisplayed() }

        userDataRobot.userClicksConfirmOnDialog()

        waitFor { userDataRobot.userClicksEditVenueVisits() }

        userDataRobot.editVenueVisitsIsDisplayed()
    }

    @Test
    fun doNotDisplayTestResultSectionIfNoTestResults() = notReported {
        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        userDataRobot.checkLastTestResultIsNotDisplayed()
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
    fun displayLastPositiveAssistedLfdAcknowledgedTestResultWithKeySubmissionSupported() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = true
        )
    }

    @Test
    fun displayLastPositiveAssistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = false
        )
    }

    @Test
    fun displayLastPositiveUnassistedLfdAcknowledgedTestResultWithKeySubmissionSupported() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            RAPID_SELF_REPORTED,
            diagnosisKeySubmissionSupported = true
        )
    }

    @Test
    fun displayLastPositiveUnassistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() = notReported {
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
    fun displayLastNegativeAssistedLfdAcknowledgedTestResultWithKeySubmissionSupported() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = true
        )
    }

    @Test
    fun displayLastNegativeAssistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = false
        )
    }

    @Test
    fun displayLastNegativeUnassistedLfdAcknowledgedTestResultWithKeySubmissionSupported() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            RAPID_SELF_REPORTED,
            diagnosisKeySubmissionSupported = true
        )
    }

    @Test
    fun displayLastNegativeUnassistedLfdAcknowledgedTestResultWithKeySubmissionNotSupported() = notReported {
        displayLastAcknowledgedTestResult(
            NEGATIVE,
            RAPID_SELF_REPORTED,
            diagnosisKeySubmissionSupported = false
        )
    }

    @Test
    fun displayLastPositiveAcknowledgedTestResultOfUnknownTypeWithKeySubmissionSupported() = notReported {
        displayLastAcknowledgedTestResult(
            POSITIVE,
            testKitType = null, // UNKNOWN
            diagnosisKeySubmissionSupported = true
        )
    }

    private fun displayLastAcknowledgedTestResult(
        testResult: VirologyTestResult,
        testKitType: VirologyTestKitType?,
        diagnosisKeySubmissionSupported: Boolean
    ) {
        testAppContext.getRelevantTestResultProvider().onTestResultAcknowledged(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = testResult,
                testKitType = testKitType,
                diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported
            )
        )

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        val shouldKitTypeBeVisible = testKitType != null
        waitFor { userDataRobot.checkLastTestResultIsDisplayed(shouldKitTypeBeVisible) }
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
                    LocalDate.now().plusDays(5)
                )
            )
        )

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        waitFor { userDataRobot.checkEncounterIsDisplayed() }
        userDataRobot.checkExposureNotificationIsDisplayed()
        userDataRobot.checkExposureNotificationDateIsNotDisplayed()
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
                    LocalDate.now().plusDays(5)
                )
            )
        )

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        waitFor { userDataRobot.checkEncounterIsDisplayed() }
        userDataRobot.checkExposureNotificationIsDisplayed()
        userDataRobot.checkExposureNotificationDateIsDisplayed()
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

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        waitFor { userDataRobot.checkSymptomsAreDisplayed() }
    }

    @Test
    fun displayLastDayOfIsolationInIsolation() = notReported {
        testAppContext.setState(
            Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    Instant.parse("2020-05-19T12:00:00Z"),
                    Instant.parse("2020-05-20T12:00:00Z"),
                    LocalDate.now().plusDays(5)
                )
            )
        )

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        waitFor { userDataRobot.checkLastDayOfIsolationIsDisplayed() }
    }

    @Test
    fun doNotDisplayLastDayOfIsolationWhenNotInIsolation() = notReported {
        testAppContext.setState(Default())

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        waitFor { userDataRobot.checkLastDayOfIsolationIsNotDisplayed() }
    }
}

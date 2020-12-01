package uk.nhs.nhsx.covid19.android.app.about

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.report.notReported
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
import java.time.Instant

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

        userDataRobot.userClicksDeleteDataOnDialog()

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

        userDataRobot.userClicksConfirmOnDialog()

        waitFor { userDataRobot.userClicksEditVenueVisits() }

        userDataRobot.editVenueVisitsIsDisplayed()
    }
}

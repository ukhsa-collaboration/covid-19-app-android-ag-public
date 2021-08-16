package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationPaymentRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.BookTestTypeVenueVisitSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper

class IsolationHubActivityTest : EspressoTest(), IsolationSetupHelper, BookTestTypeVenueVisitSetupHelper {
    private val isolationHubRobot = IsolationHubRobot()
    private val symptomsAfterRiskyVenueRobot = SymptomsAfterRiskyVenueRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val isolationPaymentRobot = IsolationPaymentRobot()
    override val isolationHelper = IsolationHelper(testAppContext.clock)

    //region Book a test button
    @Test
    fun whenInSelfAssessmentIsolation_andNoBookTestTypeVenueVisitStored_showBookATestButton_tapButton_navigateToBookAPcrTest() {
        givenSelfAssessmentIsolation()
        givenNoBookTestTypeVenueVisitStored()

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemBookTestIsDisplayed()
        isolationHubRobot.clickItemBookATest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenInSelfAssessmentIsolation_andBookTestTypeVenueVisitStored_showBookATestButton_tapButton_navigateToBookAPcrTest() {
        givenSelfAssessmentIsolation()
        givenBookTestTypeVenueVisitStored()

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemBookTestIsDisplayed()
        isolationHubRobot.clickItemBookATest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenInSelfAssessmentAndContactIsolation_andNoBookTestTypeVenueVisitStored_showBookATestButton_tapButton_navigateToBookAPcrTest() {
        givenSelfAssessmentAndContactIsolation()
        givenNoBookTestTypeVenueVisitStored()

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemBookTestIsDisplayed()
        isolationHubRobot.clickItemBookATest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenInContactIsolation_andNoBookTestTypeVenueVisitStored_showBookATestButton_tapButton_navigateToBookAPcrTest() {
        givenContactIsolation()
        givenNoBookTestTypeVenueVisitStored()

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemBookTestIsDisplayed()
        isolationHubRobot.clickItemBookATest()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenNotInActiveIsolation_andBookTestTypeVenueVisitStored_showBookATestButton_tapButton_navigateToSymptomsAfterRiskyVenue() {
        givenNeverInIsolation()
        givenBookTestTypeVenueVisitStored()

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemBookTestIsDisplayed()
        isolationHubRobot.clickItemBookATest()

        symptomsAfterRiskyVenueRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenNotInActiveIsolation_andNoBookTestTypeVenueVisitStored_shouldNotShowBookATestButton() {
        givenNeverInIsolation()
        givenNoBookTestTypeVenueVisitStored()

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemBookTestIsNotDisplayed()
    }
    //endregion

    //region Isolation payment button
    @Test
    fun whenInSelfAssessmentIsolation_thusCanNotClaimIsolationPayment_shouldNotShowIsolationPaymentButton() {
        givenSelfAssessmentIsolation()

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemIsolationPaymentIsNotDisplayed()
    }

    @Test
    fun whenCanClaimIsolationPayment_andTokenStateIsToken_showIsolationPaymentButton_tapButton_navigateToIsolationPayment() {
        givenContactIsolation()

        testAppContext.setIsolationPaymentToken("token")

        startTestActivity<IsolationHubActivity>()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemIsolationPaymentIsDisplayed()
        isolationHubRobot.clickItemIsolationPayment()

        isolationPaymentRobot.checkActivityIsDisplayed()
    }
    //endregion
}

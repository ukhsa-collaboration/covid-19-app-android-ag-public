package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.VENUE_CHECK_IN_BUTTON
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.BookTestAfterM2VenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import uk.nhs.nhsx.covid19.android.app.testhelpers.coRunWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatures
import java.time.Instant

class RiskyVenueAnalyticsTest : AnalyticsTest() {

    private val statusRobot = StatusRobot()

    private val bookTestAfterM2VenueAlert = BookTestAfterM2VenueAlert()

    @Before
    override fun setUp() {
        super.setUp()
        testAppContext.getVisitedVenuesStorage().removeAllVenueVisits()
        testAppContext.riskyVenuesApi.riskyVenuesResponse = RiskyVenuesResponse(venues = riskyVenues)
    }

    @Test
    fun receiveRiskyVenueWithTypeM1() {
        runBlocking {
            coRunWithFeature(feature = VENUE_CHECK_IN_BUTTON, enabled = true) {
                startTestActivity<MainActivity>()

                // Current date: 1st Jan
                // Starting state: App running normally
                assertAnalyticsPacketIsNormal()

                testAppContext.getVisitedVenuesStorage().setVisits(listOf(m1VenueVisit))

                // Current date: 2nd Jan -> Analytics packet for: 1st Jan
                assertOnFields {
                    assertEquals(1, Metrics::receivedRiskyVenueM1Warning)
                }

                // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
                assertAnalyticsPacketIsNormal()
            }
        }
    }

    @Test
    fun receiveRiskyVenueWithTypeM1AndM2() {
        runBlocking {
            coRunWithFeature(feature = VENUE_CHECK_IN_BUTTON, enabled = true) {
                startTestActivity<MainActivity>()

                // Current date: 1st Jan
                // Starting state: App running normally
                assertAnalyticsPacketIsNormal()

                testAppContext.getVisitedVenuesStorage().setVisits(listOf(m1VenueVisit, m2VenueVisit))

                // Current date: 2nd Jan -> Analytics packet for: 1st Jan
                assertOnFields {
                    assertEquals(1, Metrics::receivedRiskyVenueM2Warning)
                    assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
                }

                // Dates: 3rd-11th Jan -> Analytics packets for: 2nd-10th Jan
                assertOnFieldsForDateRange(3..11) {
                    assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
                }

                // Current date: 12th Jan -> Analytics packet for: 11th Jan
                assertAnalyticsPacketIsNormal()
            }
        }
    }

    @Test
    fun startAppFromRiskyVenueM2Notification_tracksDidAccessRiskyVenueM2Notification() {
        runWithFeatureEnabled(feature = VENUE_CHECK_IN_BUTTON) {
            startTestActivity<MainActivity> {
                putExtra(NotificationProvider.RISKY_VENUE_NOTIFICATION_TAPPED_WITH_TYPE, BOOK_TEST)
            }

            waitFor { statusRobot.checkActivityIsDisplayed() }

            assertOnFields {
                assertEquals(1, Metrics::didAccessRiskyVenueM2Notification)
            }
        }
    }

    @Test
    fun startAppWithPendingM2VenueAlert_takeTestLater_backToHome() {
        runBlocking {
            coRunWithFeature(feature = VENUE_CHECK_IN_BUTTON, enabled = true) {
                startTestActivity<MainActivity>()

                assertAnalyticsPacketIsNormal()

                testAppContext.getVisitedVenuesStorage().setVisits(listOf(m2VenueVisit))
                runBackgroundTasks()

                bookTestAfterM2VenueAlert(bookTest = false)

                assertOnFields {
                    assertEquals(1, Metrics::receivedRiskyVenueM2Warning)
                    assertEquals(1, Metrics::selectedTakeTestLaterM2Journey)
                    assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
                }
            }
        }
    }

    @Test
    fun startAppWithPendingM2VenueAlert_takeTestNow_hasSymptoms_showsQuestionnaire() {
        runBlocking {
            coRunWithFeature(feature = VENUE_CHECK_IN_BUTTON, enabled = true) {
                startTestActivity<MainActivity>()

                assertAnalyticsPacketIsNormal()

                testAppContext.getVisitedVenuesStorage().setVisits(listOf(m2VenueVisit))
                runBackgroundTasks()

                bookTestAfterM2VenueAlert(bookTest = true, hasSymptoms = true)

                assertOnFields {
                    assertEquals(1, Metrics::receivedRiskyVenueM2Warning)
                    assertEquals(1, Metrics::selectedTakeTestM2Journey)
                    assertEquals(1, Metrics::selectedHasSymptomsM2Journey)
                    assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
                }
            }
        }
    }

    @Test
    fun startAppWithPendingM2VenueAlert_takeTestNow_hasNoSymptoms_ordersLfdTest_showsWebsite() {
        runWithFeatures(
            featureList = listOf(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER, VENUE_CHECK_IN_BUTTON),
            enabled = true
        ) {
            runBlocking {
                startTestActivity<MainActivity>()

                assertAnalyticsPacketIsNormal()

                testAppContext.getVisitedVenuesStorage().setVisits(listOf(m2VenueVisit))
                runBackgroundTasks()

                bookTestAfterM2VenueAlert(bookTest = true, hasSymptoms = false, alreadyHasLfdTests = false)

                assertOnFields {
                    assertEquals(1, Metrics::receivedRiskyVenueM2Warning)
                    assertEquals(1, Metrics::selectedTakeTestM2Journey)
                    assertEquals(1, Metrics::selectedHasNoSymptomsM2Journey)
                    assertEquals(1, Metrics::selectedLFDTestOrderingM2Journey)
                    assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
                }
            }
        }
    }

    @Test
    fun startAppWithPendingM2VenueAlert_takeTestNow_hasNoSymptoms_alreadyHasLfdTests_backToHome() {
        runBlocking {
            coRunWithFeature(VENUE_CHECK_IN_BUTTON, true) {
                startTestActivity<MainActivity>()

                assertAnalyticsPacketIsNormal()

                testAppContext.getVisitedVenuesStorage().setVisits(listOf(m2VenueVisit))
                runBackgroundTasks()

                bookTestAfterM2VenueAlert(bookTest = true, hasSymptoms = false, alreadyHasLfdTests = true)

                assertOnFields {
                    assertEquals(1, Metrics::receivedRiskyVenueM2Warning)
                    assertEquals(1, Metrics::selectedTakeTestM2Journey)
                    assertEquals(1, Metrics::selectedHasNoSymptomsM2Journey)
                    assertEquals(1, Metrics::selectedHasLFDTestM2Journey)
                    assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
                }
            }
        }
    }

    private val m1Venue = Venue(
        id = "3KR9JX59",
        organizationPartName = "Venue1"
    )

    private val m2Venue = Venue(
        id = "2V542M5J",
        organizationPartName = "Venue2"
    )

    private val m1VenueVisit = VenueVisit(
        venue = m1Venue,
        from = Instant.parse("2020-01-01T02:00:00Z"),
        to = Instant.parse("2020-01-01T04:00:00Z")
    )

    private val m2VenueVisit = VenueVisit(
        venue = m2Venue,
        from = Instant.parse("2020-01-01T05:00:00Z"),
        to = Instant.parse("2020-01-01T06:00:00Z")
    )

    private val riskyVenues = listOf(
        RiskyVenue(
            m1Venue.id,
            RiskyWindow(
                from = Instant.parse("2020-01-01T00:00:00Z"),
                to = Instant.parse("2020-01-31T23:59:59Z")
            ),
            messageType = INFORM
        ),
        RiskyVenue(
            m2Venue.id,
            RiskyWindow(
                from = Instant.parse("2020-01-01T00:00:00Z"),
                to = Instant.parse("2020-01-30T23:59:59Z")
            ),
            messageType = BOOK_TEST
        )
    )
}

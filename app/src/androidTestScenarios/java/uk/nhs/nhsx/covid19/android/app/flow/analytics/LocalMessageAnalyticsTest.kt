package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.MockLocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalMessageRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class LocalMessageAnalyticsTest : AnalyticsTest() {

    private val statusRobot = StatusRobot()
    private val localMessageRobot = LocalMessageRobot()

    @Before
    override fun setUp() {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
    }

    @Test
    fun startApp_thenTapLocalMessageBanner_startsLocalMessageActivity() {
        testAppContext.getLocalMessagesProvider().localMessages = MockLocalMessagesApi.successResponse

        // Run background task to set background ticks
        runBackgroundTasks()

        startTestActivity<MainActivity>()

        statusRobot.clickLocalMessageBanner()
        waitFor { localMessageRobot.checkActivityIsDisplayed() }

        assertOnFields {
            assertEquals(1, Metrics::didAccessLocalInfoScreenViaBanner)
            assertPresent(Metrics::isDisplayingLocalInfoBackgroundTick)
        }
    }

    @Test
    fun startAppFromLocalMessageNotification_startsLocalMessageActivity() {
        testAppContext.localMessagesApi.response = MockLocalMessagesApi.successResponse
        testAppContext.getLocalMessagesProvider().localMessages = null

        // Run background task to set background ticks
        runBackgroundTasks()

        startTestActivity<MainActivity> {
            putExtra(NotificationProvider.TAPPED_ON_LOCAL_MESSAGE_NOTIFICATION, true)
        }

        waitFor { localMessageRobot.checkActivityIsDisplayed() }

        assertOnFields {
            assertEquals(1, Metrics::didAccessLocalInfoScreenViaNotification)
            assertEquals(1, Metrics::didSendLocalInfoNotification)
            assertPresent(Metrics::isDisplayingLocalInfoBackgroundTick)
        }
    }
}

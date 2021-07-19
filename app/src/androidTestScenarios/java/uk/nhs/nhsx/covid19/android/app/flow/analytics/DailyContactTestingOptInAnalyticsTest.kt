package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.DailyContactTesting
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics

class DailyContactTestingOptInAnalyticsTest : AnalyticsTest() {

    private var riskyContact = RiskyContact(this)
    private var dailyContactTesting = DailyContactTesting()

    @After
    override fun tearDown() {
        super.tearDown()
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun contactCaseOnly_optInToDailyContactTesting_setAppropriateAnalyticsFlags() {
        FeatureFlagTestHelper.enableFeatureFlag(DAILY_CONTACT_TESTING)

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Has risky contact on 2nd Jan
        // Isolation end date: 13th Jan
        riskyContact.triggerViaCircuitBreaker(this::advanceToNextBackgroundTaskExecution)
        riskyContact.acknowledge()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            assertEquals(1, Metrics::receivedRiskyContactNotification)
            assertEquals(1, Metrics::acknowledgedStartOfIsolationDueToRiskyContact)
            assertEquals(1, Metrics::receivedActiveIpcToken)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
            assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
            assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
            ignore(
                // totalRiskyContactReminderNotifications is set based on AlarmManager and introduces flakiness
                Metrics::totalRiskyContactReminderNotifications
            )
        }

        // Opt in to daily contact testing on 3rd Jan
        // This also updates the expiration date of the contact case isolation to the 3rd of January
        dailyContactTesting.optIn()

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            assertEquals(1, Metrics::declaredNegativeResultFromDCT)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
            assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
            assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
            ignore(
                // totalRiskyContactReminderNotifications is set based on AlarmManager and introduces flakiness
                Metrics::totalRiskyContactReminderNotifications
            )
        }

        // Dates: 4th-16th Jan -> Analytics packets for: 3rd-15th Jan
        assertOnFieldsForDateRange(4..16) {
            assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
        }

        // Current date: 17th Jan
        // When opting out in to daily contact testing, expiry date of isolation is equal to opt-in date (3rd Jan)
        // This information is deleted 14 days after expiration date, which was the 17th
        assertAnalyticsPacketIsNormal()
    }
}

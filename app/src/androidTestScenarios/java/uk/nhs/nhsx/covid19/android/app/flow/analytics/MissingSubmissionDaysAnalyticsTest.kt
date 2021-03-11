package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics

class MissingSubmissionDaysAnalyticsTest : AnalyticsTest() {

    @Test
    fun testMissingAnalyticsDaysField() {
        // Initially the missingPacketsLast7Days field is 0
        // [1111111] (visualization of packets sent for the recent days. 0 = no packet sent; 1 = packet sent; brackets symbolize sliding window)
        assertAnalyticsPacketIsNormal()

        // Fast-forward one day without submitting
        executeWhileOffline {
            advanceToEndOfAnalyticsWindow(steps = 1)
        }

        // 1[1111110]  <-- One day missed
        assertOnFields {
            assertEquals(1, Metrics::missingPacketsLast7Days)
        }

        // Fast-forward 7 days without submitting
        executeWhileOffline {
            repeat(7) {
                advanceToEndOfAnalyticsWindow(steps = 1)
            }
        }

        // [0000000]  <-- no package transmitted in the last 7 days
        assertOnFields {
            assertEquals(7, Metrics::missingPacketsLast7Days)
        }

        // 0[0000001]
        advanceToEndOfAnalyticsWindow(steps = 1)

        // 00[0000011]
        advanceToEndOfAnalyticsWindow(steps = 1)

        // 000[0000111]
        assertOnFields {
            assertEquals(4, Metrics::missingPacketsLast7Days)
        }

        // 0000[0001111]
        assertOnFields {
            assertEquals(3, Metrics::missingPacketsLast7Days)
        }

        // 00000[0011111]
        assertOnFields {
            assertEquals(2, Metrics::missingPacketsLast7Days)
        }

        // 000000[0111111]
        assertOnFields {
            assertEquals(1, Metrics::missingPacketsLast7Days)
        }

        // Back to normal
        // 0000000[1111111]
        assertAnalyticsPacketIsNormal()
    }

    private fun executeWhileOffline(action: () -> Unit) {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL
        action()
        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED
    }
}

package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat

class GroupAnalyticsEventsTest {

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-29T00:05:00.00Z"), ZoneOffset.UTC)

    private val testSubject = GroupAnalyticsEvents(
        analyticsLogStorage,
        GetAnalyticsWindow(fixedClock),
        fixedClock
    )

    @Test
    fun `failure on reading events log returns failure`() = runBlocking {
        val exception = Exception()

        every { analyticsLogStorage.value } throws exception

        val actual = testSubject.invoke()

        val expected = Failure(exception)

        assertEquals(expected, actual)
    }

    @Test
    fun `filter out the current window`() = runBlocking {
        every { analyticsLogStorage.value } returns
            listOf(currentWindowLogEntries, listOf(lastWindowLogEntry)).flatten()

        val actual = testSubject.invoke()

        val expected = Success(
            listOf(
                AnalyticsEventsGroup(
                    analyticsWindow = AnalyticsWindow(
                        startDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat(),
                        endDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
                    ),
                    listOf(lastWindowLogEntry)
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `group events in two analytics windows`() =
        runBlocking {
            every { analyticsLogStorage.value } returns
                listOf(
                    currentWindowLogEntries,
                    listOf(lastWindowLogEntry),
                    listOf(oldWindowLogEntry)
                ).flatten()

            val actual = testSubject.invoke()

            val expected = Success(
                listOf(
                    AnalyticsEventsGroup(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
                        ),
                        listOf(lastWindowLogEntry)
                    ),
                    AnalyticsEventsGroup(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-26T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat()
                        ),
                        listOf(oldWindowLogEntry)
                    )
                )
            )

            assertEquals(expected, actual)
        }

    private val currentWindowLogEntries = listOf(
        AnalyticsLogEntry(
            instant = Instant.parse("2020-09-29T00:00:00Z"),
            logItem = Event(QR_CODE_CHECK_IN)
        ),
        AnalyticsLogEntry(
            instant = Instant.parse("2020-09-29T00:00:00Z"),
            logItem = Event(CANCELED_CHECK_IN)
        )
    )

    private val lastWindowLogEntry = AnalyticsLogEntry(
        instant = Instant.parse("2020-09-27T00:00:00Z"),
        logItem = Event(POSITIVE_RESULT_RECEIVED)
    )

    private val oldWindowLogEntry = AnalyticsLogEntry(
        instant = Instant.parse("2020-09-26T00:00:00Z"),
        logItem = Event(COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION)
    )
}

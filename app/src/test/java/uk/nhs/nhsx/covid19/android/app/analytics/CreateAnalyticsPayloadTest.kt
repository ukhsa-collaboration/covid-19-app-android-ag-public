package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Instant
import kotlin.test.assertEquals

class CreateAnalyticsPayloadTest {

    private val calculateMissingSubmissionDays = mockk<CalculateMissingSubmissionDays>()
    private val metadataProvider = mockk<MetadataProvider>()
    private val updateStatusStorage = mockk<UpdateStatusStorage>()
    private val filterAnalyticsEvents = mockk<FilterAnalyticsEvents>()

    private val testSubject = CreateAnalyticsPayload(
        calculateMissingSubmissionDays,
        metadataProvider,
        updateStatusStorage,
        filterAnalyticsEvents
    )

    @Before
    fun setupMocks() {
        every { calculateMissingSubmissionDays.invoke(any()) } returns missingSubmissionDays
        every { metadataProvider.getMetadata() } returns metadata
        every { updateStatusStorage.value } returns updateStatus
        coEvery { filterAnalyticsEvents.invoke(any()) } returns metrics
    }

    @Test
    fun `payload is constructed correctly from grouped analytics`() = runBlocking {
        val payload = testSubject.invoke(group)

        verify { calculateMissingSubmissionDays.invoke(analyticsWindow) }
        verify { updateStatusStorage.value }
        verify { metadataProvider.getMetadata() }
        coVerify { filterAnalyticsEvents.invoke(metrics) }

        assertEquals(expectedPayload, payload)
    }

    @Test
    fun `payload has null fields from filtered analytics`() = runBlocking {
        coEvery { filterAnalyticsEvents.invoke(any()) } returns metrics.copy(receivedActiveIpcToken = null)

        val expectedFilteredPayload = expectedPayload.copy(metrics = metrics.copy(receivedActiveIpcToken = null))

        val payload = testSubject.invoke(group)
        assertEquals(expectedFilteredPayload, payload)
    }

    private val metadata = Metadata(
        "",
        "",
        "",
        "",
        ""
    )
    private val updateStatus = false
    private val missingSubmissionDays = 2
    private val logEntries = listOf<AnalyticsLogEntry>()
    private val metrics = Metrics().copy(missingPacketsLast7Days = missingSubmissionDays)

    private val startDate = Instant.parse("2020-09-25T00:00:00Z")
    private val endDate = Instant.parse("2020-09-26T00:00:00Z")
    private val analyticsWindow = AnalyticsWindow(
        startDate = startDate.toISOSecondsFormat(),
        endDate = endDate.toISOSecondsFormat()
    )

    private val group = AnalyticsEventsGroup(
        analyticsWindow = analyticsWindow,
        entries = logEntries
    )

    private val expectedPayload = AnalyticsPayload(
        analyticsWindow,
        updateStatus,
        metadata,
        metrics
    )
}
